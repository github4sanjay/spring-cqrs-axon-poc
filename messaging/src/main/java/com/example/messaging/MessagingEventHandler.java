package com.example.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup("messaging")
public class MessagingEventHandler {

  private final SmsService smsService;
  private final EmailService emailService;
  private final MessageService messageService;
  private final OutboxService outboxService;

  @Value("${app.mode:production}")
  private String mode;

  @EventHandler
  public void on(MessagingEvent.HighPriorityEmailSentEvent event) {
    var email =
        messageService.getEmail(
            event.getId(), event.getBody(), event.getFrom(), event.getTo(), event.getSubject());
    if (mode.equals("outbox")) {
      outboxService.addEmail(email);
    } else {
      emailService.sendEmail(email);
    }
  }

  @EventHandler
  public void on(MessagingEvent.LowPriorityEmailSentEvent event) {
    var email =
        messageService.getEmail(
            event.getId(), event.getBody(), event.getFrom(), event.getTo(), event.getSubject());
    if (mode.equals("outbox")) {
      outboxService.addEmail(email);
    } else {
      emailService.sendEmail(email);
    }
  }

  @EventHandler
  public void on(MessagingEvent.HighPrioritySmsSentEvent event) {
    var sms = messageService.getSms(event.getId(), event.getPhoneNumber(), event.getMessage());
    if (mode.equals("outbox")) {
      outboxService.addSms(sms);
    } else {
      smsService.sendSms(sms);
    }
  }

  @EventHandler
  public void on(MessagingEvent.LowPrioritySmsSentEvent event) {
    var sms = messageService.getSms(event.getId(), event.getPhoneNumber(), event.getMessage());
    if (mode.equals("outbox")) {
      outboxService.addSms(sms);
    } else {
      smsService.sendSms(sms);
    }
  }
}
