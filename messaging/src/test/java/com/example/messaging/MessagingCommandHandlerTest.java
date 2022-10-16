package com.example.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.api.messaging.MessagingCommand;
import com.example.api.messaging.PhoneNumber;
import com.example.api.messaging.Priority;
import java.util.List;
import java.util.UUID;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@WebFluxTest
@ImportAutoConfiguration(value = {RedissonAutoConfiguration.class})
@ContextConfiguration(
    classes = {MockRedis.class, OutboxService.class, MessagingCommandHandler.class})
public class MessagingCommandHandlerTest {

  @MockBean private EventGateway eventGateway;
  @Autowired private MessagingCommandHandler messagingCommandHandler;

  @Test
  @DisplayName("test SendEmailCommand when high priority expect HighPriorityEmailSentEvent")
  public void testSendEmailCommandWhenHighPriorityExpectHighPriorityEmailSentEvent() {
    doNothing()
        .when(eventGateway)
        .publish(ArgumentMatchers.<MessagingEvent.HighPriorityEmailSentEvent>any());
    var command =
        MessagingCommand.SendEmailCommand.builder()
            .profile("web-app")
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
            .priority(Priority.HIGH)
            .build();
    messagingCommandHandler.on(command);
    var argumentCapture = ArgumentCaptor.forClass(MessagingEvent.HighPriorityEmailSentEvent.class);
    verify(eventGateway, times(1)).publish(argumentCapture.capture());
    var event = argumentCapture.getValue();
    assertEquals(command.getOptions().getPriority(), event.getOptions().getPriority());
    assertEquals(command.getOptions().getProfile(), event.getOptions().getProfile());
    assertEquals(command.getId(), event.getId());
    assertEquals(command.getSubject(), event.getSubject());
    assertEquals(command.getTo(), event.getTo());
    assertEquals(command.getFrom(), event.getFrom());
    assertEquals(command.getBody(), event.getBody());
  }

  @Test
  @DisplayName("test SendEmailCommand when bulk priority expect LowPriorityEmailSentEvent")
  public void testSendEmailCommandWhenBulkPriorityExpectLowPriorityEmailSentEvent() {
    doNothing()
        .when(eventGateway)
        .publish(ArgumentMatchers.<MessagingEvent.LowPriorityEmailSentEvent>any());
    var command =
        MessagingCommand.SendEmailCommand.builder()
            .profile("web-app")
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
            .priority(Priority.BULK)
            .build();
    messagingCommandHandler.on(command);
    var argumentCapture = ArgumentCaptor.forClass(MessagingEvent.LowPriorityEmailSentEvent.class);
    verify(eventGateway, times(1)).publish(argumentCapture.capture());
    var event = argumentCapture.getValue();
    assertEquals(command.getOptions().getPriority(), event.getOptions().getPriority());
    assertEquals(command.getOptions().getProfile(), event.getOptions().getProfile());
    assertEquals(command.getId(), event.getId());
    assertEquals(command.getSubject(), event.getSubject());
    assertEquals(command.getTo(), event.getTo());
    assertEquals(command.getFrom(), event.getFrom());
    assertEquals(command.getBody(), event.getBody());
  }

  @Test
  @DisplayName("test SendEmailCommand when high priority expect HighPrioritySmsSentEvent")
  public void testSendEmailCommandWhenHighPriorityExpectHighPrioritySmsSentEvent() {
    doNothing()
        .when(eventGateway)
        .publish(ArgumentMatchers.<MessagingEvent.HighPrioritySmsSentEvent>any());
    var command =
        MessagingCommand.SendSmsCommand.builder()
            .id(UUID.randomUUID().toString())
            .profile("web-app")
            .message("Hello, there")
            .phoneNumber(PhoneNumber.builder().value("+6587304661").build())
            .priority(Priority.HIGH)
            .build();
    messagingCommandHandler.on(command);
    var argumentCapture = ArgumentCaptor.forClass(MessagingEvent.HighPrioritySmsSentEvent.class);
    verify(eventGateway, times(1)).publish(argumentCapture.capture());
    var event = argumentCapture.getValue();
    assertEquals(command.getOptions().getPriority(), event.getOptions().getPriority());
    assertEquals(command.getOptions().getProfile(), event.getOptions().getProfile());
    assertEquals(command.getId(), event.getId());
    assertEquals(command.getMessage(), event.getMessage());
    assertEquals(command.getPhoneNumber(), event.getPhoneNumber());
  }

  @Test
  @DisplayName("test SendSmsCommand when bulk priority expect LowPrioritySmsSentEvent")
  public void testSendSmsCommandWhenBulkPriorityExpectLowPrioritySmsSentEvent() {
    doNothing()
        .when(eventGateway)
        .publish(ArgumentMatchers.<MessagingEvent.LowPrioritySmsSentEvent>any());
    var command =
        MessagingCommand.SendSmsCommand.builder()
            .id(UUID.randomUUID().toString())
            .profile("web-app")
            .message("Hello, there")
            .phoneNumber(PhoneNumber.builder().value("+6587304661").build())
            .priority(Priority.BULK)
            .build();
    messagingCommandHandler.on(command);
    var argumentCapture = ArgumentCaptor.forClass(MessagingEvent.LowPrioritySmsSentEvent.class);
    verify(eventGateway, times(1)).publish(argumentCapture.capture());
    var event = argumentCapture.getValue();
    assertEquals(command.getOptions().getPriority(), event.getOptions().getPriority());
    assertEquals(command.getOptions().getProfile(), event.getOptions().getProfile());
    assertEquals(command.getId(), event.getId());
    assertEquals(command.getMessage(), event.getMessage());
    assertEquals(command.getPhoneNumber(), event.getPhoneNumber());
  }
}
