package com.example.auth.token.access.keys;

import com.example.auth.AuthException;
import com.example.auth.token.access.JWKSSettings;
import com.example.security.core.RSAUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class SigningKeyService {

  private final PublicSigningKeyRepository publicSigningKeyRepository;
  private final PrivateSigningKeyRepository privateSigningKeyRepository;
  private final JWKSSettings settings;
  private final Cache<String, PublicSigningKey> cache;

  public SigningKeyService(
      PublicSigningKeyRepository publicSigningKeyRepository,
      PrivateSigningKeyRepository privateSigningKeyRepository,
      JWKSSettings settings) {
    this.publicSigningKeyRepository = publicSigningKeyRepository;
    this.privateSigningKeyRepository = privateSigningKeyRepository;
    this.settings = settings;
    this.cache = Caffeine.newBuilder().expireAfterWrite(settings.getCoolDownPeriod()).build();
  }

  public PrivateSigningKey getCurrentSigningKey() {
    return privateSigningKeyRepository.findAny().orElseGet(this::createNewSigningKey);
  }

  public PublicSigningKey getByKeyId(String id) {
    var signingKey = cache.getIfPresent(id);
    if (signingKey == null) {
      signingKey =
          publicSigningKeyRepository.findById(id).orElseThrow(AuthException.INVALID_TOKEN::getEx);
      cache.put(id, signingKey);
    }
    return signingKey;
  }

  public Stream<PublicSigningKey> getKeys() {
    return publicSigningKeyRepository.findAllByExpireAtAfter(Instant.now()).stream();
  }

  public PrivateSigningKey createNewSigningKey() {
    var keyPair = RSAUtil.generateKeyPair();
    var privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    var publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    var publicSigningKey =
        publicSigningKeyRepository.save(
            PublicSigningKey.builder()
                .id(UUID.randomUUID().toString())
                .expireAt(
                    Instant.now()
                        .plus(settings.getKeyRotationPeriod())
                        .plus(settings.getCoolDownPeriod()))
                .publicKey(publicKey)
                .build());
    return privateSigningKeyRepository.save(publicSigningKey.getId(), privateKey);
  }
}
