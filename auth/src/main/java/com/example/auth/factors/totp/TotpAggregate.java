package com.example.auth.factors.totp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

@Slf4j
@Data
@NoArgsConstructor
@Aggregate(snapshotTriggerDefinition = "snapshotTriggerDefinition")
public class TotpAggregate {

  @AggregateIdentifier private String id;
  private String accountId;
  private String secret;
  private String recoveryCode;
  private byte[] iv;

  @CommandHandler
  @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
  public String on(TotpCommand.CreateTotpCommand command) {
    var event =
        TotpEvent.TotpCreatedEvent.builder()
            .accountId(command.getAccountId())
            .secret(command.getSecret())
            .id(command.getId())
            .iv(command.getIv())
            .recoveryCode(command.getRecoveryCode())
            .build();
    AggregateLifecycle.apply(event);
    return command.getId();
  }

  @EventSourcingHandler
  public void on(TotpEvent.TotpCreatedEvent event) {
    this.id = event.getId();
    this.accountId = event.getAccountId();
    this.secret = event.getSecret();
    this.iv = event.getIv();
    this.recoveryCode = event.getRecoveryCode();
  }
}
