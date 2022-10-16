package com.example.messaging;

import com.example.api.messaging.Options;
import com.example.api.messaging.PhoneNumber;
import com.example.api.messaging.Priority;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@WebFluxTest(properties = {"app.mode=outbox"})
@ImportAutoConfiguration(value = {RedissonAutoConfiguration.class})
@ContextConfiguration(
    classes = {
      MockRedis.class,
      OutboxService.class,
      AwsConfiguration.class,
      MessageService.class,
      SmsService.class,
      EmailService.class,
      MessagingEventHandler.class
    })
class MessagingEventHandlerModeOutboxTest {

  @Autowired private MessagingEventHandler messagingEventHandler;
  @Autowired private RedissonClient redissonClient;
  @Autowired private OutboxService outboxService;

  @BeforeEach
  public void beforeEach() {
    redissonClient.getKeys().flushall();
  }

  @Test
  @DisplayName("test LowPrioritySmsSentEvent expect sms is stored in redis")
  public void testLowPrioritySmsSentEventExpectSmsIsSent() {
    var phoneNumber = PhoneNumber.builder().value("+6587304661").build();
    var event =
        MessagingEvent.LowPrioritySmsSentEvent.builder()
            .id(UUID.randomUUID().toString())
            .message("Hello, there")
            .phoneNumber(phoneNumber)
            .options(Options.builder().profile("web-app").priority(Priority.BULK).build())
            .build();
    messagingEventHandler.on(event);
    var message = outboxService.getAllSms().get(event.getId());
    Assertions.assertEquals(phoneNumber.getValue(), message.getSms().getPhoneNumber().getValue());
    Assertions.assertEquals(event.getMessage(), message.getSms().getMessage());
  }

  @Test
  @DisplayName("test HighPrioritySmsSentEvent expect sms is stored in redis")
  public void testHighPrioritySmsSentEventExpectSmsIsSent() {
    var phoneNumber = PhoneNumber.builder().value("+6587304661").build();
    var event =
        MessagingEvent.HighPrioritySmsSentEvent.builder()
            .id(UUID.randomUUID().toString())
            .message("Hello, there")
            .phoneNumber(phoneNumber)
            .options(Options.builder().profile("web-app").priority(Priority.HIGH).build())
            .build();
    messagingEventHandler.on(event);

    var message = outboxService.getAllSms().get(event.getId());
    Assertions.assertEquals(phoneNumber.getValue(), message.getSms().getPhoneNumber().getValue());
    Assertions.assertEquals(event.getMessage(), message.getSms().getMessage());
  }

  @Test
  @DisplayName("test LowPriorityEmailSentEvent expect email is stored in redis")
  public void testLowPriorityEmailSentEventExpectEmailIsSent() {
    var event =
        MessagingEvent.LowPriorityEmailSentEvent.builder()
            .id(UUID.randomUUID().toString())
            .options(Options.builder().profile("web-app").priority(Priority.BULK).build())
            .body(
                """
                  <!DOCTYPE html>
                  <html>
                     <body>
                         <h1>My First Heading</h1>
                         <p>My first paragraph.</p>
                     </body>
                  </html>
                """)
            .from("no-reply@example.com")
            .id(UUID.randomUUID().toString())
            .subject("Test Subject")
            .to(List.of("github4sanjay@gmail.com"))
            .build();
    messagingEventHandler.on(event);

    var message = outboxService.getAllEmails().get(event.getId());
    Assertions.assertEquals(event.getFrom(), message.getEmail().getFrom());
  }

  @Test
  @DisplayName("test HighPriorityEmailSentEvent expect email is stored in redis")
  public void testHighPriorityEmailSentEventExpectEmailIsSent() {
    var event =
        MessagingEvent.HighPriorityEmailSentEvent.builder()
            .id(UUID.randomUUID().toString())
            .options(Options.builder().profile("web-app").priority(Priority.HIGH).build())
            .body(
                """
                  <!DOCTYPE html>
                  <html>
                     <body>
                         <h1>My First Heading</h1>
                         <p>My first paragraph.</p>
                     </body>
                  </html>
                """)
            .from("no-reply@example.com")
            .id(UUID.randomUUID().toString())
            .subject("Test Subject")
            .to(List.of("github4sanjay@gmail.com"))
            .build();
    messagingEventHandler.on(event);

    var message = outboxService.getAllEmails().get(event.getId());
    Assertions.assertEquals(event.getFrom(), message.getEmail().getFrom());
  }
}
