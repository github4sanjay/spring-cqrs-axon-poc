package com.example.auth.factors.totp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TotpQuery {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GetTotpByAccountIdQuery {
    private String accountId;
  }
}
