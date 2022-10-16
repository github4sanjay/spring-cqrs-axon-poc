package com.example.auth.token.access.keys;

import com.auth0.jwt.algorithms.Algorithm;
import com.example.security.core.RSAUtil;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "signing_keys")
public class PublicSigningKey {
  @Id private String id;
  @Lob private String publicKey;
  private Instant expireAt;

  @Transient
  private RSAPublicKey publicKey() {
    return (RSAPublicKey) RSAUtil.getPublicKey(publicKey);
  }

  @Transient
  public Algorithm getDecodeAlgorithm() {
    return Algorithm.RSA256(publicKey(), null);
  }

  @Transient
  public RSAKey toPublicJWK() {
    return new RSAKey.Builder(publicKey())
        .keyID(id)
        .keyUse(KeyUse.SIGNATURE)
        .algorithm(JWSAlgorithm.RS256)
        .build();
  }
}
