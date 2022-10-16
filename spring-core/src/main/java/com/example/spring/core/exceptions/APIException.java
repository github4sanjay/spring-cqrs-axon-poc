package com.example.spring.core.exceptions;

public class APIException extends RuntimeException {

  public final Error error;

  public APIException(Error error) {
    super(error.code + " - " + error.description);
    this.error = error;
  }
}
