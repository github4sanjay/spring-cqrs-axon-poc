package com.example.api.core;

import com.example.spring.core.exceptions.APIException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class APIErrorDecoder implements ErrorDecoder {

  private final FeignExceptionHandler handler;

  public APIErrorDecoder(FeignExceptionHandler handler) {
    this.handler = handler;
  }

  @Override
  public APIException decode(String methodKey, Response response) {
    return handler.handle(response);
  }
}
