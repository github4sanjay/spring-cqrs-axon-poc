package com.example.auth.device;

import lombok.*;
import org.axonframework.serialization.Revision;

public class DeviceEvent {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class DeviceRegisteredEvent {
    private String id;
    private Integer saltLength;
    private Integer hashLength;
    private String hash;
    private String publicKey;
    private String name;
    private String client;
    private String model;
    private String manufacturer;
    private String os;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class DeviceNameUpdatedEvent {
    private String id;
    private String name;
  }
}
