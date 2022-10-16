package com.example.spring.core.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@ToString
@NoArgsConstructor
public class Error {

  public Instant datetime;
  public String code;
  public String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String uri;

  public HttpStatus httpStatus;

  public Error(ApplicationException exception, String uri) {
    this.datetime = Instant.now();
    this.code = exception.code;
    this.description = exception.getMessage();
    this.uri = uri;
    this.httpStatus = exception.getStatus();
  }

  public Error(ApplicationException exception) {
    this.datetime = Instant.now();
    this.code = exception.code;
    this.description = exception.getMessage();
    this.uri = null;
    this.httpStatus = exception.getStatus();
  }
}
