package com.example.auth.device;

import lombok.*;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class DeviceCommand {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RegisterDeviceCommand {

    @TargetAggregateIdentifier private String id;
    private Integer saltLength;
    private Integer hashLength;
    private String hash;
    private String publicKey;
    private String client;
    private String name;
    private String model;
    private String manufacturer;
    private String os;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateDeviceNameCommand {

    @TargetAggregateIdentifier private String id;
    private String name;
  }
}
