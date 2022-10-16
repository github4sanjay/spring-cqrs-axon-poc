package com.example.auth.device.trust;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.*;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class UserDeviceCommand {

  @Data
  @NoArgsConstructor
  public static class RegisterUserDeviceCommand {
    @TargetAggregateIdentifier private String id;
    private String deviceId;
    private String accountId;
    private Instant lastLoginAt;

    @Builder
    public RegisterUserDeviceCommand(String deviceId, String accountId, Instant lastLoginAt) {
      Objects.requireNonNull(deviceId);
      Objects.requireNonNull(accountId);
      Objects.requireNonNull(lastLoginAt);
      this.id = UserDeviceCommand.getId(deviceId, accountId);
      this.deviceId = deviceId;
      this.accountId = accountId;
      this.lastLoginAt = lastLoginAt;
    }
  }

  private static String getId(String deviceId, String accountId) {
    return UUID.nameUUIDFromBytes(("UserDeviceAggregate." + deviceId + "." + accountId).getBytes())
        .toString();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TrustUserDeviceCommand {
    @TargetAggregateIdentifier private String id;

    @Builder
    public TrustUserDeviceCommand(String deviceId, String accountId) {
      Objects.requireNonNull(deviceId);
      Objects.requireNonNull(accountId);
      this.id = UserDeviceCommand.getId(deviceId, accountId);
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RemoveTrustUserDeviceCommand {
    @TargetAggregateIdentifier private String id;

    @Builder
    public RemoveTrustUserDeviceCommand(String deviceId, String accountId) {
      Objects.requireNonNull(deviceId);
      Objects.requireNonNull(accountId);
      this.id = UserDeviceCommand.getId(deviceId, accountId);
    }
  }
}
