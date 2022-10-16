package com.example.gateway;

import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.swagger.v3.core.util.Json;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {

  @Bean
  public Decoder decoder() {
    return new JacksonDecoder(Json.mapper());
  }

  @Bean
  public Encoder encoder() {
    return new JacksonEncoder();
  }
}
