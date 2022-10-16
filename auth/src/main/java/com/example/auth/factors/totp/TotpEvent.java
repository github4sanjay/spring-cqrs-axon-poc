package com.example.auth.factors.totp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.serialization.Revision;

public class TotpEvent {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class TotpCreatedEvent {
    private String id;
    private String accountId;
    private String secret;
    private String recoveryCode;
    private byte[] iv;
  }
}
