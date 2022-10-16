package com.example.security.core;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public interface Signature {

  default String sign(String privateKey)
      throws NoSuchAlgorithmException, InvalidKeyException, SignatureException,
          InvalidKeySpecException {
    var privateSignature = java.security.Signature.getInstance("SHA256withRSA");
    privateSignature.initSign(RSAUtil.getPrivateKey(privateKey));
    privateSignature.update(this.create().getBytes(StandardCharsets.UTF_8));
    var signature = privateSignature.sign();
    return Base64.getEncoder().encodeToString(signature);
  }

  default boolean verify(String signature, String publicKey)
      throws NoSuchAlgorithmException, InvalidKeyException, SignatureException,
          InvalidKeySpecException {
    var publicSignature = java.security.Signature.getInstance("SHA256withRSA");
    publicSignature.initVerify(RSAUtil.getPublicKey(publicKey));
    publicSignature.update(this.create().getBytes(StandardCharsets.UTF_8));
    var signatureBytes = Base64.getDecoder().decode(signature);
    return publicSignature.verify(signatureBytes);
  }

  String create();
}
