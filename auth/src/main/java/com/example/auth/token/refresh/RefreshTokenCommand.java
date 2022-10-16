package com.example.auth.token.refresh;

import com.example.auth.token.Claims;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.*;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class RefreshTokenCommand {

  @Data
  @NoArgsConstructor
  public static class GenerateRefreshTokenCommand {
    @TargetAggregateIdentifier private String id;
    private String deviceId;
    private String subject;
    private Claims.AMR amr;
    private String token;
    private Instant expireAt;
    private Instant createdAt;
    private Duration refreshChainExpiry;

    @Builder
    public GenerateRefreshTokenCommand(
        String identifier,
        String deviceId,
        String subject,
        Claims.AMR amr,
        String token,
        Instant expireAt,
        Instant createdAt,
        Duration refreshChainExpiry) {
      Objects.requireNonNull(identifier);
      Objects.requireNonNull(deviceId);
      Objects.requireNonNull(subject);
      Objects.requireNonNull(amr);
      Objects.requireNonNull(token);
      Objects.requireNonNull(expireAt);
      Objects.requireNonNull(createdAt);
      Objects.requireNonNull(refreshChainExpiry);
      this.id = RefreshTokenCommand.getId(identifier, deviceId);
      this.deviceId = deviceId;
      this.subject = subject;
      this.amr = amr;
      this.token = token;
      this.expireAt = expireAt;
      this.createdAt = createdAt;
      this.refreshChainExpiry = refreshChainExpiry;
    }
  }

  @Data
  @NoArgsConstructor
  public static class RefreshRefreshTokenCommand {
    @TargetAggregateIdentifier private String id;
    private String token;
    private Instant expireAt;

    @Builder
    public RefreshRefreshTokenCommand(
        String identifier, String deviceId, String token, Instant expireAt) {
      Objects.requireNonNull(identifier);
      Objects.requireNonNull(deviceId);
      Objects.requireNonNull(token);
      Objects.requireNonNull(expireAt);
      this.id = RefreshTokenCommand.getId(identifier, deviceId);
      this.token = token;
      this.expireAt = expireAt;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DisableRefreshTokenCommand {
    @TargetAggregateIdentifier private String id;

    @Builder
    public DisableRefreshTokenCommand(String identifier, String deviceId) {
      Objects.requireNonNull(deviceId);
      Objects.requireNonNull(identifier);
      this.id = RefreshTokenCommand.getId(identifier, deviceId);
    }
  }

  private static String getId(String identifier, String deviceId) {
    return UUID.nameUUIDFromBytes(
            ("RefreshTokenAggregate." + deviceId + "." + identifier)
                .getBytes(StandardCharsets.UTF_8))
        .toString();
  }
}
