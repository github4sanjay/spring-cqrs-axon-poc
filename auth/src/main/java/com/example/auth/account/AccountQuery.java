package com.example.auth.account;

import lombok.*;

public class AccountQuery {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GetAccountByIdQuery {
    private String id;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GetAccountByEmailQuery {
    private String email;
  }
}
