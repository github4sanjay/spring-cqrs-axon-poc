package com.example.auth.factors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.serialization.Revision;

public class FactorEvent {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class FactorCreatedEvent {
    private String id;
    private String accountId;
    private FactorType factorType;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class FactorActivatedEvent {
    private String id;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class FactorDeactivatedEvent {
    private String id;
  }
}
