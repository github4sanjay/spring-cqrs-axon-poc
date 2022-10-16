package com.example.auth.common;

import com.example.auth.AuthException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Configuration
@ConfigurationProperties(prefix = "auth")
public class ClientConfiguration {

  private Map<String, Client> clientConfig;

  @Data
  public static class Client {
    private Boolean requestSigningEnabled = false;
    private AccountConfig account;
    private JwtConfig jwt;
    private DeviceTrustConfig deviceTrust;
    private LoginOTPConfig loginOtp;
    private AlertConfig alerts;
    private Flags flags;
    private List<String> capsules;
    private List<String> allowedOrigins;
    private String messageProfile;
    private String zoneId;
    private Factors factors;

    public boolean isValidCapsule(String aud) {
      if (capsules == null) return false;
      return capsules.stream().anyMatch(capsule -> capsule.equals(aud));
    }
  }

  public List<String> getCorsAllowedOrigin() {
    var allowedOrigins = new ArrayList<String>();
    clientConfig
        .values()
        .forEach(
            client -> {
              if (client.getAllowedOrigins() != null) {
                allowedOrigins.addAll(client.getAllowedOrigins());
              }
            });
    return allowedOrigins;
  }

  @Data
  public static class AccountConfig {
    private Boolean enabled = false;
    private Boolean mfa = false;
    private Duration mfaExpiry = Duration.ofMinutes(5);
    private String emailActivationLink;
    private LoginOTPConfig mfaOTP;
  }

  @Data
  public static class Factors {
    private Duration expiry = Duration.ofMinutes(3);
    private Duration resendAfter = Duration.ofMinutes(1);
    private Integer maxAllowedOTP = 5;
    private Duration durationForMaxAllowedOTP = Duration.ofMinutes(30);
    private Integer maxAttemptForVerification = 5;
    private String reference = "login/";
    private SMS sms;
    private Email email;
    private Totp totp;
  }

  @Data
  public static class JwtConfig {
    private Duration accessTokenExpiry = Duration.ofMinutes(10);
    private RefreshTokenExpiry refreshTokenExpiry;
    private Duration refreshChainExpiry = Duration.ofDays(10);
  }

  @Data
  public static class RefreshTokenExpiry {
    private Duration mfa = Duration.ofMinutes(30);
    private Duration pwd = Duration.ofMinutes(30);
    private Duration bio = Duration.ofMinutes(30);
    private Duration net = Duration.ofDays(3);
    private Duration otp = Duration.ofMinutes(10);
  }

  @Data
  public static class DeviceTrustConfig {
    private Integer maxDeviceCount = 10;
  }

  @Data
  public static class LoginOTPConfig {
    private Duration expiry = Duration.ofMinutes(3);
    private Duration resendAfter = Duration.ofMinutes(1);
    private Integer maxAllowedOTP = 5;
    private Duration durationForMaxAllowedOTP = Duration.ofMinutes(30);
    private Integer maxAttemptForVerification = 5;
    private String reference = "login/";
    private SMS sms;
    private Email email;
  }

  @Data
  public static class SMS {
    private boolean otpChallengeEnabled = false;
    private String template;
  }

  @Data
  public static class Email {
    private boolean otpChallengeEnabled = false;
    private String from = "";
    private String template;
    private String subject;
  }

  @Data
  public static class Totp {
    private String password;
    private String salt;
  }

  @Data
  public static class AlertConfig {
    private Alert newDeviceDetected;
    private Alert trustedDeviceAdded;
    private Alert mobileNumberAdded;
  }

  @Data
  public static class Alert {
    private String sms;
    private Email email;
  }

  @Data
  public static class User {
    private String accountId;
    private List<Tag> tags;

    public List<String> getAllValidTags() {
      var validTags = new ArrayList<String>();
      for (Tag tag : this.tags) {
        if (tag.isValid()) {
          validTags.add(tag.getName());
        }
      }
      return validTags;
    }

    public boolean hasAppleReviewTag() {
      for (Tag tag : this.tags) {
        if (tag.isValidAppleReviewTag()) {
          return true;
        }
      }
      return false;
    }
  }

  @Data
  public static class Tag {
    private String name;
    private Instant exp;
    private Instant nbf;

    public boolean isValid() {
      var current = Instant.now();
      return current.isAfter(this.nbf) && current.isBefore(this.exp);
    }

    public boolean isValidAppleReviewTag() {
      if (name.equals("apple-review")) {
        var current = Instant.now();
        return current.isAfter(this.nbf) && current.isBefore(this.exp);
      }
      return false;
    }
  }

  @Data
  public static class Flags {

    private List<User> users;

    public User findUser(String accountId) {
      for (User specialUser : users) {
        if (accountId.equalsIgnoreCase(specialUser.getAccountId())) {
          return specialUser;
        }
      }
      return null;
    }
  }

  public Client getCurrentClient(String client) {
    var config = this.clientConfig.get(client);
    if (config == null) {
      throw AuthException.INVALID_CLIENT.getEx();
    }
    return config;
  }

  public boolean isValidClient(String client) {
    if (this.clientConfig == null) return false;
    return this.clientConfig.keySet().stream().anyMatch(audience -> audience.equals(client));
  }
}
