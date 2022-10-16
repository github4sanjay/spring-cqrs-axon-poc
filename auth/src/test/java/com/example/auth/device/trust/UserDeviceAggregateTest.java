package com.example.auth.device.trust;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserDeviceAggregateTest {

  private FixtureConfiguration<UserDeviceAggregate> fixture;

  @BeforeEach
  public void setUp() {
    fixture = new AggregateTestFixture<>(UserDeviceAggregate.class);
  }

  @Test
  @DisplayName("test register new user device")
  public void testRegisterUserDevice() {

    var command =
        UserDeviceCommand.RegisterUserDeviceCommand.builder()
            .deviceId(UUID.randomUUID().toString())
            .accountId(UUID.randomUUID().toString())
            .lastLoginAt(Instant.now())
            .build();

    fixture
        .given()
        .when(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(
            UserDeviceEvent.UserDeviceRegisteredEvent.builder()
                .id(command.getId())
                .deviceId(command.getDeviceId())
                .accountId(command.getAccountId())
                .lastLoginAt(command.getLastLoginAt())
                .isTrusted(false)
                .build());
  }

  @Test
  @DisplayName(
      "test register user device when user device already created should create UserDeviceLastLoginUpdatedEvent")
  public void
      testRegisterUserDeviceWhenUserDeviceAlreadyCreatedShouldCreateUserDeviceLastLoginUpdatedEvent() {

    var command =
        UserDeviceCommand.RegisterUserDeviceCommand.builder()
            .deviceId(UUID.randomUUID().toString())
            .accountId(UUID.randomUUID().toString())
            .lastLoginAt(Instant.now())
            .build();

    fixture
        .given(
            UserDeviceEvent.UserDeviceRegisteredEvent.builder()
                .id(command.getId())
                .deviceId(command.getDeviceId())
                .accountId(command.getAccountId())
                .lastLoginAt(command.getLastLoginAt())
                .isTrusted(false)
                .build())
        .when(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(
            UserDeviceEvent.UserDeviceLastLoginUpdatedEvent.builder()
                .id(command.getId())
                .lastLoginAt(command.getLastLoginAt())
                .build());
  }

  @Test
  @DisplayName(
      "test given UserDeviceRegisteredEvent when TrustUserDeviceCommand should create UserDeviceTrustedEvent")
  public void
      testGivenUserDeviceRegisteredEventWhenTrustUserDeviceCommandShouldCreateUserDeviceTrustedEvent() {

    var deviceId = UUID.randomUUID().toString();
    var accountId = UUID.randomUUID().toString();
    var command =
        UserDeviceCommand.TrustUserDeviceCommand.builder()
            .deviceId(deviceId)
            .accountId(accountId)
            .build();

    fixture
        .given(
            UserDeviceEvent.UserDeviceRegisteredEvent.builder()
                .id(command.getId())
                .deviceId(deviceId)
                .accountId(accountId)
                .lastLoginAt(Instant.now())
                .isTrusted(false)
                .build())
        .when(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(UserDeviceEvent.UserDeviceTrustedEvent.builder().id(command.getId()).build());
  }

  @Test
  @DisplayName(
      "test given UserDeviceRegisteredEvent and UserDeviceTrustedEvent when RemoveTrustUserDeviceCommand should create UserDeviceTrustRemovedEvent")
  public void
      testGivenUserDeviceRegisteredEventAndUserDeviceTrustedEventWhenRemoveTrustUserDeviceCommandShouldCreateUserDeviceTrustRemovedEvent() {

    var deviceId = UUID.randomUUID().toString();
    var accountId = UUID.randomUUID().toString();
    var command =
        UserDeviceCommand.RemoveTrustUserDeviceCommand.builder()
            .deviceId(deviceId)
            .accountId(accountId)
            .build();

    fixture
        .given(
            UserDeviceEvent.UserDeviceRegisteredEvent.builder()
                .id(command.getId())
                .deviceId(deviceId)
                .accountId(accountId)
                .lastLoginAt(Instant.now())
                .isTrusted(false)
                .build(),
            UserDeviceEvent.UserDeviceTrustedEvent.builder().id(command.getId()).build())
        .when(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(
            UserDeviceEvent.UserDeviceTrustRemovedEvent.builder().id(command.getId()).build());
  }
}
