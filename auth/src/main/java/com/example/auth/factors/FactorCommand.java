package com.example.auth.factors;

import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class FactorCommand {

  @Data
  @NoArgsConstructor
  public static class CreateFactorCommand {
    @TargetAggregateIdentifier private String id;
    private String accountId;
    private FactorType factorType;

    @Builder
    public CreateFactorCommand(String accountId, FactorType factorType) {
      Objects.requireNonNull(accountId);
      Objects.requireNonNull(factorType);
      this.id = FactorCommand.getId(accountId, factorType);
      this.accountId = accountId;
      this.factorType = factorType;
    }
  }

  @Data
  @NoArgsConstructor
  public static class ActivateFactorCommand {
    @TargetAggregateIdentifier private String id;

    @Builder
    public ActivateFactorCommand(String accountId, FactorType factorType) {
      Objects.requireNonNull(accountId);
      Objects.requireNonNull(factorType);
      this.id = FactorCommand.getId(accountId, factorType);
    }
  }

  @Data
  @NoArgsConstructor
  public static class DeactivateFactorCommand {
    @TargetAggregateIdentifier private String id;

    @Builder
    public DeactivateFactorCommand(String accountId, FactorType factorType) {
      Objects.requireNonNull(accountId);
      Objects.requireNonNull(factorType);
      this.id = FactorCommand.getId(accountId, factorType);
    }
  }

  private static String getId(String accountId, FactorType factorType) {
    return UUID.nameUUIDFromBytes(
            ("FactorAggregate." + factorType.name() + "." + accountId).getBytes())
        .toString();
  }
}
