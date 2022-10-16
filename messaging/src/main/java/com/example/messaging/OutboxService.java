package com.example.messaging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

  public static final String EMAIL_MAP = "messaging-outbox-email-message";
  private static final String SMS_MAP = "messaging-outbox-sms-message";

  private final RedissonClient redisson;

  public void addSms(SmsMessage message) {
    RMapCache<String, SmsMessage> map = redisson.getMapCache(SMS_MAP);
    map.put(message.getSpanId(), message, 1, TimeUnit.DAYS);
  }

  public void addEmail(EmailMessage message) {
    RMapCache<String, EmailMessage> map = redisson.getMapCache(EMAIL_MAP);
    map.put(message.getSpanId(), message, 1, TimeUnit.DAYS);
  }

  public Map<String, SmsMessage> getAllSms() {
    RMapCache<String, SmsMessage> map = redisson.getMapCache(SMS_MAP);
    return map.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  public Map<String, EmailMessage> getAllEmails() {
    RMapCache<String, EmailMessage> map = redisson.getMapCache(EMAIL_MAP);
    return map.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }
}
