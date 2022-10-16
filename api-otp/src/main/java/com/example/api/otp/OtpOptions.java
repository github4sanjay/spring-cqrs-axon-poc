package com.example.api.otp;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OtpOptions {
  private String profile;
  private Integer expiration;
  private Integer resendAfter;
  private Integer rateLimitCount;
  private Integer rateLimitExpiry;
  private Integer verifyLimitCount;
  private JsonNode state;

  /**
   * Constructor for class OtpOptions
   *
   * @param profile Profile name that determines how the message will be sent. Should follow regex
   *     "^[a-z]{2,15}$". Default value is unspecified. e.g., webapp
   * @param expiration Expiration time for OTP validity, in seconds. Should be positive. Default
   *     value is 60, e.g., 30
   * @param resendAfter Resend OTP allowed after duration, in seconds. Should be positive. Default
   *     value is 30, e.g., 60
   * @param rateLimitCount Rate limit for new OTP request allowed in given duration. Should be
   *     positive. Default value is 5, e.g., 3
   * @param rateLimitExpiry Duration in seconds for rate limit on OTP requests. Should be positive.
   *     Default value is 1800, e.g., 3600
   * @param verifyLimitCount Limit number of verify OTP request. Should be positive. Default value
   *     is 5, e.g., 3
   * @param state Custom additional data that will be returned after successful validation. e.g.,
   *     {"user": "github4sanjay@example.com"}
   */
  @Builder
  public OtpOptions(
      String profile,
      Integer expiration,
      Integer resendAfter,
      Integer rateLimitCount,
      Integer rateLimitExpiry,
      Integer verifyLimitCount,
      JsonNode state) {
    if (profile == null) profile = "unspecified";
    if (expiration == null) expiration = 60;
    if (resendAfter == null) resendAfter = 30;
    if (rateLimitCount == null) rateLimitCount = 5;
    if (rateLimitExpiry == null) rateLimitExpiry = 1800;
    if (verifyLimitCount == null) verifyLimitCount = 5;
    this.profile = profile;
    this.expiration = expiration;
    this.resendAfter = resendAfter;
    this.rateLimitCount = rateLimitCount;
    this.rateLimitExpiry = rateLimitExpiry;
    this.verifyLimitCount = verifyLimitCount;
    this.state = state;
  }
}
