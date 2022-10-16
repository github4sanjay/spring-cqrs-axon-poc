package com.example.auth.device;

import java.util.UUID;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeviceAggregateTest {

  private FixtureConfiguration<DeviceAggregate> fixture;

  @BeforeEach
  public void setUp() {
    fixture = new AggregateTestFixture<>(DeviceAggregate.class);
  }

  @Test
  @DisplayName("register new device")
  public void testRegisterDevice() {

    var command =
        DeviceCommand.RegisterDeviceCommand.builder()
            .publicKey("publicKey")
            .id(UUID.randomUUID().toString())
            .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
            .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
            .hash("some-hash")
            .client("web-app")
            .manufacturer("Windows")
            .os("windows")
            .model("DFFG-123")
            .name("Some Name")
            .build();

    fixture
        .given()
        .when(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(
            DeviceEvent.DeviceRegisteredEvent.builder()
                .publicKey(command.getPublicKey())
                .id(command.getId())
                .hashLength(command.getHashLength())
                .saltLength(command.getSaltLength())
                .hash(command.getHash())
                .client(command.getClient())
                .manufacturer(command.getManufacturer())
                .os(command.getOs())
                .model(command.getModel())
                .name(command.getName())
                .build());
  }

  @Test
  @DisplayName(
      "test given DeviceRegisteredEvent when UpdateDeviceNameCommand expect DeviceNameUpdatedEvent")
  public void
      testGivenDeviceRegisteredEventWhenUpdateDeviceNameCommandExpectDeviceNameUpdatedEvent() {

    var command =
        DeviceCommand.UpdateDeviceNameCommand.builder()
            .id(UUID.randomUUID().toString())
            .name("Some Name changed")
            .build();

    fixture
        .given(
            DeviceEvent.DeviceRegisteredEvent.builder()
                .publicKey("publicKey")
                .id(command.getId())
                .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
                .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
                .hash("some-hash")
                .client("web-app")
                .manufacturer("Windows")
                .os("windows")
                .model("DFFG-123")
                .name("Some Name")
                .build())
        .when(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(
            DeviceEvent.DeviceNameUpdatedEvent.builder()
                .name(command.getName())
                .id(command.getId())
                .build());
  }
}
