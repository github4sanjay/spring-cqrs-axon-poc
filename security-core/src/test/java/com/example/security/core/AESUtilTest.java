package com.example.security.core;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AESUtilTest {

  @Test
  @DisplayName("given string when encrypt then success")
  void givenStringWhenEncryptThenSuccess()
      throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException,
          BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException {

    String input = "some-text";
    SecretKey key = AESUtil.generateKey(128);
    IvParameterSpec ivParameterSpec = AESUtil.generateIv();
    String cipherText = AESUtil.encrypt(AESUtil.ALGORITHM, input, key, ivParameterSpec);
    String plainText = AESUtil.decrypt(AESUtil.ALGORITHM, cipherText, key, ivParameterSpec);
    Assertions.assertEquals(input, plainText);
  }

  @Test
  @DisplayName("given string, password and salt when encrypt then success")
  void givenStringPasswordAndSaltWhenEncryptThenSuccess()
      throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException,
          BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException,
          InvalidKeySpecException {

    var input = "some-text";
    var password = "my_super_secret_key";
    var salt = "ssshhhhhhhhhhh!!!!";
    var iv = AESUtil.generateIv();
    String cipherText = AESUtil.encrypt(input, password, salt, iv);
    String plainText = AESUtil.decrypt(cipherText, password, salt, iv);
    Assertions.assertEquals(input, plainText);
  }
}
