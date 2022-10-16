package com.example.api.messaging;

import java.util.List;
import java.util.Objects;
import javax.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.common.StringUtils;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class MessagingCommand {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SendSmsCommand {

    @TargetAggregateIdentifier private String id;
    private Options options;
    private PhoneNumber phoneNumber;
    private String message;

    @Builder
    public SendSmsCommand(
        String id, Priority priority, String profile, PhoneNumber phoneNumber, String message) {
      if (!StringUtils.nonEmptyOrNull(id)) {
        throw new ConstraintViolationException("id is required", null);
      }
      Objects.requireNonNull(phoneNumber, "phone number is required");
      if (!StringUtils.nonEmptyOrNull(message)) {
        throw new ConstraintViolationException("message is required", null);
      }
      if (profile != null && profile.isBlank()) {
        throw new ConstraintViolationException("profile cannot be blank", null);
      }
      if (priority == null) priority = Priority.BULK;
      if (profile == null) profile = "unspecified";
      this.id = id;
      this.options = Options.builder().priority(priority).profile(profile).build();
      this.phoneNumber = phoneNumber;
      this.message = message;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SendEmailCommand {
    @TargetAggregateIdentifier private String id;

    private Options options;
    private String from;
    private List<String> to;
    private String body;
    private String subject;

    @Builder
    public SendEmailCommand(
        String id,
        Priority priority,
        String profile,
        String from,
        List<String> to,
        String body,
        String subject) {
      if (!StringUtils.nonEmptyOrNull(id)) {
        throw new ConstraintViolationException("id is required", null);
      }
      if (profile != null && profile.isBlank()) {
        throw new ConstraintViolationException("profile cannot be blank", null);
      }
      if (!StringUtils.nonEmptyOrNull(from)) {
        throw new ConstraintViolationException("from is required", null);
      }
      Objects.requireNonNull(to);
      if (to.isEmpty()) {
        throw new ConstraintViolationException("should have at least size 1", null);
      } else if (to.stream().anyMatch(StringUtils::emptyOrNull)) {
        throw new ConstraintViolationException("to emails should not be empty or null", null);
      }
      if (!StringUtils.nonEmptyOrNull(body)) {
        throw new ConstraintViolationException("body is required", null);
      }
      if (!StringUtils.nonEmptyOrNull(subject)) {
        throw new ConstraintViolationException("subject is required", null);
      }

      if (priority == null) priority = Priority.BULK;
      if (profile == null) profile = "unspecified";
      this.id = id;
      this.options = Options.builder().priority(priority).profile(profile).build();
      this.body = body;
      this.from = from;
      this.to = to;
      this.subject = subject;
    }
  }
}
