package com.example.messaging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.ConstraintViolationException;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@NoArgsConstructor
public class EmailMessage implements Comparable<EmailMessage> {

  private String sentAt;
  private String spanId;
  private Email email;

  @Builder
  public EmailMessage(Email email, String spanId) {
    Objects.requireNonNull(email);
    Objects.requireNonNull(spanId);
    this.sentAt = Instant.now().toString();
    this.spanId = spanId;
    this.email = email;
  }

  @Override
  public int compareTo(@NotNull EmailMessage o) {
    return o.sentAt.compareTo(this.sentAt);
  }

  @Data
  @NoArgsConstructor
  public static class Email {
    private String from;
    private List<String> to;
    private String body;
    private String subject;
    private List<EmbeddedImage> images;

    @Builder
    public Email(
        String from, List<String> to, String body, String subject, List<EmbeddedImage> images) {
      Objects.requireNonNull(from);
      Objects.requireNonNull(to);
      if (to.isEmpty()) {
        throw new ConstraintViolationException("should have at least size 1", null);
      }
      Objects.requireNonNull(body);
      Objects.requireNonNull(subject);
      this.body = body;
      this.from = from;
      this.to = to;
      this.subject = subject;
      if (images == null) images = new ArrayList<>();
      this.images = images;
    }
  }

  @Data
  @NoArgsConstructor
  public static class EmbeddedImage {
    private String cid;
    private String contentType;
    private byte[] data;

    @Builder
    public EmbeddedImage(String cid, String contentType, byte[] data) {
      Objects.requireNonNull(cid);
      Objects.requireNonNull(contentType);
      this.cid = cid;
      this.contentType = contentType;
      this.data = data;
    }
  }
}
