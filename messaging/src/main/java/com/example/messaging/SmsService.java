package com.example.messaging;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
@Service
@AllArgsConstructor
public class SmsService {

  public static final String SMS_MAP = "messaging-sms-message";

  private final SnsClient snsClient;
  private final RedissonClient redisson;

  public void sendSms(SmsMessage smsMessage) {
    if (Instant.parse(smsMessage.getSentAt()).isBefore(Instant.now().minus(1, ChronoUnit.DAYS))) {
      log.info("ignoring sms {} because of delayed delivery", smsMessage.getSpanId());
      return;
    }
    RMapCache<String, SmsMessage> map = redisson.getMapCache(SMS_MAP);
    if (map.containsKey(smsMessage.getSpanId())) return;

    snsClient.publish(
        PublishRequest.builder()
            .messageAttributes(Map.of())
            .phoneNumber(smsMessage.getSms().getPhoneNumber().withoutPlus())
            .message(smsMessage.getSms().getMessage())
            .build());

    map.put(smsMessage.getSpanId(), smsMessage, 5, TimeUnit.DAYS);
  }
}
