package com.example.auth.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
  Optional<Account> findAccountByEmail(String email);
}
