package com.example.gateway;

import com.example.spring.core.exceptions.ApplicationException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum GatewayExceptions {
  INVALID_SIGNATURE(
      new ApplicationException(
          "invalid-signature", "signature not valid", HttpStatus.UNAUTHORIZED)),
  SIGNATURE_HEADER_REQUIRED(
      new ApplicationException(
          "signature-header-required", "x-signature header required", HttpStatus.UNAUTHORIZED)),
  KEY_HEADER_REQUIRED(
      new ApplicationException(
          "key-header-required", "x-key header required", HttpStatus.UNAUTHORIZED)),
  ;

  public final ApplicationException ex;
}
