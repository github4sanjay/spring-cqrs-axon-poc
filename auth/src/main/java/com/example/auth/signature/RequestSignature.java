package com.example.auth.signature;

import com.example.security.core.Signature;
import org.springframework.util.StringUtils;

public class RequestSignature implements Signature {
  private final String clientCode;
  private final String authorization;
  private final String body;
  private final String queryParam;

  public RequestSignature(
      String clientCode, String authorization, String body, String queryString) {
    this.clientCode = clientCode;
    this.authorization = StringUtils.hasLength(authorization) ? authorization : "";
    this.body = StringUtils.hasLength(body) ? body : "";
    this.queryParam = StringUtils.hasLength(queryString) ? queryString : "";
  }

  public String create() {
    return "Signature{"
        + "clientCode='"
        + clientCode
        + '\''
        + ", authorization='"
        + authorization
        + '\''
        + ", body='"
        + body
        + '\''
        + ", queryParam='"
        + queryParam
        + '\''
        + '}';
  }
}
