package com.example.auth.token;

import com.example.auth.AuthException;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.Device;
import com.example.spring.core.exceptions.CoreExceptions;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Claims {

  public static final String ACCOUNT = "account";
  public static final String PHONE_NUMBER = "phone-number";
  public static final String EMAIL = "email";
  public static final String DEVICE = "device";

  private final Subject subject;
  private final AMR amr;
  private final Map<String, String> customClaims;
  private final String aud;

  public Claims(Subject subject, Device device, AMR amr) {
    this.subject = subject;
    this.aud = device.getClient();
    this.customClaims = new HashMap<>(subject.getCustomClaims());
    customClaims.put(DEVICE, device.getId());
    this.amr = amr;
  }

  public Claims delegate(String audience) {
    return new Claims(subject, amr, customClaims, audience);
  }

  public enum AMR {
    mfa,
    pwd,
    bio,
    net,
    otp
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AccountSubject implements Claims.Subject {
    private String id;
    private String email;

    @Override
    public String get() {
      return "account|" + this.id + "|" + this.email;
    }

    @Override
    public Map<String, String> getCustomClaims() {
      return Map.of(ACCOUNT, id.toString(), EMAIL, email);
    }

    public static AccountSubject parse(String sub) {
      String[] subs = sub.split("\\|");
      return AccountSubject.builder().id(subs[1]).email(subs[2]).build();
    }
  }

  public AccountSubject getAccountSubject() {
    if (this.subject instanceof AccountSubject) {
      return (AccountSubject) this.getSubject();
    } else {
      throw AuthException.ACCOUNT_LOGIN_REQUIRED.getEx();
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PhoneNumberSubject implements Subject {
    private String phoneNumber;

    @Override
    public String get() {
      return "phone-number|" + this.phoneNumber;
    }

    @Override
    public Map<String, String> getCustomClaims() {
      return Map.of(PHONE_NUMBER, phoneNumber);
    }

    public static PhoneNumberSubject parse(String sub) {
      String[] subs = sub.split("\\|");
      return PhoneNumberSubject.builder().phoneNumber(subs[1]).build();
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EMAILSubject implements Subject {
    private String email;

    @Override
    public String get() {
      return "email|" + this.email;
    }

    @Override
    public Map<String, String> getCustomClaims() {
      return Map.of(EMAIL, email);
    }

    public static EMAILSubject parse(String sub) {
      return EMAILSubject.builder().email(getEmailFromSubject(sub)).build();
    }
  }

  public interface Subject {
    String get();

    Map<String, String> getCustomClaims();

    static Subject parse(String value) {
      if (value.startsWith("account|")) {
        return AccountSubject.parse(value);
      } else if (value.startsWith("phone-number|")) {
        return PhoneNumberSubject.parse(value);
      } else if (value.startsWith("email|")) {
        return EMAILSubject.parse(value);
      } else {
        throw AuthException.INVALID_TOKEN.getEx();
      }
    }
  }

  public static Claims getClaims(String sub, AMR amr) {
    if (sub.contains(ACCOUNT)) {
      var accountSubject = AccountSubject.parse(sub);
      return Claims.builder()
          .customClaims(accountSubject.getCustomClaims())
          .subject(accountSubject)
          .amr(amr)
          .build();
    }
    if (sub.contains(PHONE_NUMBER)) {
      var phoneNumberSubject = PhoneNumberSubject.parse(sub);
      return Claims.builder()
          .customClaims(phoneNumberSubject.getCustomClaims())
          .subject(phoneNumberSubject)
          .amr(amr)
          .build();
    } else if (sub.contains(EMAIL)) {
      var emailSubject = EMAILSubject.parse(sub);
      return Claims.builder()
          .customClaims(emailSubject.getCustomClaims())
          .subject(emailSubject)
          .amr(amr)
          .build();
    } else {
      throw CoreExceptions.INTERNAL_SERVER_ERROR.getEx();
    }
  }

  public static String getEmailFromSubject(String sub) {
    if (sub.contains(EMAIL)) {
      String[] subs = sub.split("\\|");
      return subs[1];
    }
    return null;
  }

  public static List<String> getFlags(ClientConfiguration.Client client, String accountId) {
    var flags = new ArrayList<String>();
    var flagsConfig = client.getFlags();
    if (flagsConfig != null && accountId != null) {
      var user = flagsConfig.findUser(accountId);
      if (user != null) {
        return user.getAllValidTags();
      }
    }
    return flags;
  }
}
