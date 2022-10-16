package com.example.auth.token.refresh;

import lombok.*;

public class RefreshTokenQuery {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GetRefreshTokenByDeviceIdAndTokenQuery {
    private String deviceId;
    private String token;
  }
}
