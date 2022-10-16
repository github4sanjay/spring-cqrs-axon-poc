package com.example.gateway;

import com.example.spring.core.exceptions.ApplicationException;
import com.example.spring.core.exceptions.CoreExceptions;
import com.example.spring.core.exceptions.Error;
import com.example.spring.core.json.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Order(-1)
@Configuration
@RequiredArgsConstructor
public class ExceptionHandler implements ErrorWebExceptionHandler {
  private final ObjectMapper objectMapper;

  @Override
  @NonNull
  public Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable ex) {
    ServerHttpResponse response = exchange.getResponse();

    if (response.isCommitted()) {
      return Mono.error(ex);
    }
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    if (ex instanceof ApplicationException exe) {
      var errorResponse = new ErrorResponse(new Error(exe));
      response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
      response.setStatusCode(exe.getStatus());
      return response.writeWith(
          Mono.fromSupplier(
              () -> {
                DataBufferFactory bufferFactory = response.bufferFactory();
                try {
                  return bufferFactory.wrap(objectMapper.writeValueAsBytes(errorResponse));
                } catch (JsonProcessingException e) {
                  log.warn("Error writing response", ex);
                  return bufferFactory.wrap(new byte[0]);
                }
              }));
    } else {
      log.error("error in processing request", ex);
      response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
      return response.writeWith(
          Mono.fromSupplier(
              () -> {
                DataBufferFactory bufferFactory = response.bufferFactory();
                try {
                  return bufferFactory.wrap(
                      objectMapper.writeValueAsBytes(
                          new ErrorResponse(
                              new Error(CoreExceptions.SERVICE_UNAVAILABLE.getEx()))));
                } catch (JsonProcessingException e) {
                  log.warn("Error writing response", ex);
                  return bufferFactory.wrap(new byte[0]);
                }
              }));
    }
  }
}
