package com.example.spring.core.exceptions;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@Getter
public class ApplicationException extends RuntimeException {

  public final String code;
  public final String description;
  public final HttpStatus status;

  public ApplicationException(String code, String description, HttpStatus status) {
    super(description);
    this.code = code;
    this.status = status;
    this.description = description;
  }
}
