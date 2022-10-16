package com.example.auth.account;

import com.example.auth.AuthException;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.Device;
import com.example.spring.core.exceptions.ApplicationException;
import java.util.UUID;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AccountController implements AccountAPI {

  private final ReactorCommandGateway commandGateway;
  private final ReactorQueryGateway queryGateway;
  private final AccountConfig accountConfig;
  private final ClientConfiguration clientConfiguration;

  @Override
  public Mono<AccountResponse> registerAccount(AccountRequest request, Device device) {
    var clientConfig = clientConfiguration.getCurrentClient(device.getClient());
    var clientConfigAccount = clientConfig.getAccount();
    if (!clientConfigAccount.getEnabled()) throw AuthException.ACCOUNT_CREATION_NOT_ENABLED.getEx();

    var accountId = UUID.randomUUID().toString();
    return queryGateway
        .query(
            AccountQuery.GetAccountByEmailQuery.builder().email(request.getEmail()).build(),
            Account.class)
        .onErrorResume(
            throwable -> throwable instanceof ApplicationException,
            throwable -> {
              var applicationException = (ApplicationException) throwable;
              if (applicationException
                  .getCode()
                  .equals(AuthException.ACCOUNT_NOT_FOUND.getEx().getCode())) {
                var password = accountConfig.getHash(request.getPassword());
                var command =
                    AccountCommand.CreateAccountCommand.builder()
                        .id(accountId)
                        .email(request.getEmail())
                        .password(password)
                        .saltLength(AccountConfig.SALT_LENGTH)
                        .hashLength(AccountConfig.HASH_LENGTH)
                        .build();

                return commandGateway
                    .send(command)
                    .publishOn(Schedulers.boundedElastic())
                    .mapNotNull(
                        o ->
                            queryGateway
                                .subscriptionQuery(
                                    AccountQuery.GetAccountByIdQuery.builder()
                                        .id(command.getId())
                                        .build(),
                                    Account.class)
                                .blockFirst());
              } else {
                throw applicationException;
              }
            })
        .map(
            account -> {
              if (!accountId.equals(account.getId())) {
                throw AuthException.ACCOUNT_ALREADY_EXIST.getEx();
              }
              return AccountResponse.builder().id(UUID.fromString(account.getId())).build();
            });
  }
}
