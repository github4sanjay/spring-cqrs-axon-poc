package com.example.auth.account;

import com.example.auth.AuthException;
import com.example.spring.axon.reactor.ApplicationQueryExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup("account")
public class AccountProjector {

  private final AccountRepository accountRepository;
  private final QueryUpdateEmitter queryUpdateEmitter;

  @EventHandler
  public void on(AccountEvent.AccountCreatedEvent event) {
    var account =
        Account.builder()
            .hashLength(event.getHashLength())
            .email(event.getEmail())
            .id(event.getId())
            .saltLength(event.getSaltLength())
            .password(event.getPassword())
            .status(event.getStatus())
            .build();

    accountRepository.save(account);
    queryUpdateEmitter.emit(
        AccountQuery.GetAccountByIdQuery.class,
        getAccountByIdQuery -> getAccountByIdQuery.getId().equals(account.getId()),
        account);
  }

  @QueryHandler
  public Account on(AccountQuery.GetAccountByIdQuery query) {
    return accountRepository
        .findById(query.getId())
        .orElseThrow(() -> new ApplicationQueryExecutionException(AuthException.ACCOUNT_NOT_FOUND));
  }

  @QueryHandler
  public Account on(AccountQuery.GetAccountByEmailQuery query) {
    return accountRepository
        .findAccountByEmail(query.getEmail())
        .orElseThrow(() -> new ApplicationQueryExecutionException(AuthException.ACCOUNT_NOT_FOUND));
  }
}
