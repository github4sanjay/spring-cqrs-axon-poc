package com.example.api.otp;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import javax.validation.ConstraintViolationException;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.common.StringUtils;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class OtpCommand {

  /** Represent an axon command which sends sms otp and returns outcome {@link SendOtpOutcome} */
  @Data
  @NoArgsConstructor
  public static class SendSmsOtpCommand {

    @TargetAggregateIdentifier private String id;
    private OtpOptions options;
    private PhoneNumber phoneNumber;
    private String message;

    /**
     * @param reference Key used for rate limiting. You should probably include some user
     *     identification in this value. e.g., login/example@singtel.com
     * @param options Different options can be chosen while sending an otp
     * @param phoneNumber Phone number to send the SMS message to.
     * @param message Content of the SMS message. Every instance of `{code}` will be replaced with
     *     the OTP. e.g., Use {code} to login to example.com and manage your account
     */
    @Builder
    public SendSmsOtpCommand(
        String reference, OtpOptions options, PhoneNumber phoneNumber, String message) {
      Objects.requireNonNull(reference, "reference is required");
      Objects.requireNonNull(phoneNumber, "phone number is required");
      Objects.requireNonNull(message, "message is required");
      if (!StringUtils.nonEmptyOrNull(reference)) {
        throw new ConstraintViolationException("reference should be non null and non empty", null);
      }
      if (!StringUtils.nonEmptyOrNull(message) && !message.contains("{code}")) {
        throw new ConstraintViolationException(
            "message should be non null and non empty and should contain '{code}'", null);
      }
      if (options == null) options = OtpOptions.builder().build();
      this.id = getSmsOtpId(reference);
      this.options = options;
      this.phoneNumber = phoneNumber;
      this.message = message;
    }
  }

  /** Represent an axon command which sends sms otp and returns outcome {@link SendOtpOutcome} */
  @Data
  @NoArgsConstructor
  public static class SendEmailOtpCommand {
    @TargetAggregateIdentifier private String id;

    private OtpOptions options;
    private String fromEmail;
    private String toEmail;
    private String body;
    private String subject;

    /**
     * @param reference Key used for rate limiting. You should probably include some user
     *     identification in this value. e.g., login/example@singtel.com
     * @param options Different options can be chosen while sending an otp
     * @param fromEmail Sender e-mail address. If not specified, the profile's default e-mail
     *     address will be used. e.g., no-reply@example.com
     * @param toEmail Recipient e-mail, e.g. john.doe@example.com
     * @param body Content of the e-mail message. Every instance of `{code}` will be replaced with
     *     the OTP. e.g., <html></><body>Use {code} to login to GOMO and manage your
     *     account</body></html>
     * @param subject Subject of e-mail. Every instance of `{code}` will be replaced with the OTP.
     *     e.g., OTP is {code} for your account
     */
    @Builder
    public SendEmailOtpCommand(
        String reference,
        OtpOptions options,
        String fromEmail,
        String toEmail,
        String body,
        String subject) {
      Objects.requireNonNull(reference);
      Objects.requireNonNull(body);
      Objects.requireNonNull(subject);
      if (!StringUtils.nonEmptyOrNull(reference)) {
        throw new ConstraintViolationException("reference should be non null and non empty", null);
      }
      if (!StringUtils.nonEmptyOrNull(fromEmail)) {
        throw new ConstraintViolationException("fromEmail should be a valid email", null);
      }
      if (!StringUtils.nonEmptyOrNull(toEmail)) {
        throw new ConstraintViolationException("toEmail should be a valid email", null);
      }
      if (!StringUtils.nonEmptyOrNull(subject)) {
        throw new ConstraintViolationException("subject should be non null and non empty", null);
      }
      if (!StringUtils.nonEmptyOrNull(body) && !body.contains("{code}")) {
        throw new ConstraintViolationException(
            "body should be non null and non empty and should contain '{code}'", null);
      }
      this.id = OtpCommand.getEmailOtpId(reference);
      this.options = options;
      this.body = body;
      this.fromEmail = fromEmail;
      this.toEmail = toEmail;
      this.subject = subject;
    }
  }

  /** Represent an axon command which verify otp and returns outcome {@link VerifyOtpOutcome} */
  @Data
  @NoArgsConstructor
  public static class VerifyOtpCommand {

    private String otp;
    private String token;

    /**
     * Constructor for class VerifyOtpCommand
     *
     * @param otp Code received after th otp request. e.g. 232424
     * @param token Token of the otp. e.g., 087ac49e-0c0a-45da-a701-8624a40f2bba
     */
    @Builder
    public VerifyOtpCommand(String otp, String token) {
      Objects.requireNonNull(otp);
      Objects.requireNonNull(token);
      this.otp = otp;
      this.token = token;
    }
  }

  private static String getEmailOtpId(String reference) {
    return UUID.nameUUIDFromBytes(
            ("EmailOtpAggregate." + reference).getBytes(StandardCharsets.UTF_8))
        .toString();
  }

  private static String getSmsOtpId(String reference) {
    return UUID.nameUUIDFromBytes(("SmsOtpAggregate." + reference).getBytes(StandardCharsets.UTF_8))
        .toString();
  }
}
