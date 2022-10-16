package com.example.messaging;

import com.example.api.messaging.PhoneNumber;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

  private static final Pattern DATA = Pattern.compile("(?s)^data:(.*);base64,(.*)$");

  public EmailMessage getEmail(
      String id, String body, String from, List<String> to, String subject) {

    var document = Jsoup.parse(body);

    // Use the <title> as the mail subject if no subject is specified
    if (subject == null) {
      Element title = document.getElementsByTag("title").first();
      if (title == null) {
        throw new RuntimeException("email title is required");
      }
      subject = title.text();
    }

    // Replace inline images with attachments for better e-mail client compatibility
    var inlineImages = new ArrayList<EmailMessage.EmbeddedImage>();
    for (Element element : document.getElementsByTag("img")) {
      String src = element.attr("src");
      Matcher dataUrl = DATA.matcher(src);
      if (!dataUrl.matches()) continue;
      var name = "image" + inlineImages.size() + 1;
      element.attr("src", "cid:" + name);
      var image =
          EmailMessage.EmbeddedImage.builder()
              .cid(name)
              .data(Base64.getMimeDecoder().decode(dataUrl.group(2)))
              .contentType(dataUrl.group(1))
              .build();
      inlineImages.add(image);
    }
    body = inlineImages.size() == 0 ? body : document.outerHtml();
    var email =
        EmailMessage.Email.builder()
            .body(body)
            .from(from)
            .images(inlineImages)
            .subject(subject)
            .to(to)
            .build();
    return EmailMessage.builder().email(email).spanId(id).build();
  }

  public SmsMessage getSms(String id, PhoneNumber phoneNumber, String message) {
    var sms = SmsMessage.Sms.builder().message(message).phoneNumber(phoneNumber).build();
    return SmsMessage.builder().spanId(id).sms(sms).build();
  }
}
