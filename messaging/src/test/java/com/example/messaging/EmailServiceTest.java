package com.example.messaging;

import static org.mockito.Mockito.times;

import java.nio.charset.StandardCharsets;
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

@ExtendWith(SpringExtension.class)
@WebFluxTest(properties = {"app.mode=outbox"})
@ImportAutoConfiguration(value = {RedissonAutoConfiguration.class})
@ContextConfiguration(
    classes = {
      MockRedis.class,
      AwsConfiguration.class,
      EmailService.class,
    })
class EmailServiceTest {

  @Autowired private EmailService emailService;
  @MockBean private SesClient sesClient;

  @Test
  @DisplayName("test send email with duplicate email should be sent only once")
  public void testSendEmailWithDuplicateEmailShouldBeSentOnlyOnce() {
    var email =
        EmailMessage.builder()
            .email(
                EmailMessage.Email.builder()
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
                    .subject("Test Subject")
                    .to(List.of("github4sanjay@gmail.com"))
                    .images(
                        List.of(
                            EmailMessage.EmbeddedImage.builder()
                                .cid("myimagecid")
                                .contentType("image/jpeg")
                                .data(
                                    "yourbase64encodedimageasastringcangohere"
                                        .getBytes(StandardCharsets.UTF_8))
                                .build()))
                    .build())
            .spanId(UUID.randomUUID().toString())
            .build();
    emailService.sendEmail(email);
    emailService.sendEmail(email);

    var requestArgumentCaptor = ArgumentCaptor.forClass(SendRawEmailRequest.class);
    Mockito.verify(sesClient, times(1)).sendRawEmail(requestArgumentCaptor.capture());
    var sendRawEmailRequest = requestArgumentCaptor.getValue();
    Assertions.assertEquals(email.getEmail().getFrom(), sendRawEmailRequest.source());
  }
}
