package com.example.security.core;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.SneakyThrows;

public class AESUtil {

  public static final String ALGORITHM = "AES/CBC/PKCS5Padding";

  public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
    var keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(n);
    return keyGenerator.generateKey();
  }

  public static SecretKey getKeyFromPassword(String password, String salt)
      throws NoSuchAlgorithmException, InvalidKeySpecException {

    var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    var spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
    return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
  }

  public static IvParameterSpec generateIv() {
    var iv = new byte[16];
    new SecureRandom().nextBytes(iv);
    return generateIv(iv);
  }

  public static IvParameterSpec generateIv(byte[] iv) {
    return new IvParameterSpec(iv);
  }

  @SneakyThrows
  public static String encrypt(String input, String password, String salt, IvParameterSpec iv) {
    return encrypt(ALGORITHM, input, getKeyFromPassword(password, salt), iv);
  }

  public static String encrypt(String algorithm, String input, SecretKey key, IvParameterSpec iv)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
          InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

    var cipher = Cipher.getInstance(algorithm);
    cipher.init(Cipher.ENCRYPT_MODE, key, iv);
    var cipherText = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(cipherText);
  }

  @SneakyThrows
  public static String decrypt(
      String cipherText, String password, String salt, IvParameterSpec iv) {
    return decrypt(ALGORITHM, cipherText, getKeyFromPassword(password, salt), iv);
  }

  public static String decrypt(
      String algorithm, String cipherText, SecretKey key, IvParameterSpec iv)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
          InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

    var cipher = Cipher.getInstance(algorithm);
    cipher.init(Cipher.DECRYPT_MODE, key, iv);
    var plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
    return new String(plainText);
  }
}
