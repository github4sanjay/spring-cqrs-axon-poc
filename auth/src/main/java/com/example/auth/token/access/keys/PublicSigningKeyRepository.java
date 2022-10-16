package com.example.auth.token.access.keys;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicSigningKeyRepository extends JpaRepository<PublicSigningKey, String> {

  List<PublicSigningKey> findAllByExpireAtAfter(Instant instant);
}
