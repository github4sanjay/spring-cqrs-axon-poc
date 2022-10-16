package com.example.auth.device;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import(value = {DeviceRegistrationConfig.class})
class DeviceRegistrationConfigTest {

  @Autowired private DeviceRegistrationConfig config;

  @Test
  @DisplayName("verify hash should return true if correct password")
  void verifyHashShouldReturnTrueIfCorrectPassword() {
    var password = "password";
    var hash = config.getHash(password);
    Assertions.assertTrue(config.verifyHash(password, hash));
  }

  @Test
  @DisplayName("verify hash should return false if incorrect password")
  void verifyHashShouldReturnFalseIfINCorrectPassword() {
    var password = "right-password";
    var hash = config.getHash(password);
    Assertions.assertFalse(config.verifyHash("wrong-password", hash));
  }
}
