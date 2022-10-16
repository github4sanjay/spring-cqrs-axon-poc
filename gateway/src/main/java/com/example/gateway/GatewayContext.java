package com.example.gateway;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.MultiValueMap;

@Getter
@Setter
public class GatewayContext {
  public static final String CACHE_GATEWAY_CONTEXT = "cacheGatewayContext";
  private String cacheBody;
  private MultiValueMap<String, String> formData;
  private String path;
  private String query;
}
