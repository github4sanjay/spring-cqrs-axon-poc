package com.example.messaging;

import com.example.api.messaging.PhoneNumber;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@NoArgsConstructor
public class SmsMessage implements Comparable<SmsMessage> {

  private String sentAt;
  private String spanId;
  private Sms sms;

  @Builder
  public SmsMessage(Sms sms, String spanId) {
    Objects.requireNonNull(sms);
    Objects.requireNonNull(spanId);
    this.sentAt = Instant.now().toString();
    this.spanId = spanId;
    this.sms = sms;
  }

  @Override
  public int compareTo(@NotNull SmsMessage o) {
    return o.sentAt.compareTo(this.sentAt);
  }

  @Data
  @NoArgsConstructor
  public static class Sms {
    private PhoneNumber phoneNumber;
    private String message;

    @Builder
    public Sms(PhoneNumber phoneNumber, String message) {
      Objects.requireNonNull(phoneNumber);
      Objects.requireNonNull(message);
      this.message = message;
      this.phoneNumber = phoneNumber;
    }
  }
}
