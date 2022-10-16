package com.example.auth.factors;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactorRepository extends JpaRepository<Factor, String> {
  List<Factor> findAllByAccountId(String accountId);

  Optional<Factor> findByAccountIdAndFactorType(String accountId, FactorType factorType);
}
