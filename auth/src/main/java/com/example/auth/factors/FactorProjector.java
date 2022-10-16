package com.example.auth.factors;

import com.example.auth.AuthException;
import com.example.spring.axon.reactor.ApplicationQueryExecutionException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup("factor")
public class FactorProjector {

  private final FactorRepository factorRepository;

  @EventHandler
  public void on(FactorEvent.FactorCreatedEvent event) {
    var factor =
        Factor.builder()
            .factorType(event.getFactorType())
            .accountId(event.getAccountId())
            .id(event.getId())
            .enabled(false)
            .build();
    factorRepository.save(factor);
  }

  @EventHandler
  public void on(FactorEvent.FactorActivatedEvent event) {
    var factor = factorRepository.findById(event.getId()).orElseThrow();
    factor.setEnabled(true);
    factorRepository.save(factor);
  }

  @EventHandler
  public void on(FactorEvent.FactorDeactivatedEvent event) {
    var factor = factorRepository.findById(event.getId()).orElseThrow();
    factor.setEnabled(false);
    factorRepository.save(factor);
  }

  @QueryHandler
  public List<Factor> on(FactorQuery.GetFactorByAccountIdQuery query) {
    return factorRepository.findAllByAccountId(query.getAccountId());
  }

  @QueryHandler
  public Factor on(FactorQuery.GetFactorByAccountIdAndFactorTypeQuery query) {
    return factorRepository
        .findByAccountIdAndFactorType(query.getAccountId(), query.getFactorType())
        .orElseThrow(() -> new ApplicationQueryExecutionException(AuthException.FACTOR_NOT_FOUND));
  }
}
