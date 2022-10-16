package com.example.gateway;

import io.netty.buffer.ByteBufAllocator;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class GatewayContextFilter implements GlobalFilter, Ordered {
  private static final byte[] EMPTY_BODY = new byte[0];
  private static final List<HttpMessageReader<?>> messageReaders =
      HandlerStrategies.withDefaults().messageReaders();

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var request = exchange.getRequest();
    var path = request.getPath().pathWithinApplication().value();
    var gatewayContext = new GatewayContext();
    gatewayContext.setPath(path);
    gatewayContext.setQuery(request.getURI().getQuery());
    exchange.getAttributes().put(GatewayContext.CACHE_GATEWAY_CONTEXT, gatewayContext);
    var headers = request.getHeaders();
    var contentType = headers.getContentType();
    if (request.getMethod() == HttpMethod.POST
        || request.getMethod() == HttpMethod.PUT
        || request.getMethod() == HttpMethod.PATCH) {
      Mono<Void> voidMono;
      if (MediaType.APPLICATION_FORM_URLENCODED.equals(contentType)) {
        voidMono = readFormData(exchange, chain, gatewayContext);
      } else {
        voidMono = readBody(exchange, chain, gatewayContext);
      }
      return voidMono;
    }
    return chain.filter(exchange);
  }

  private Mono<Void> readFormData(
      ServerWebExchange exchange, GatewayFilterChain chain, GatewayContext gatewayContext) {
    final var request = exchange.getRequest();
    var headers = request.getHeaders();
    return exchange
        .getFormData()
        .doOnNext(gatewayContext::setFormData)
        .then(
            Mono.defer(
                () -> {
                  var charset =
                      (headers.getContentType() == null
                              || headers.getContentType().getCharset() == null)
                          ? StandardCharsets.UTF_8
                          : headers.getContentType().getCharset();
                  var charsetName = charset.name();
                  var formData = gatewayContext.getFormData();
                  if (null == formData || formData.isEmpty()) {
                    return chain.filter(exchange);
                  }
                  var formDataBodyBuilder = new StringBuilder();
                  String entryKey;
                  List<String> entryValue;
                  try {
                    for (Map.Entry<String, List<String>> entry : formData.entrySet()) {
                      entryKey = entry.getKey();
                      entryValue = entry.getValue();
                      if (entryValue.size() > 1) {
                        for (String value : entryValue) {
                          formDataBodyBuilder
                              .append(entryKey)
                              .append("=")
                              .append(URLEncoder.encode(value, charsetName))
                              .append("&");
                        }
                      } else {
                        formDataBodyBuilder
                            .append(entryKey)
                            .append("=")
                            .append(URLEncoder.encode(entryValue.get(0), charsetName))
                            .append("&");
                      }
                    }
                  } catch (UnsupportedEncodingException e) {
                    // ignore URLEncode Exception
                  }
                  var formDataBodyString = "";
                  if (formDataBodyBuilder.length() > 0) {
                    formDataBodyString =
                        formDataBodyBuilder.substring(0, formDataBodyBuilder.length() - 1);
                  }
                  var bodyBytes = formDataBodyString.getBytes(charset);
                  var contentLength = bodyBytes.length;
                  var decorator =
                      new ServerHttpRequestDecorator(request) {
                        @Override
                        @NonNull
                        public HttpHeaders getHeaders() {
                          HttpHeaders httpHeaders = new HttpHeaders();
                          httpHeaders.putAll(super.getHeaders());
                          if (contentLength > 0) {
                            httpHeaders.setContentLength(contentLength);
                          } else {
                            httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                          }
                          return httpHeaders;
                        }

                        @Override
                        @NonNull
                        public Flux<DataBuffer> getBody() {
                          return DataBufferUtils.read(
                              new ByteArrayResource(bodyBytes),
                              new NettyDataBufferFactory(ByteBufAllocator.DEFAULT),
                              contentLength);
                        }
                      };
                  var mutateExchange = exchange.mutate().request(decorator).build();
                  return chain.filter(mutateExchange);
                }));
  }

  private Mono<Void> readBody(
      ServerWebExchange exchange, GatewayFilterChain chain, GatewayContext gatewayContext) {
    return DataBufferUtils.join(exchange.getRequest().getBody())
        .map(
            dataBuffer -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              DataBufferUtils.release(dataBuffer);
              return bytes;
            })
        .defaultIfEmpty(EMPTY_BODY)
        .flatMap(
            bytes -> {
              var mutatedRequest =
                  new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    @NonNull
                    public Flux<DataBuffer> getBody() {
                      if (bytes.length > 0) {
                        var dataBufferFactory = exchange.getResponse().bufferFactory();
                        return Flux.just(dataBufferFactory.wrap(bytes));
                      }
                      return Flux.empty();
                    }
                  };
              var mutatedExchange = exchange.mutate().request(mutatedRequest).build();
              return ServerRequest.create(mutatedExchange, messageReaders)
                  .bodyToMono(String.class)
                  .doOnNext(gatewayContext::setCacheBody)
                  .then(chain.filter(mutatedExchange));
            });
  }

  @Override
  public int getOrder() {
    return Integer.MIN_VALUE;
  }
}
