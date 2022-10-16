package com.example.api.otp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VerifyOtpOutcome {

  private JsonNode state;
  private VerifyOtpResult result;
  private Integer remainingCount;

  /**
   * @param state Custom additional data that was provided when the OTP was sent, if any. e.g.,
   *     {"user": "example@singtel.com"}
   * @param result VerifyResult {@link VerifyOtpResult}
   * @param remainingCount remaining verification count left
   */
  @Builder
  public VerifyOtpOutcome(JsonNode state, VerifyOtpResult result, Integer remainingCount) {
    Objects.requireNonNull(result, "result should not be null");
    this.state = state;
    this.result = result;
    this.remainingCount = remainingCount;
  }
}
