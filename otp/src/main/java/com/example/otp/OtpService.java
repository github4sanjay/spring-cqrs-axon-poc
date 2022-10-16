package com.example.otp;

import com.example.api.otp.VerifyOtpOutcome;
import com.example.api.otp.VerifyOtpResult;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OtpService {

  public static final String OTP_REFERENCES_TOKEN_MAP = "otp-references-token";
  public static final String OTP_TOKEN_VERIFY_COUNT = "otp-references-verify-limit-";
  public static final String OTP_REFERENCES_RATE_LIMIT_COUNT = "otp-references-rate-limit-";
  public static final String OTP_REFERENCES_EARLY_REQUEST_TIME_MAP = "otp-references-early-request";
  private final RedissonClient redisson;
  private final Clock clock;

  public void saveToken(
      String token, String otp, JsonNode state, Integer expiration, Integer verifyLimitCount) {
    RMapCache<String, TokenState> map = redisson.getMapCache(OTP_REFERENCES_TOKEN_MAP);

    map.put(
        token,
        TokenState.builder()
            .otp(otp)
            .state(state)
            .expireAt(clock.instant().plus(expiration, ChronoUnit.SECONDS))
            .build(),
        (long) expiration,
        TimeUnit.SECONDS);

    RAtomicLong atomicLong = redisson.getAtomicLong(OTP_TOKEN_VERIFY_COUNT + token);
    atomicLong.set(verifyLimitCount + 1);
    atomicLong.expire(Duration.of(expiration, ChronoUnit.SECONDS));
  }

  public Optional<Long> getRetryAfterForRateLimit(
      String id, Integer rateLimitCount, Integer rateLimitExpiry) {
    RAtomicLong atomicLong = redisson.getAtomicLong(OTP_REFERENCES_RATE_LIMIT_COUNT + id);
    var usedCount = atomicLong.incrementAndGet();
    if (atomicLong.remainTimeToLive() == -1) {
      atomicLong.expire(Duration.of(rateLimitExpiry, ChronoUnit.SECONDS));
    }
    if (usedCount > rateLimitCount) {
      return Optional.of(atomicLong.remainTimeToLive() / 1000);
    }
    return Optional.empty();
  }

  public Optional<Long> getRetryAfterForEarlyRequest(String id, Integer resendAfter) {
    RMapCache<String, Long> map = redisson.getMapCache(OTP_REFERENCES_EARLY_REQUEST_TIME_MAP);
    var resendAfterInstant = map.get(id);
    if (resendAfterInstant != null) {
      var now = clock.instant().toEpochMilli();
      var diff = resendAfterInstant - now;
      if (diff > 0) {
        return Optional.of(diff / 1000);
      }
    }
    map.put(id, clock.instant().plus(resendAfter, ChronoUnit.SECONDS).toEpochMilli());
    return Optional.empty();
  }

  public VerifyOtpOutcome verifyOtp(String token, String expectedOtp) {
    RMapCache<String, TokenState> map = redisson.getMapCache(OTP_REFERENCES_TOKEN_MAP);
    var tokenState = map.get(token);
    if (tokenState == null || tokenState.getExpireAt().isBefore(clock.instant())) {
      return VerifyOtpOutcome.builder().remainingCount(0).result(VerifyOtpResult.Expired).build();
    } else {
      RAtomicLong atomicLong = redisson.getAtomicLong(OTP_TOKEN_VERIFY_COUNT + token);
      var remainingTry = atomicLong.decrementAndGet();
      if (remainingTry == 0) {
        return VerifyOtpOutcome.builder().remainingCount(0).result(VerifyOtpResult.Blocked).build();
      } else if (remainingTry == -1) {
        return VerifyOtpOutcome.builder().remainingCount(0).result(VerifyOtpResult.Expired).build();
      }
      if (expectedOtp.equals(tokenState.otp)) {
        return VerifyOtpOutcome.builder()
            .remainingCount(0)
            .result(VerifyOtpResult.Valid)
            .state(tokenState.getState())
            .build();
      } else {
        return VerifyOtpOutcome.builder()
            .remainingCount((int) remainingTry)
            .result(VerifyOtpResult.Invalid)
            .build();
      }
    }
  }

  @Data
  @NoArgsConstructor
  private static class TokenState {
    private String otp;
    private JsonNode state;
    private Long expireAt;

    @Builder
    public TokenState(String otp, JsonNode state, Instant expireAt) {
      Objects.requireNonNull(otp);
      Objects.requireNonNull(expireAt);
      this.otp = otp;
      this.state = state;
      this.expireAt = expireAt.toEpochMilli();
    }

    public Instant getExpireAt() {
      return Instant.ofEpochMilli(expireAt);
    }
  }
}
