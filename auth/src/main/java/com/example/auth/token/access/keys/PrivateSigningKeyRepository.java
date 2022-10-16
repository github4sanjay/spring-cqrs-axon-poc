package com.example.auth.token.access.keys;

import com.example.auth.token.access.JWKSSettings;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PrivateSigningKeyRepository {

  private final Cache<String, PrivateSigningKey> cache;

  @Autowired
  public PrivateSigningKeyRepository(JWKSSettings settings) {
    this.cache = Caffeine.newBuilder().expireAfterWrite(settings.getKeyRotationPeriod()).build();
  }

  public PrivateSigningKey save(String keyId, String key) {
    var privateSigningKey = PrivateSigningKey.builder().id(keyId).key(key).build();
    cache.put(keyId, privateSigningKey);
    return privateSigningKey;
  }

  public Optional<PrivateSigningKey> findAny() {
    return cache.asMap().values().stream().findAny();
  }

  public void clearKeys() {
    cache.invalidateAll();
  }
}
