package com.example.auth.factors.totp;

import com.example.security.core.AESUtil;
import java.util.UUID;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TotpAggregateTest {

  private FixtureConfiguration<TotpAggregate> fixture;

  @BeforeEach
  public void setUp() {
    fixture = new AggregateTestFixture<>(TotpAggregate.class);
  }

  @Test
  @DisplayName("test given CreateTotpCommand it creates TotpCreatedEvent")
  public void testGiveCreateTotpCommandCreatesTotpCreatedEvent() {
    var command =
        TotpCommand.CreateTotpCommand.builder()
            .recoveryCode(UUID.randomUUID().toString())
            .accountId(UUID.randomUUID().toString())
            .iv(AESUtil.generateIv().getIV())
            .secret("totpSecret")
            .build();
    fixture
        .given()
        .when(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(
            TotpEvent.TotpCreatedEvent.builder()
                .id(command.getId())
                .recoveryCode(command.getRecoveryCode())
                .accountId(command.getAccountId())
                .iv(command.getIv())
                .secret(command.getSecret())
                .build());
  }
}
