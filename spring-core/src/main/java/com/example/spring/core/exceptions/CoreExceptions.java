package com.example.spring.core.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CoreExceptions implements IException {

  // HTTP 4xx
  BAD_REQUEST(
      new ApplicationException(
          "bad-request", HttpStatus.BAD_REQUEST.getReasonPhrase(), HttpStatus.BAD_REQUEST)),
  UNAUTHORIZED(
      new ApplicationException(
          "unauthorized", HttpStatus.UNAUTHORIZED.getReasonPhrase(), HttpStatus.UNAUTHORIZED)),
  FORBIDDEN(
      new ApplicationException(
          "forbidden", HttpStatus.FORBIDDEN.getReasonPhrase(), HttpStatus.FORBIDDEN)),
  NOT_FOUND(
      new ApplicationException(
          "not-found", HttpStatus.NOT_FOUND.getReasonPhrase(), HttpStatus.NOT_FOUND)),

  // HTTP 5xx
  INTERNAL_SERVER_ERROR(
      new ApplicationException(
          "internal-server-error",
          HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
          HttpStatus.INTERNAL_SERVER_ERROR)),
  NOT_IMPLEMENTED(
      new ApplicationException(
          "not-implemented",
          HttpStatus.NOT_IMPLEMENTED.getReasonPhrase(),
          HttpStatus.NOT_IMPLEMENTED)),

  SERVICE_UNAVAILABLE(
      new ApplicationException(
          "service-unavailable",
          HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
          HttpStatus.SERVICE_UNAVAILABLE)),

  // core
  TOO_MANY(new ApplicationException("too-many", "Too many elements", HttpStatus.BAD_REQUEST)),

  INVALID_LOCALE(
      new ApplicationException("invalid-locale", "Invalid Locale", HttpStatus.BAD_REQUEST)),

  VALIDATION_FAILED(
      new ApplicationException("validation-failed", "Validation failed", HttpStatus.BAD_REQUEST)),

  INVALID_EXCHANGE(
      new ApplicationException("invalid-exchange", "Validation failed", HttpStatus.BAD_REQUEST)),

  INVALID_CURRENCY(
      new ApplicationException("invalid-currency", "Validation failed", HttpStatus.BAD_REQUEST)),

  INVALID_MARKET(
      new ApplicationException("invalid-market", "Validation failed", HttpStatus.BAD_REQUEST)),

  INVALID_ORDER_STATUS(
      new ApplicationException(
          "invalid-order-status", "Validation failed", HttpStatus.BAD_REQUEST)),

  INVALID_TIME_IN_FORCE(
      new ApplicationException(
          "invalid-time-in-force", "Validation failed", HttpStatus.BAD_REQUEST)),

  INVALID_ORDER_TYPE(
      new ApplicationException("invalid-order-type", "Validation failed", HttpStatus.BAD_REQUEST)),

  INVALID_RESOLUTION(
      new ApplicationException("invalid-resolution", "Validation failed", HttpStatus.BAD_REQUEST)),

  INVALID_BLOCKCHAIN(
      new ApplicationException("invalid-blockchain", "Validation failed", HttpStatus.BAD_REQUEST)),

  INVALID_SIDE(
      new ApplicationException("invalid-side", "Validation failed", HttpStatus.BAD_REQUEST)),

  AGGREGATE_NOT_FOUND(
      new ApplicationException("aggregate-not-found", "aggregate-not-found", HttpStatus.NOT_FOUND));

  private final ApplicationException ex;
}
