package com.example.messaging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(SpringExtension.class)
@WebFluxTest(
    controllers = OutboxController.class,
    properties = {"app.mode=outbox"})
@ImportAutoConfiguration(value = {RedissonAutoConfiguration.class})
@ContextConfiguration(
    classes = {
      MockRedis.class,
      OutboxService.class,
      AwsConfiguration.class,
      MessageService.class,
      SmsService.class,
      EmailService.class,
      MessagingEventHandler.class,
      OutboxController.class
    })
class OutboxControllerTest {

  @Autowired private WebTestClient webTestClient;

  @Test
  @DisplayName("test outbox expect ok")
  void testOutboxExpectOk() {

    webTestClient
        .get()
        .uri("/outbox")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk();
  }
}
