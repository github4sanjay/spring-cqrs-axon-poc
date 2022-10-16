package com.example.gateway;

import com.example.security.core.Signature;
import org.springframework.util.StringUtils;

public class RequestSignature implements Signature {
  private final String body;
  private final String queryParam;

  public RequestSignature(String body, String queryString) {
    this.body = StringUtils.hasText(body) ? body : "";
    this.queryParam = StringUtils.hasText(queryString) ? queryString : "";
  }

  public String create() {
    return "Signature{" + "body='" + body + '\'' + ", queryParam='" + queryParam + '\'' + '}';
  }
}
