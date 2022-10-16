package com.example.api.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {

  @Bean
  Logger.Level feignLoggerLevel() {
    return Logger.Level.FULL;
  }

  @Autowired
  @Bean
  public ErrorDecoder feignErrorDecoder(ObjectMapper objectMapper) {
    return new APIErrorDecoder(new DefaultFeignExceptionHandler(objectMapper));
  }
}
