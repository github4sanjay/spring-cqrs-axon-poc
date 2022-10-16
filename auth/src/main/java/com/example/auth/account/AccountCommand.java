package com.example.auth.account;

import lombok.*;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class AccountCommand {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateAccountCommand {
    @TargetAggregateIdentifier private String id;
    private Integer saltLength;
    private Integer hashLength;
    private String email;
    private String password;
  }
}
