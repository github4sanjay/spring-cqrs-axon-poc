package com.example.auth.device.trust;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserDeviceQuery {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GetUserDeviceByAccountIdQuery {
    private String accountId;
  }
}
