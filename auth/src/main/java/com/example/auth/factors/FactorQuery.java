package com.example.auth.factors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FactorQuery {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GetFactorByAccountIdAndFactorTypeQuery {
    private String accountId;
    private FactorType factorType;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GetFactorByAccountIdQuery {
    private String accountId;
  }
}
