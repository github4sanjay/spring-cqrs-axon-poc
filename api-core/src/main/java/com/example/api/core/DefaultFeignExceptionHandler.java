package com.example.api.core;

import com.example.spring.core.exceptions.APIException;
import com.example.spring.core.exceptions.CoreExceptions;
import com.example.spring.core.exceptions.Error;
import com.example.spring.core.json.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultFeignExceptionHandler implements FeignExceptionHandler {

  private final ObjectMapper objectMapper;

  DefaultFeignExceptionHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public APIException handle(Response response) {
    if (response.body() == null)
      return new APIException(new Error(CoreExceptions.INTERNAL_SERVER_ERROR.getEx()));
    try {
      var errorResponse =
          objectMapper.readValue(response.body().asInputStream(), ErrorResponse.class);
      return new APIException(errorResponse.error);
    } catch (Exception e) {
      log.error("unable to parse API response as ErrorResponse", e);
      return new APIException(new Error(CoreExceptions.INTERNAL_SERVER_ERROR.getEx()));
    }
  }
}
