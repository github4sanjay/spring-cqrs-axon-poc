package com.example.auth.device.trust;

import java.time.Instant;
import lombok.*;
import org.axonframework.serialization.Revision;

public class UserDeviceEvent {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class UserDeviceRegisteredEvent {
    private String id;
    private String deviceId;
    private String accountId;
    private Instant lastLoginAt;
    private Boolean isTrusted;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class UserDeviceLastLoginUpdatedEvent {
    private String id;
    private Instant lastLoginAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class UserDeviceTrustedEvent {
    private String id;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class UserDeviceTrustRemovedEvent {
    private String id;
  }
}
