package com.example.messaging;

import com.example.api.messaging.MessagingCommand;
import com.example.api.messaging.Priority;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class MessagingCommandHandler {

  private final EventGateway eventGateway;

  @CommandHandler
  public String on(MessagingCommand.SendSmsCommand command) {
    if (command.getOptions().getPriority() == Priority.BULK) {
      eventGateway.publish(
          MessagingEvent.LowPrioritySmsSentEvent.builder()
              .id(command.getId())
              .message(command.getMessage())
              .phoneNumber(command.getPhoneNumber())
              .options(command.getOptions())
              .build());
    } else {
      eventGateway.publish(
          MessagingEvent.HighPrioritySmsSentEvent.builder()
              .id(command.getId())
              .message(command.getMessage())
              .phoneNumber(command.getPhoneNumber())
              .options(command.getOptions())
              .build());
    }
    return command.getId();
  }

  @CommandHandler
  public String on(MessagingCommand.SendEmailCommand command) {
    if (command.getOptions().getPriority() == Priority.BULK) {
      eventGateway.publish(
          MessagingEvent.LowPriorityEmailSentEvent.builder()
              .body(command.getBody())
              .id(command.getId())
              .to(command.getTo())
              .from(command.getFrom())
              .subject(command.getSubject())
              .options(command.getOptions())
              .build());
    } else {
      eventGateway.publish(
          MessagingEvent.HighPriorityEmailSentEvent.builder()
              .body(command.getBody())
              .id(command.getId())
              .to(command.getTo())
              .from(command.getFrom())
              .subject(command.getSubject())
              .options(command.getOptions())
              .build());
    }
    return command.getId();
  }
}
