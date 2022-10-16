package com.example.gateway;

import static com.example.gateway.GatewayExceptions.*;

import com.example.spring.core.exceptions.CoreExceptions;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AddAuthHeaderGatewayFilter
    implements GatewayFilterFactory<AbstractGatewayFilterFactory.NameConfig> {
  private final KeyService keyService;

  @Autowired
  public AddAuthHeaderGatewayFilter(KeyService keyService) {
    this.keyService = keyService;
  }

  @Override
  public Class<AbstractGatewayFilterFactory.NameConfig> getConfigClass() {
    return AbstractGatewayFilterFactory.NameConfig.class;
  }

  @Override
  public GatewayFilter apply(AbstractGatewayFilterFactory.NameConfig config) {
    return (exchange, chain) ->
        ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .flatMap(
                authentication -> {
                  if (authentication != null
                      && authentication.getPrincipal() != null
                      && authentication.getPrincipal() instanceof Jwt jwt) {
                    var accountClaim = (String) jwt.getClaim("account");
                    var deviceClaim = (String) jwt.getClaim("device");
                    if (accountClaim != null && deviceClaim != null) {
                      log.info(
                          "API {} request by {} from device {}",
                          exchange.getRequest().getPath().value(),
                          accountClaim,
                          deviceClaim);
                      if (exchange.getRequest().getHeaders().containsKey("X-AUTHORIZATION-ID")) {
                        return Mono.error(CoreExceptions.FORBIDDEN.getEx());
                      }
                      if (exchange.getRequest().getHeaders().containsKey("X-DEVICE-ID")) {
                        return Mono.error(CoreExceptions.FORBIDDEN.getEx());
                      }
                      exchange
                          .getRequest()
                          .mutate()
                          .header("X-AUTHORIZATION-ID", accountClaim)
                          .build();
                      exchange.getRequest().mutate().header("X-DEVICE-ID", deviceClaim).build();
                      return chain.filter(exchange);
                    }
                  }

                  var gatewayContext =
                      exchange.<GatewayContext>getAttribute(GatewayContext.CACHE_GATEWAY_CONTEXT);
                  if (gatewayContext == null) {
                    log.error("gateway context is null");
                    return Mono.error(CoreExceptions.INTERNAL_SERVER_ERROR.getEx());
                  }
                  var request = exchange.getRequest();
                  var signatureHeader = request.getHeaders().get("x-signature");
                  var keyHeader = request.getHeaders().get("x-key");
                  if (signatureHeader == null || signatureHeader.isEmpty())
                    return Mono.error(SIGNATURE_HEADER_REQUIRED.ex);
                  if (keyHeader == null || keyHeader.isEmpty())
                    return Mono.error(KEY_HEADER_REQUIRED.ex);
                  var actualSignature = signatureHeader.get(0);
                  var key = keyHeader.get(0);
                  var expectedSignature =
                      new RequestSignature(
                          gatewayContext.getCacheBody(), gatewayContext.getQuery());
                  return keyService
                      .getKey(key)
                      .doOnNext(
                          keyResponse -> {
                            try {
                              if (!expectedSignature.verify(
                                  actualSignature, keyResponse.getPublicKey())) {
                                throw INVALID_SIGNATURE.ex;
                              }
                            } catch (NoSuchAlgorithmException
                                | InvalidKeyException
                                | SignatureException
                                | InvalidKeySpecException
                                | IllegalArgumentException e) {
                              throw INVALID_SIGNATURE.ex;
                            }
                          })
                      .flatMap(
                          keyResponse -> {
                            exchange
                                .getRequest()
                                .mutate()
                                .header("X-AUTHORIZATION-ID", keyResponse.getUserId().toString())
                                .build();
                            return chain.filter(exchange);
                          });
                });
  }
}
