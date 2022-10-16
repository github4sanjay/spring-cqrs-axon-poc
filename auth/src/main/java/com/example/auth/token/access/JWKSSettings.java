package com.example.auth.token.access;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "auth.jwks")
public class JWKSSettings {

  private String issuer;
  private Duration keyRotationPeriod;
  private Duration coolDownPeriod;
}
