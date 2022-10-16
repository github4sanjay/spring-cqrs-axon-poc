package com.example.auth.account;

import com.example.auth.AuthException;
import com.example.spring.axon.reactor.ApplicationQueryExecutionException;
import com.example.spring.core.exceptions.IException;
import java.util.UUID;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@Import(value = {AccountProjector.class})
class AccountProjectorTest {

  @MockBean private QueryUpdateEmitter queryUpdateEmitter;
  @Autowired private AccountProjector accountProjector;
  @Autowired private AccountRepository accountRepository;

  @BeforeEach
  public void beforeEach() {
    accountRepository.deleteAll();
  }

  @Test
  @DisplayName("test device registered event handler should save data in db")
  public void testDeviceRegisteredEventHandlerShouldSaveDataInDb() {
    var event =
        AccountEvent.AccountCreatedEvent.builder()
            .hashLength(16)
            .email("github4sanjay@gmail.com")
            .id(UUID.randomUUID().toString())
            .saltLength(32)
            .password("password")
            .status(AccountStatus.ACTIVE)
            .build();
    accountProjector.on(event);
    var mayBeAccount = accountRepository.findById(event.getId().toString());
    Assertions.assertTrue(mayBeAccount.isPresent());
    var account = mayBeAccount.get();
    Assertions.assertNotNull(account.getPassword());
    Assertions.assertNotNull(account.getEmail());
    Assertions.assertNotNull(account.getStatus());
    Assertions.assertNotNull(account.getHashLength());
    Assertions.assertNotNull(account.getSaltLength());
  }

  @Test
  @DisplayName(
      "test GetAccountByIdQuery when account not present should return account not found exception")
  public void testGetAccountByIdQueryWhenAccountNotPresentShouldReturnAccountNotFoundException() {
    var exception =
        Assertions.assertThrows(
            ApplicationQueryExecutionException.class,
            () ->
                accountProjector.on(
                    AccountQuery.GetAccountByIdQuery.builder()
                        .id(UUID.randomUUID().toString())
                        .build()));
    Assertions.assertTrue(exception.getDetails().isPresent());
    var iException = (IException) exception.getDetails().get();
    Assertions.assertEquals(
        AuthException.ACCOUNT_NOT_FOUND.getEx().getCode(), iException.getEx().getCode());
  }

  @Test
  @DisplayName("test GetAccountByIdQuery when account present should return account")
  public void testGetAccountByIdQueryWhenAccountPresentShouldReturnAccount() {
    var id = UUID.randomUUID().toString();
    accountRepository.save(
        Account.builder()
            .id(id.toString())
            .hashLength(16)
            .email("github4sanjay@gmail.com")
            .saltLength(32)
            .password("password")
            .status(AccountStatus.ACTIVE)
            .build());
    var account = accountProjector.on(AccountQuery.GetAccountByIdQuery.builder().id(id).build());
    Assertions.assertEquals(id.toString(), account.getId());
  }

  @Test
  @DisplayName(
      "test GetAccountByEmailQuery when account not present should return account not found exception")
  public void
      testGetAccountByUsernameQueryWhenAccountNotPresentShouldReturnAccountNotFoundException() {
    var exception =
        Assertions.assertThrows(
            ApplicationQueryExecutionException.class,
            () ->
                accountProjector.on(
                    AccountQuery.GetAccountByEmailQuery.builder()
                        .email("github4sanjay@gmail.com")
                        .build()));
    Assertions.assertTrue(exception.getDetails().isPresent());
    var iException = (IException) exception.getDetails().get();
    Assertions.assertEquals(
        AuthException.ACCOUNT_NOT_FOUND.getEx().getCode(), iException.getEx().getCode());
  }

  @Test
  @DisplayName("test GetAccountByEmailQuery when account present should return account")
  public void testGetAccountByUsernameQueryWhenAccountPresentShouldReturnAccount() {
    var email = "github4sanjay@gmail.com";
    accountRepository.save(
        Account.builder()
            .id(UUID.randomUUID().toString())
            .hashLength(16)
            .email(email)
            .saltLength(32)
            .password("password")
            .status(AccountStatus.ACTIVE)
            .build());
    var account =
        accountProjector.on(AccountQuery.GetAccountByEmailQuery.builder().email(email).build());
    Assertions.assertEquals(email, account.getEmail());
  }
}
