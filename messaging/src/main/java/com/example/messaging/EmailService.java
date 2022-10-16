package com.example.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

@Slf4j
@Service
@AllArgsConstructor
public class EmailService {

  public static final String EMAIL_MAP = "messaging-email-message";
  private final SesClient ses;
  private final RedissonClient redisson;

  public void sendEmail(EmailMessage emailMessage) {

    if (Instant.parse(emailMessage.getSentAt()).isBefore(Instant.now().minus(1, ChronoUnit.DAYS))) {
      log.info("ignoring email {} because of delayed delivery", emailMessage.getSpanId());
      return;
    }

    RMapCache<String, EmailMessage> map = redisson.getMapCache(EMAIL_MAP);
    if (map.containsKey(emailMessage.getSpanId())) return;

    log.debug("Sending e-mail: {}", emailMessage);
    var email = emailMessage.getEmail();

    byte[] body;

    try {
      var session = Session.getDefaultInstance(new Properties());
      var mime = new MimeMessage(session);
      mime.setSubject(email.getSubject(), "UTF-8");
      mime.setFrom(new InternetAddress(email.getFrom()));
      for (String address : email.getTo()) {
        mime.setRecipients(MimeMessage.RecipientType.TO, address);
      }

      var html = new MimeBodyPart();
      html.setContent(email.getBody(), "text/html; charset=UTF-8");

      var alternative = new MimeMultipart("alternative");
      alternative.addBodyPart(html);

      var wrapper = new MimeBodyPart();
      wrapper.setContent(alternative);

      var mixed = new MimeMultipart();
      mime.setContent(mixed);
      mixed.addBodyPart(wrapper);

      for (EmailMessage.EmbeddedImage image : email.getImages()) {
        var part = new MimeBodyPart();
        part.setDataHandler(
            new DataHandler(new ByteArrayDataSource(image.getData(), image.getContentType())));
        part.setDisposition(Part.INLINE);
        part.setContentID("<" + image.getCid() + ">");
        part.setHeader("X-Attachment-Id", image.getCid());
        part.setFileName(image.getCid());
        mixed.addBodyPart(part);
      }

      var outputStream = new ByteArrayOutputStream();
      mime.writeTo(outputStream);
      body = outputStream.toByteArray();

    } catch (MessagingException | IOException e) {
      throw new IllegalStateException("Could not create message", e);
    }

    ses.sendRawEmail(
        SendRawEmailRequest.builder()
            .rawMessage(RawMessage.builder().data(SdkBytes.fromByteArray(body)).build())
            .source(email.getFrom())
            .build());

    map.put(emailMessage.getSpanId(), emailMessage, 5, TimeUnit.DAYS);
  }
}
