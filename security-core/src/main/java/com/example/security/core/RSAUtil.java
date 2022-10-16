package com.example.security.core;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAUtil {

  private RSAUtil() {}

  public static KeyPair generateKeyPair() {
    try {
      var keyGen = KeyPairGenerator.getInstance("RSA");
      keyGen.initialize(2048);
      return keyGen.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static PublicKey getPublicKey(String base64PublicKey) {
    var keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey.getBytes()));
    try {
      var keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(keySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  public static PrivateKey getPrivateKey(String base64PrivateKey) {
    try {
      var keyFactory = KeyFactory.getInstance("RSA");
      var keySpec =
          new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64PrivateKey.getBytes()));
      return keyFactory.generatePrivate(keySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }
}
