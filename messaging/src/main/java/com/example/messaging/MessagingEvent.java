package com.example.messaging;

import com.example.api.messaging.Options;
import com.example.api.messaging.PhoneNumber;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.serialization.Revision;

public class MessagingEvent {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class HighPrioritySmsSentEvent {
    private String id;
    private Options options;
    private PhoneNumber phoneNumber;
    private String message;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class LowPrioritySmsSentEvent {
    private String id;
    private Options options;
    private PhoneNumber phoneNumber;
    private String message;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class HighPriorityEmailSentEvent {
    private String id;
    private Options options;
    private String from;
    private List<String> to;
    private String body;
    private String subject;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Revision("1.0")
  public static class LowPriorityEmailSentEvent {
    private String id;
    private Options options;
    private String from;
    private List<String> to;
    private String body;
    private String subject;
  }
}
