package com.example.auth.account;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountConfig {
  public static final int SALT_LENGTH = 16;
  public static final int HASH_LENGTH = 32;
  public static final int ITERATIONS = 20;
  public static final int MEMORY = 65536;
  public static final int PARALLELISM = 1;
  public static final Argon2 ARGON2 = Argon2Factory.create(SALT_LENGTH, HASH_LENGTH);

  public String getHash(String password) {
    var passwordCharArray = password.toCharArray();
    try {
      return ARGON2.hash(ITERATIONS, MEMORY, PARALLELISM, passwordCharArray);
    } finally {
      ARGON2.wipeArray(passwordCharArray);
    }
  }

  public boolean verifyHash(String password, String hash) {
    var passwordCharArray = password.toCharArray();
    return ARGON2.verify(hash, passwordCharArray);
  }
}
