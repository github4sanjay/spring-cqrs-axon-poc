package com.example.auth.account;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Slf4j
@Data
@NoArgsConstructor
@Aggregate(snapshotTriggerDefinition = "snapshotTriggerDefinition")
public class AccountAggregate {

  @AggregateIdentifier private String id;
  private Integer saltLength;
  private Integer hashLength;
  private String email;
  private String password;
  private AccountStatus status;

  @CommandHandler
  public AccountAggregate(AccountCommand.CreateAccountCommand command) {
    var event =
        AccountEvent.AccountCreatedEvent.builder()
            .hashLength(command.getHashLength())
            .email(command.getEmail())
            .id(command.getId())
            .saltLength(command.getSaltLength())
            .password(command.getPassword())
            .status(AccountStatus.ACTIVE)
            .build();
    AggregateLifecycle.apply(event);
  }

  @EventSourcingHandler
  public void on(AccountEvent.AccountCreatedEvent event) {
    this.id = event.getId();
    this.email = event.getEmail();
    this.hashLength = event.getHashLength();
    this.saltLength = event.getSaltLength();
    this.password = event.getPassword();
    this.status = event.getStatus();
  }
}
