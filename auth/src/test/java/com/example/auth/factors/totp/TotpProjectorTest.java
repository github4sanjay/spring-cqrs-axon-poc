package com.example.auth.factors.totp;

import com.example.auth.AuthException;
import com.example.auth.account.*;
import com.example.security.core.AESUtil;
import com.example.spring.axon.reactor.ApplicationQueryExecutionException;
import com.example.spring.core.exceptions.IException;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@Import(value = {TotpProjector.class})
class TotpProjectorTest {

  @Autowired private TotpProjector totpProjector;
  @Autowired private TotpRepository totpRepository;

  @BeforeEach
  public void beforeEach() {
    totpRepository.deleteAll();
  }

  @Test
  @DisplayName("test given TotpCreatedEvent should save data in db")
  public void testGivenTotpCreatedEventShouldSaveDataInDb() {
    var event =
        TotpEvent.TotpCreatedEvent.builder()
            .id(UUID.randomUUID().toString())
            .recoveryCode(UUID.randomUUID().toString())
            .accountId(UUID.randomUUID().toString())
            .iv(AESUtil.generateIv().getIV())
            .secret("totpSecret")
            .build();
    totpProjector.on(event);
    var totp =
        totpProjector.on(
            TotpQuery.GetTotpByAccountIdQuery.builder().accountId(event.getAccountId()).build());
    Assertions.assertNotNull(totp.getSecret());
    Assertions.assertNotNull(totp.getIv());
    Assertions.assertNotNull(totp.getAccountId());
    Assertions.assertNotNull(totp.getRecoveryCode());
  }

  @Test
  @DisplayName(
      "test GetTotpByAccountIdQuery when totp not present should return factor not found exception")
  public void testGetTotpByAccountIdQueryWhenTotpNotPresentShouldReturnFactorNotFoundException() {
    var exception =
        Assertions.assertThrows(
            ApplicationQueryExecutionException.class,
            () ->
                totpProjector.on(
                    TotpQuery.GetTotpByAccountIdQuery.builder()
                        .accountId(UUID.randomUUID().toString())
                        .build()));
    Assertions.assertTrue(exception.getDetails().isPresent());
    var iException = (IException) exception.getDetails().get();
    Assertions.assertEquals(
        AuthException.FACTOR_NOT_FOUND.getEx().getCode(), iException.getEx().getCode());
  }
}
