package com.example.auth.account;

import java.util.UUID;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountAggregateTest {

  private FixtureConfiguration<AccountAggregate> fixture;

  @BeforeEach
  public void setUp() {
    fixture = new AggregateTestFixture<>(AccountAggregate.class);
  }

  @Test
  @DisplayName("can create account")
  public void canCreateAccount() {
    var command =
        AccountCommand.CreateAccountCommand.builder()
            .id(UUID.randomUUID().toString())
            .email("github4sanjay@gmail.com")
            .password("password")
            .saltLength(AccountConfig.SALT_LENGTH)
            .hashLength(AccountConfig.HASH_LENGTH)
            .build();
    fixture
        .given()
        .when(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(
            AccountEvent.AccountCreatedEvent.builder()
                .id(command.getId())
                .email(command.getEmail())
                .password(command.getPassword())
                .saltLength(command.getSaltLength())
                .hashLength(command.getHashLength())
                .status(AccountStatus.ACTIVE)
                .build());
  }
}
