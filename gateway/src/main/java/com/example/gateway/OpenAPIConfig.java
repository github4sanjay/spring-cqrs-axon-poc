package com.example.gateway;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;

@Configuration
public class OpenAPIConfig {

  private final OpenAPIService openAPIService;

  @Autowired
  public OpenAPIConfig(OpenAPIService openAPIService) {
    this.openAPIService = openAPIService;
  }

  @Bean
  public OpenAPI customOpenAPI() throws IOException {
    return openAPIService.get(false, false);
  }

  public interface Client {
    @GetMapping("/v3/api-docs")
    OpenAPI getApiDoc();

    String getName();
  }

  @FeignClient(name = "auth")
  public interface Config extends Client {
    @Override
    default String getName() {
      return "auth";
    }
  }
}
