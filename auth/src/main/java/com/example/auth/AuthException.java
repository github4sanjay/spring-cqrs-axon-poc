package com.example.auth;

import com.example.spring.core.exceptions.ApplicationException;
import com.example.spring.core.exceptions.IException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthException implements IException {
  DEVICE_NOT_FOUND(
      new ApplicationException("device-not-found", "device not found", HttpStatus.NOT_FOUND)),

  ACCOUNT_CREATION_NOT_ENABLED(
      new ApplicationException(
          "account-creation-not-enabled", "account creation not enabled", HttpStatus.FORBIDDEN)),

  ACCOUNT_NOT_FOUND(
      new ApplicationException("account-not-found", "account not found", HttpStatus.NOT_FOUND)),

  ACCOUNT_ALREADY_EXIST(
      new ApplicationException(
          "account-already-exist", "account already exist", HttpStatus.FORBIDDEN)),

  ACCOUNT_INACTIVE_STATUS(
      new ApplicationException(
          "account-inactive-status", "account inactive status", HttpStatus.FORBIDDEN)),
  INVALID_CLIENT(
      new ApplicationException("invalid-client", "invalid client", HttpStatus.BAD_REQUEST)),
  INVALID_TOKEN(
      new ApplicationException("invalid-token", "invalid token", HttpStatus.UNAUTHORIZED)),

  INVALID_DEVICE_CODE(
      new ApplicationException(
          "invalid-device-code", "invalid device code", HttpStatus.UNAUTHORIZED)),
  INVALID_SIGNATURE(
      new ApplicationException("invalid-signature", "invalid signature", HttpStatus.UNAUTHORIZED)),
  ORIGIN_NOT_ALLOWED(
      new ApplicationException(
          "origin-not-allowed", "origin not allowed for this client", HttpStatus.UNAUTHORIZED)),
  EXPIRED_TOKEN(
      new ApplicationException("expired-token", "token is expired", HttpStatus.UNAUTHORIZED)),
  INVALID_CREDENTIAL(
      new ApplicationException(
          "invalid-credential", "credential is not valid", HttpStatus.UNAUTHORIZED)),

  REFRESH_TOKEN_NOT_AVAILABLE(
      new ApplicationException(
          "refresh-token-not-available", "refresh token not available", HttpStatus.NOT_FOUND)),

  ACCOUNT_LOGIN_REQUIRED(
      new ApplicationException(
          "account-login-required", "account login required", HttpStatus.UNAUTHORIZED)),

  TOO_MANY_OTP_REQUEST(
      new ApplicationException(
          "too-many-otp-requests",
          "too many otp requests retry after %d seconds",
          HttpStatus.UNAUTHORIZED)),

  TOO_EARLY_OTP_REQUEST(
      new ApplicationException(
          "too-early-otp-requests",
          "too early otp requests retry after %d seconds",
          HttpStatus.UNAUTHORIZED)),

  INVALID_OTP(new ApplicationException("invalid-otp", "invalid otp", HttpStatus.UNAUTHORIZED)),

  EXPIRED_OTP(new ApplicationException("expired-otp", "expired otp", HttpStatus.UNAUTHORIZED)),

  BLOCKED_OTP_VERIFICATION(
      new ApplicationException(
          "blocked-otp-verification", "blocked otp verification", HttpStatus.UNAUTHORIZED)),

  FACTOR_ALREADY_ACTIVE(
      new ApplicationException(
          "factor-already-active", "factor already active", HttpStatus.UNAUTHORIZED)),

  FACTOR_NOT_FOUND(
      new ApplicationException("factor-not-found", "factor not found", HttpStatus.NOT_FOUND)),

  INVALID_RECOVERY_CODE(
      new ApplicationException(
          "invalid-recovery-code", "invalid-recovery-code", HttpStatus.UNAUTHORIZED)),
  ;

  public static ApplicationException TOO_MANY_OTP_REQUEST(Long retryAfter) {
    var ex = TOO_MANY_OTP_REQUEST.getEx();
    throw new ApplicationException(
        ex.getCode(), String.format(ex.getDescription(), retryAfter), ex.getStatus());
  }

  public static ApplicationException TOO_EARLY_OTP_REQUEST(Long retryAfter) {
    var ex = TOO_EARLY_OTP_REQUEST.getEx();
    return new ApplicationException(
        ex.getCode(), String.format(ex.getDescription(), retryAfter), ex.getStatus());
  }

  private final ApplicationException ex;
}
