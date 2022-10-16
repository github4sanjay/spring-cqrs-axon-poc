package com.example.auth.token.access.keys;

import com.auth0.jwt.algorithms.Algorithm;
import com.example.security.core.RSAUtil;
import java.security.interfaces.RSAPrivateKey;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrivateSigningKey {
  private final String key;
  private final String id;

  public Algorithm getSignAlgorithm() {
    return Algorithm.RSA256(null, privateKey(key));
  }

  private RSAPrivateKey privateKey(String privateKey) {
    return (RSAPrivateKey) RSAUtil.getPrivateKey(privateKey);
  }
}
