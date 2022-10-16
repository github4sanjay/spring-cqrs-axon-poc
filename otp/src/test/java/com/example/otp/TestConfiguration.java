package com.example.otp;

import java.time.Clock;
import org.springframework.context.annotation.Bean;

@org.springframework.boot.test.context.TestConfiguration
public class TestConfiguration {
  @Bean
  public Clock testClock() {
    return new TestClock();
  }
}
