package com.example.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;

import com.example.api.messaging.PhoneNumber;
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
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@ExtendWith(SpringExtension.class)
@WebFluxTest(properties = {"app.mode=outbox"})
@ImportAutoConfiguration(value = {RedissonAutoConfiguration.class})
@ContextConfiguration(
    classes = {
      MockRedis.class,
      AwsConfiguration.class,
      SmsService.class,
    })
class SmsServiceTest {

  @Autowired private SmsService smsService;
  @MockBean private SnsClient snsClient;

  @Test
  @DisplayName("test send sms with duplicate sms should be sent only once")
  public void testSendSmsWithDubplicateSmsShouldBeSentOnlyOnce() {
    var sms =
        SmsMessage.builder()
            .sms(
                SmsMessage.Sms.builder()
                    .message("some message")
                    .phoneNumber(PhoneNumber.builder().value("+6587304661").build())
                    .build())
            .spanId(UUID.randomUUID().toString())
            .build();
    smsService.sendSms(sms);
    smsService.sendSms(sms);

    var requestArgumentCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    Mockito.verify(snsClient, times(1)).publish(requestArgumentCaptor.capture());
    var publishRequest = requestArgumentCaptor.getValue();
    Assertions.assertEquals(
        sms.getSms().getPhoneNumber().withoutPlus(), publishRequest.phoneNumber());
  }
}
