package com.example.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;

import com.example.api.messaging.Options;
import com.example.api.messaging.PhoneNumber;
import com.example.api.messaging.Priority;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@ExtendWith(SpringExtension.class)
@WebFluxTest(properties = {"app.mode=production"})
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
class MessagingEventHandlerTest {

  @Autowired private MessagingEventHandler messagingEventHandler;
  @MockBean private SnsClient snsClient;
  @MockBean private SesClient sesClient;

  @Test
  @DisplayName("test LowPrioritySmsSentEvent expect sms is sent")
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

    var requestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    Mockito.verify(snsClient, times(1)).publish(requestArgumentCaptor.capture());
    var sendRawSmsRequest = requestArgumentCaptor.getValue();
    Assertions.assertEquals(phoneNumber.withoutPlus(), sendRawSmsRequest.phoneNumber());
    Assertions.assertEquals(event.getMessage(), sendRawSmsRequest.message());
  }

  @Test
  @DisplayName("test HighPrioritySmsSentEvent expect sms is sent")
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

    var requestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    Mockito.verify(snsClient, times(1)).publish(requestArgumentCaptor.capture());
    var sendRawSmsRequest = requestArgumentCaptor.getValue();
    Assertions.assertEquals(phoneNumber.withoutPlus(), sendRawSmsRequest.phoneNumber());
    Assertions.assertEquals(event.getMessage(), sendRawSmsRequest.message());
  }

  @Test
  @DisplayName("test LowPriorityEmailSentEvent expect email is sent")
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

    var requestArgumentCaptor = ArgumentCaptor.forClass(SendRawEmailRequest.class);
    Mockito.verify(sesClient, times(1)).sendRawEmail(requestArgumentCaptor.capture());
    var sendRawEmailRequest = requestArgumentCaptor.getValue();
    Assertions.assertEquals(event.getFrom(), sendRawEmailRequest.source());
  }

  @Test
  @DisplayName("test HighPriorityEmailSentEvent expect email is sent")
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

    var requestArgumentCaptor = ArgumentCaptor.forClass(SendRawEmailRequest.class);
    Mockito.verify(sesClient, times(1)).sendRawEmail(requestArgumentCaptor.capture());
    var sendRawEmailRequest = requestArgumentCaptor.getValue();
    Assertions.assertEquals(event.getFrom(), sendRawEmailRequest.source());
  }
}
