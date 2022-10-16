package com.example.otp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestUtils {

  public static String extractOtp(final String in) {
    final Pattern p = Pattern.compile("(\\d{6})");
    final Matcher m = p.matcher(in);
    if (m.find()) {
      return m.group(0);
    }
    throw new RuntimeException("otp not found");
  }
}
