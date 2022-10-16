package com.example.auth.factors;

import com.example.auth.AuthException;
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
@Import(value = {FactorProjector.class})
class FactorProjectorTest {

  @Autowired private FactorProjector factorProjector;
  @Autowired private FactorRepository factorRepository;

  @BeforeEach
  public void beforeEach() {
    factorRepository.deleteAll();
  }

  @Test
  @DisplayName("test given FactorCreatedEvent should save data in db")
  public void testGivenTotpCreatedEventShouldSaveDataInDb() {
    var event =
        FactorEvent.FactorCreatedEvent.builder()
            .id(UUID.randomUUID().toString())
            .factorType(FactorType.email)
            .accountId(UUID.randomUUID().toString())
            .build();
    factorProjector.on(event);
    var factor =
        factorProjector.on(
            FactorQuery.GetFactorByAccountIdAndFactorTypeQuery.builder()
                .factorType(FactorType.email)
                .accountId(event.getAccountId())
                .build());
    Assertions.assertNotNull(factor.getFactorType());
    Assertions.assertNotNull(factor.getAccountId());
    Assertions.assertNotNull(factor.getEnabled());
    Assertions.assertNotNull(factor.getId());
  }

  @Test
  @DisplayName(
      "test GetTotpByAccountIdQuery when totp not present should return factor not found exception")
  public void testGetTotpByAccountIdQueryWhenTotpNotPresentShouldReturnFactorNotFoundException() {
    var exception =
        Assertions.assertThrows(
            ApplicationQueryExecutionException.class,
            () ->
                factorProjector.on(
                    FactorQuery.GetFactorByAccountIdAndFactorTypeQuery.builder()
                        .factorType(FactorType.email)
                        .accountId(UUID.randomUUID().toString())
                        .build()));
    Assertions.assertTrue(exception.getDetails().isPresent());
    var iException = (IException) exception.getDetails().get();
    Assertions.assertEquals(
        AuthException.FACTOR_NOT_FOUND.getEx().getCode(), iException.getEx().getCode());
  }

  @Test
  @DisplayName("test given FactorActivatedEvent should factor should be enabled true in db")
  public void testGiveFactorActivatedEventShouldBeEnabledTrueInDb() {
    var factor =
        factorRepository.save(
            Factor.builder()
                .id(UUID.randomUUID().toString())
                .factorType(FactorType.email)
                .accountId(UUID.randomUUID().toString())
                .enabled(false)
                .build());
    var event = FactorEvent.FactorActivatedEvent.builder().id(factor.getId()).build();
    factorProjector.on(event);
    factor =
        factorRepository
            .findByAccountIdAndFactorType(factor.getAccountId(), FactorType.email)
            .orElseThrow();
    Assertions.assertTrue(factor.getEnabled());
  }

  @Test
  @DisplayName("test given FactorDeactivatedEvent should factor should be enabled false in db")
  public void testGiveFactorActivatedEventShouldBeEnabledFalseInDb() {
    var factor =
        factorRepository.save(
            Factor.builder()
                .id(UUID.randomUUID().toString())
                .factorType(FactorType.email)
                .accountId(UUID.randomUUID().toString())
                .enabled(false)
                .build());

    factorProjector.on(FactorEvent.FactorActivatedEvent.builder().id(factor.getId()).build());
    factor =
        factorRepository
            .findByAccountIdAndFactorType(factor.getAccountId(), FactorType.email)
            .orElseThrow();
    Assertions.assertTrue(factor.getEnabled());

    factorProjector.on(FactorEvent.FactorDeactivatedEvent.builder().id(factor.getId()).build());
    factor =
        factorRepository
            .findByAccountIdAndFactorType(factor.getAccountId(), FactorType.email)
            .orElseThrow();
    Assertions.assertFalse(factor.getEnabled());
  }

  @Test
  @DisplayName(
      "test given factors in db when GetFactorByAccountIdQuery should return all the factors")
  public void testGivenFactorsInDbWhenGetFactorByAccountIdQueryShouldReturnAllFactors() {

    var accountId = UUID.randomUUID().toString();
    factorRepository.save(
        Factor.builder()
            .id(UUID.randomUUID().toString())
            .factorType(FactorType.email)
            .accountId(accountId)
            .enabled(false)
            .build());

    factorRepository.save(
        Factor.builder()
            .id(UUID.randomUUID().toString())
            .factorType(FactorType.totp)
            .accountId(accountId)
            .enabled(false)
            .build());

    var factors =
        factorProjector.on(
            FactorQuery.GetFactorByAccountIdQuery.builder().accountId(accountId).build());
    Assertions.assertEquals(2, factors.size());
  }
}
