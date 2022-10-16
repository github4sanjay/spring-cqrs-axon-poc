package com.example.spring.web;

import com.example.spring.core.exceptions.APIException;
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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Order(-2)
@Configuration
@RequiredArgsConstructor
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

  private final ObjectMapper objectMapper;

  @Override
  @NonNull
  public Mono<Void> handle(ServerWebExchange serverWebExchange, @NonNull Throwable throwable) {
    DataBufferFactory bufferFactory = serverWebExchange.getResponse().bufferFactory();
    var appException = CoreExceptions.INTERNAL_SERVER_ERROR.getEx();
    if (throwable instanceof ApplicationException) {
      appException = (ApplicationException) throwable;
    } else if (throwable instanceof APIException e) {
      var error = e.error;
      appException = new ApplicationException(error.code, error.description, error.httpStatus);
    } else if (throwable instanceof WebExchangeBindException e) {
      if (e.getStatus() == HttpStatus.BAD_REQUEST) {
        var errors = e.getBindingResult().getAllErrors();
        var firstError = errors.get(0);
        appException =
            new ApplicationException(
                CoreExceptions.BAD_REQUEST.getEx().getCode(),
                firstError.getDefaultMessage(),
                e.getStatus());
      }
    } else if (throwable instanceof ResponseStatusException e) {
      if (e.getStatus() == HttpStatus.NOT_FOUND) {
        appException = CoreExceptions.NOT_FOUND.getEx();
      } else {
        appException = new ApplicationException(e.getMessage(), e.getReason(), e.getStatus());
      }
    } else {
      log.error("", throwable);
    }
    var response = serverWebExchange.getResponse();
    var request = serverWebExchange.getRequest();
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    response.setStatusCode(appException.getStatus());
    DataBuffer dataBuffer = null;
    try {
      dataBuffer =
          bufferFactory.wrap(
              objectMapper.writeValueAsBytes(
                  new ErrorResponse(new Error(appException, request.getURI().toString()))));
    } catch (JsonProcessingException e) {
      dataBuffer = bufferFactory.wrap("".getBytes());
    }
    return serverWebExchange.getResponse().writeWith(Mono.just(dataBuffer));
  }
}
