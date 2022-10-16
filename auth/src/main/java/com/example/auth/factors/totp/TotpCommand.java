package com.example.auth.factors.totp;

import java.util.Objects;
import java.util.UUID;
import lombok.*;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class TotpCommand {

  @Data
  @NoArgsConstructor
  public static class CreateTotpCommand {
    @TargetAggregateIdentifier private String id;
    private String accountId;
    private String recoveryCode;
    private String secret;
    private byte[] iv;

    @Builder
    public CreateTotpCommand(String accountId, String recoveryCode, String secret, byte[] iv) {
      Objects.requireNonNull(accountId);
      Objects.requireNonNull(recoveryCode);
      Objects.requireNonNull(secret);
      Objects.requireNonNull(iv);
      this.id = CreateTotpCommand.getId(accountId);
      this.accountId = accountId;
      this.secret = secret;
      this.recoveryCode = recoveryCode;
      this.iv = iv;
    }

    private static String getId(String accountId) {
      return UUID.nameUUIDFromBytes(("TotpAggregate." + "." + accountId).getBytes()).toString();
    }
  }
}
