package com.example.auth.device.trust;

import java.time.Instant;
import lombok.*;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Aggregate(snapshotTriggerDefinition = "snapshotTriggerDefinition")
public class UserDeviceAggregate {

  @AggregateIdentifier private String id;
  private String deviceId;
  private String accountId;
  private Boolean trusted;
  private String biometric;
  private Instant expireAt;
  private Instant lastLoginAt;

  @CommandHandler
  @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
  public String on(UserDeviceCommand.RegisterUserDeviceCommand command) {
    if (id == null) {
      var event =
          UserDeviceEvent.UserDeviceRegisteredEvent.builder()
              .id(command.getId())
              .deviceId(command.getDeviceId())
              .accountId(command.getAccountId())
              .lastLoginAt(command.getLastLoginAt())
              .isTrusted(false)
              .build();
      AggregateLifecycle.apply(event);
    } else {
      var event =
          UserDeviceEvent.UserDeviceLastLoginUpdatedEvent.builder()
              .id(command.getId())
              .lastLoginAt(command.getLastLoginAt())
              .build();
      AggregateLifecycle.apply(event);
    }
    return command.getId();
  }

  @CommandHandler
  public String on(UserDeviceCommand.TrustUserDeviceCommand command) {
    var event = UserDeviceEvent.UserDeviceTrustedEvent.builder().id(command.getId()).build();
    AggregateLifecycle.apply(event);
    return command.getId();
  }

  @CommandHandler
  public String on(UserDeviceCommand.RemoveTrustUserDeviceCommand command) {
    var event = UserDeviceEvent.UserDeviceTrustRemovedEvent.builder().id(command.getId()).build();
    AggregateLifecycle.apply(event);
    return command.getId();
  }

  @EventSourcingHandler
  public void on(UserDeviceEvent.UserDeviceRegisteredEvent event) {
    this.id = event.getId();
    this.deviceId = event.getDeviceId();
    this.accountId = event.getAccountId();
    this.lastLoginAt = event.getLastLoginAt();
    this.trusted = event.getIsTrusted();
  }

  @EventSourcingHandler
  public void on(UserDeviceEvent.UserDeviceLastLoginUpdatedEvent event) {
    this.lastLoginAt = event.getLastLoginAt();
  }

  @EventSourcingHandler
  public void on(UserDeviceEvent.UserDeviceTrustedEvent event) {
    this.trusted = true;
  }

  @EventSourcingHandler
  public void on(UserDeviceEvent.UserDeviceTrustRemovedEvent event) {
    this.trusted = false;
  }
}
