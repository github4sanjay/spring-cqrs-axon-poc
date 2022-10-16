package com.example.messaging;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class AwsConfiguration {

  private @Value("${aws.endpoint-url:#{null}}") URI endpoint;

  @Bean
  @ConditionalOnMissingBean
  public SesClient sesClient() {
    if (endpoint != null) {
      return SesClient.builder().endpointOverride(endpoint).build();
    }
    return SesClient.builder().build();
  }

  @Bean
  @ConditionalOnMissingBean
  public SnsClient snsClient() {
    if (endpoint != null) {
      return SnsClient.builder().endpointOverride(endpoint).build();
    }
    return SnsClient.create();
  }
}
