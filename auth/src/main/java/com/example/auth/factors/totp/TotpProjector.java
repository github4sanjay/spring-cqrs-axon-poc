package com.example.auth.factors.totp;

import com.example.auth.AuthException;
import com.example.spring.axon.reactor.ApplicationQueryExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup("totp")
public class TotpProjector {

  private final TotpRepository totpRepository;

  @EventHandler
  public void on(TotpEvent.TotpCreatedEvent event) {
    var totp =
        Totp.builder()
            .accountId(event.getAccountId())
            .secret(event.getSecret())
            .iv(event.getIv())
            .recoveryCode(event.getRecoveryCode())
            .build();
    totpRepository.save(totp);
  }

  @QueryHandler
  public Totp on(TotpQuery.GetTotpByAccountIdQuery query) {
    return totpRepository
        .findByAccountId(query.getAccountId())
        .orElseThrow(() -> new ApplicationQueryExecutionException(AuthException.FACTOR_NOT_FOUND));
  }
}
