package com.example.auth.factors.totp;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TotpRepository extends JpaRepository<Totp, String> {
  Optional<Totp> findByAccountId(String acountId);
}
