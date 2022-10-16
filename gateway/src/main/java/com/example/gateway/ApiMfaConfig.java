package com.example.gateway;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

@Setter
@Configuration
@ConfigurationProperties(prefix = "app.mfa")
public class ApiMfaConfig {
  public static final String X_MFA_EMAIL = "x-mfa-email";
  public static final String X_MFA_SMS = "x-mfa-sms";
  public static final String X_MFA_AUTHENTICATOR = "x-mfa-authenticator";

  private List<APIConfig> apis;

  public boolean isMfaAPI(String methodType, String uri) {
    var matcher = new AntPathMatcher();
    for (var api : apis) {
      if (matcher.match(api.getUri(), uri) && api.getMethodTypes().contains(methodType)) {
        return true;
      }
    }
    return false;
  }

  @Getter
  @Setter
  public static class APIConfig {
    private Set<String> methodTypes;
    private String uri;
  }
}
