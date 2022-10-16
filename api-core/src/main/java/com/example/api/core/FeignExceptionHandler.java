package com.example.api.core;

import com.example.spring.core.exceptions.APIException;
import feign.Response;

public interface FeignExceptionHandler {
  APIException handle(Response response);
}
