package com.example.auth.factors;

import com.example.auth.AuthException;
import com.example.spring.axon.reactor.ApplicationCommandExecutionException;
import java.util.UUID;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FactorAggregateTest {

  private FixtureConfiguration<FactorAggregate> fixture;

  @BeforeEach
  public void setUp() {
    fixture = new AggregateTestFixture<>(FactorAggregate.class);
  }

  @Test
  @DisplayName("test given CreateFactorCommand it creates FactorCreatedEvent")
  public void testCreateFactorCommandCreatesFactorCreatedEvent() {
    var command =
        FactorCommand.CreateFactorCommand.builder()
            .factorType(FactorType.totp)
            .accountId(UUID.randomUUID().toString())
            .build();
    fixture
        .given()
        .when(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(
            FactorEvent.FactorCreatedEvent.builder()
                .id(command.getId())
                .factorType(command.getFactorType())
                .accountId(command.getAccountId())
                .build());
  }

  @Test
  @DisplayName(
      "test given FactorCreatedEvent when ActivateFactorCommand expect FactorActivatedEvent")
  public void testGivenFactorCreatedEventWhenActivateFactorCommandExpectFactorActivatedEvent() {
    var accountId = UUID.randomUUID().toString();
    var command =
        FactorCommand.ActivateFactorCommand.builder()
            .factorType(FactorType.totp)
            .accountId(accountId)
            .build();

    fixture
        .given(
            FactorEvent.FactorCreatedEvent.builder()
                .id(command.getId())
                .factorType(FactorType.totp)
                .accountId(accountId)
                .build())
        .when(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(FactorEvent.FactorActivatedEvent.builder().id(command.getId()).build());
  }

  @Test
  @DisplayName(
      "test given FactorCreatedEvent and FactorActivatedEvent when ActivateFactorCommand expect FACTOR_ALREADY_ACTIVE error")
  public void
      testGivenFactorCreatedEventAndFactorActivatedEventWhenActivateFactorCommandExpectFactorAlreadyActiveError() {
    var accountId = UUID.randomUUID().toString();
    var command =
        FactorCommand.ActivateFactorCommand.builder()
            .factorType(FactorType.totp)
            .accountId(accountId)
            .build();

    fixture
        .given(
            FactorEvent.FactorCreatedEvent.builder()
                .id(command.getId())
                .factorType(FactorType.totp)
                .accountId(accountId)
                .build(),
            FactorEvent.FactorActivatedEvent.builder().id(command.getId()).build())
        .when(command)
        .expectException(ApplicationCommandExecutionException.class)
        .expectExceptionMessage(AuthException.FACTOR_ALREADY_ACTIVE.getEx().getCode());
  }

  @Test
  @DisplayName(
      "test given FactorCreatedEvent and FactorActivatedEvent when DeactivateFactorCommand expect FactorDeactivatedEvent")
  public void
      testGivenFactorCreatedEventAndFactorActivatedEventWhenActivateFactorCommandExpectFactorDeactivatedEvent() {
    var accountId = UUID.randomUUID().toString();
    var command =
        FactorCommand.DeactivateFactorCommand.builder()
            .factorType(FactorType.totp)
            .accountId(accountId)
            .build();

    fixture
        .given(
            FactorEvent.FactorCreatedEvent.builder()
                .id(command.getId())
                .factorType(FactorType.totp)
                .accountId(accountId)
                .build(),
            FactorEvent.FactorActivatedEvent.builder().id(command.getId()).build())
        .when(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(FactorEvent.FactorDeactivatedEvent.builder().id(command.getId()).build());
  }

  @Test
  @DisplayName(
      "test given FactorCreatedEvent when DeactivateFactorCommand expect FACTOR_NOT_FOUND error")
  public void testGivenFactorCreatedEventWhenActivateFactorCommandExpectFactorNotFoundError() {
    var accountId = UUID.randomUUID().toString();
    var command =
        FactorCommand.DeactivateFactorCommand.builder()
            .factorType(FactorType.totp)
            .accountId(accountId)
            .build();

    fixture
        .given(
            FactorEvent.FactorCreatedEvent.builder()
                .id(command.getId())
                .factorType(FactorType.totp)
                .accountId(accountId)
                .build())
        .when(command)
        .expectException(ApplicationCommandExecutionException.class)
        .expectExceptionMessage(AuthException.FACTOR_NOT_FOUND.getEx().getCode());
  }
}
