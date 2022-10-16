package com.example.auth.device;

import java.util.List;
import lombok.*;

public class DeviceQuery {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GetDeviceByIdQuery {
    private String id;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GetDeviceByClientCodeQuery {
    private String clientCode;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GetDevicesByIdsQuery {
    private List<String> ids;
  }
}
