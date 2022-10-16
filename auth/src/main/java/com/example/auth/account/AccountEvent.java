package com.example.auth.account;

import lombok.*;
import org.axonframework.serialization.Revision;

public class AccountEvent {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class AccountCreatedEvent {
    private String id;
    private Integer saltLength;
    private Integer hashLength;
    private String email;
    private String password;
    private AccountStatus status;
  }
}
