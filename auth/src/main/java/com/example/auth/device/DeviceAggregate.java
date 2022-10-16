package com.example.auth.device;

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
public class DeviceAggregate {

  @AggregateIdentifier private String id;
  private Integer saltLength;
  private Integer hashLength;
  private String hash;
  private String publicKey;
  private String name;
  private String client;
  private String model;
  private String manufacturer;
  private String os;

  @CommandHandler
  public DeviceAggregate(DeviceCommand.RegisterDeviceCommand registerDeviceCommand) {
    var event =
        DeviceEvent.DeviceRegisteredEvent.builder()
            .publicKey(registerDeviceCommand.getPublicKey())
            .id(registerDeviceCommand.getId())
            .hashLength(registerDeviceCommand.getHashLength())
            .saltLength(registerDeviceCommand.getSaltLength())
            .hash(registerDeviceCommand.getHash())
            .os(registerDeviceCommand.getOs())
            .client(registerDeviceCommand.getClient())
            .name(registerDeviceCommand.getName())
            .model(registerDeviceCommand.getModel())
            .manufacturer(registerDeviceCommand.getManufacturer())
            .build();
    AggregateLifecycle.apply(event);
  }

  @CommandHandler
  public String on(DeviceCommand.UpdateDeviceNameCommand command) {
    var event =
        DeviceEvent.DeviceNameUpdatedEvent.builder()
            .id(command.getId())
            .name(command.getName())
            .build();
    AggregateLifecycle.apply(event);
    return command.getId();
  }

  @EventSourcingHandler
  public void on(DeviceEvent.DeviceRegisteredEvent event) {
    this.id = event.getId();
    this.saltLength = event.getSaltLength();
    this.hashLength = event.getHashLength();
    this.hash = event.getHash();
    this.publicKey = event.getPublicKey();
    this.name = event.getName();
    this.manufacturer = event.getManufacturer();
    this.os = event.getOs();
    this.client = event.getClient();
    this.model = event.getModel();
  }

  @EventSourcingHandler
  public void on(DeviceEvent.DeviceNameUpdatedEvent event) {
    this.name = event.getName();
  }
}
