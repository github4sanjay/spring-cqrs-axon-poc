package com.example.auth;

import com.example.auth.common.ClientConfiguration;
import com.example.auth.common.OpenApi;
import com.example.auth.device.Device;
import com.example.auth.device.DeviceQuery;
import com.example.auth.device.DeviceRegistrationConfig;
import com.example.auth.signature.RequestSignature;
import com.example.spring.core.exceptions.CoreExceptions;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.request-verification",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RequestVerifier implements WebFilter {

  private static final byte[] EMPTY_BODY = new byte[0];

  private final ReactorQueryGateway reactorQueryGateway;
  private final DeviceRegistrationConfig deviceRegistrationConfig;
  private final ClientConfiguration clientConfiguration;

  @Override
  @NonNull
  public Mono<Void> filter(
      ServerWebExchange serverWebExchange, @NonNull WebFilterChain webFilterChain) {
    var serverRequest = serverWebExchange.getRequest();
    var method = serverRequest.getMethod();
    var uri = serverRequest.getPath().value();
    if ((uri.equals("/api/v1/devices") && HttpMethod.POST.equals(method))
        || uri.equals("/")
        || uri.contains("/swagger-ui")
        || uri.equals("/favicon.ico")
        || uri.startsWith("/actuator")
        || uri.startsWith("/v3/api-docs")
        || uri.equals("/api/v1/introspect")
        || uri.equals("/.well-known/jwks.json")
        || HttpMethod.OPTIONS.equals(method)) {
      return webFilterChain.filter(serverWebExchange);
    } else {
      return DataBufferUtils.join(serverWebExchange.getRequest().getBody())
          .map(
              dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return bytes;
              })
          .defaultIfEmpty(EMPTY_BODY)
          .flatMap(
              bytes ->
                  verifyRequest(
                          serverRequest.getHeaders(),
                          serverRequest.getURI().getQuery(),
                          new String(bytes))
                      .doOnSuccess(
                          device -> serverWebExchange.getAttributes().put("device", device))
                      .thenReturn(bytes))
          .flatMap(
              bytes -> {
                ServerHttpRequestDecorator decorator =
                    new ServerHttpRequestDecorator(serverWebExchange.getRequest()) {
                      @Nonnull
                      @Override
                      public Flux<DataBuffer> getBody() {
                        if (bytes.length > 0) {
                          DataBufferFactory dataBufferFactory =
                              serverWebExchange.getResponse().bufferFactory();
                          return Flux.just(dataBufferFactory.wrap(bytes));
                        }
                        return Flux.empty();
                      }
                    };
                return webFilterChain.filter(serverWebExchange.mutate().request(decorator).build());
              });
    }
  }

  private Mono<Device> verifyRequest(HttpHeaders httpHeaders, String queryString, String body) {
    var clientCodes = httpHeaders.get(OpenApi.CLIENT_CODE);
    var signatures = httpHeaders.get(OpenApi.SIGNATURE_HEADER);
    var signature = signatures == null ? null : signatures.get(0);
    return getDevice(clientCodes)
        .doOnSuccess(
            device -> {
              verifySignature(
                  httpHeaders, queryString, body, clientCodes.get(0), signature, device);
            })
        .doOnSuccess(
            device -> {
              verifyCorsOrigin(httpHeaders, device);
            });
  }

  private Mono<Device> getDevice(List<String> clientCodes) {
    if (clientCodes == null) {
      throw AuthException.INVALID_DEVICE_CODE.getEx();
    }
    var clientCode = clientCodes.get(0);
    var idAndPw = clientCode.split("\\.");
    if (idAndPw.length != 2) {
      throw AuthException.INVALID_DEVICE_CODE.getEx();
    }
    var id = idAndPw[0];
    var pw = idAndPw[1];
    return reactorQueryGateway
        .query(DeviceQuery.GetDeviceByIdQuery.builder().id(id).build(), Device.class)
        .doOnSuccess(
            device -> {
              if (!deviceRegistrationConfig.verifyHash(pw, device.getHash())) {
                throw AuthException.INVALID_DEVICE_CODE.getEx();
              }
            });
  }

  private void verifyCorsOrigin(HttpHeaders httpHeaders, Device currentDevice) {
    var origins = httpHeaders.get("origin");
    var origin = origins != null ? origins.get(0) : null;
    if (origin == null) return;
    var client = clientConfiguration.getCurrentClient(currentDevice.getClient());
    var allowedOrigins = client.getAllowedOrigins();
    if (allowedOrigins != null && !allowedOrigins.contains(origin)) {
      throw AuthException.ORIGIN_NOT_ALLOWED.getEx();
    }
  }

  private void verifySignature(
      HttpHeaders httpHeaders,
      String queryString,
      String body,
      String clientCode,
      String encryptedSignature,
      Device currentDevice) {
    var client = clientConfiguration.getCurrentClient(currentDevice.getClient());
    var authorizations = httpHeaders.get(OpenApi.JWT);
    var authorization = authorizations == null ? null : authorizations.get(0);
    if (isSignatureVerificationRequired(client, encryptedSignature)) {
      var signature = createSignature(clientCode, authorization, body, queryString);
      try {
        if (!signature.verify(encryptedSignature, currentDevice.getPublicKey())) {
          throw AuthException.INVALID_SIGNATURE.getEx();
        }
      } catch (InvalidKeyException
          | NoSuchAlgorithmException
          | SignatureException
          | InvalidKeySpecException e) {
        throw CoreExceptions.UNAUTHORIZED.getEx();
      }
    }
  }

  private boolean isSignatureVerificationRequired(
      ClientConfiguration.Client client, String encryptedSignature) {
    return client.getRequestSigningEnabled() || encryptedSignature != null;
  }

  private RequestSignature createSignature(
      String clientCodeHeaderValue, String authorizationHeader, String body, String paramMap) {
    return new RequestSignature(clientCodeHeaderValue, authorizationHeader, body, paramMap);
  }
}
