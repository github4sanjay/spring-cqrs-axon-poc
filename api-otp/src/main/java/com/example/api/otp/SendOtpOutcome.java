package com.example.api.otp;

import java.util.Objects;
import javax.validation.ConstraintViolationException;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SendOtpOutcome {

  private String token;
  private Long retryAfter;
  private SendOtpResult result;

  /**
   * @param token Opaque token that identifies this attempt. It is nullable in case Otp was not
   *     sent. e.g., h65rd5vst8ejl2szrkzs
   * @param retryAfter Duration after which next request can be sent, in seconds. e.g., 60
   * @param result OTPResult {@link SendOtpResult}
   */
  @Builder
  public SendOtpOutcome(String token, Long retryAfter, SendOtpResult result) {
    Objects.requireNonNull(retryAfter, "retryAfter should not be null");
    if (retryAfter < 0) {
      throw new ConstraintViolationException("retryAfter should be positive", null);
    }
    Objects.requireNonNull(result, "result should not be null");
    this.token = token;
    this.retryAfter = retryAfter;
    this.result = result;
  }
}
