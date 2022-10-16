package com.example.auth.factors;

import com.example.auth.AuthException;
import com.example.spring.axon.reactor.ApplicationCommandExecutionException;
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
public class FactorAggregate {

  @AggregateIdentifier private String id;
  private String accountId;
  private FactorType factorType;
  private Boolean enabled;

  @CommandHandler
  @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
  public String on(FactorCommand.CreateFactorCommand command) {
    if (id == null) {
      var event =
          FactorEvent.FactorCreatedEvent.builder()
              .accountId(command.getAccountId())
              .factorType(command.getFactorType())
              .id(command.getId())
              .build();
      AggregateLifecycle.apply(event);
    } else if (enabled) {
      throw new ApplicationCommandExecutionException(AuthException.FACTOR_ALREADY_ACTIVE);
    }
    return command.getId();
  }

  @CommandHandler
  public String on(FactorCommand.ActivateFactorCommand command) {
    if (enabled) {
      throw new ApplicationCommandExecutionException(AuthException.FACTOR_ALREADY_ACTIVE);
    }
    var event = FactorEvent.FactorActivatedEvent.builder().id(command.getId()).build();
    AggregateLifecycle.apply(event);
    return command.getId();
  }

  @CommandHandler
  public String on(FactorCommand.DeactivateFactorCommand command) {
    if (!enabled) {
      throw new ApplicationCommandExecutionException(AuthException.FACTOR_NOT_FOUND);
    }
    var event = FactorEvent.FactorDeactivatedEvent.builder().id(command.getId()).build();
    AggregateLifecycle.apply(event);
    return command.getId();
  }

  @EventSourcingHandler
  public void on(FactorEvent.FactorCreatedEvent event) {
    this.id = event.getId();
    this.accountId = event.getAccountId();
    this.factorType = event.getFactorType();
    this.enabled = false;
  }

  @EventSourcingHandler
  public void on(FactorEvent.FactorActivatedEvent event) {
    this.enabled = true;
  }

  @EventSourcingHandler
  public void on(FactorEvent.FactorDeactivatedEvent event) {
    this.enabled = false;
  }
}
