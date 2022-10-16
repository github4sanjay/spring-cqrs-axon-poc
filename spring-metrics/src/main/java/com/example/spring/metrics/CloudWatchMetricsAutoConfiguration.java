package com.example.spring.metrics;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.cloudwatch2.CloudWatchNamingConvention;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.config.NamingConvention;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({
  CompositeMeterRegistryAutoConfiguration.class,
  SimpleMetricsExportAutoConfiguration.class
})
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnClass(CloudWatchMeterRegistry.class)
@ConditionalOnProperty(
    prefix = "management.metrics.export.cloudwatch",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(CloudWatchMetricsProperties.class)
public class CloudWatchMetricsAutoConfiguration {

  @Bean
  @Primary
  @ConditionalOnMissingBean
  public CloudWatchMeterRegistry cloudWatchMeterRegistry(
      CloudWatchConfig config, Clock clock, CloudWatchAsyncClient client) {
    CloudWatchMeterRegistry registry = new CloudWatchMeterRegistry(config, clock, client);
    registry
        .config()
        .namingConvention(new CloudWatchNamingConvention(NamingConvention.upperCamelCase));
    return registry;
  }

  @Bean
  @ConditionalOnMissingBean
  public CloudWatchAsyncClient amazonCloudWatchAsync() {
    return CloudWatchAsyncClient.create();
  }

  @Bean
  @ConditionalOnMissingBean
  public CloudWatchConfig cloudWatchConfig(CloudWatchMetricsProperties properties) {
    return new CloudWatchMetricsConfigAdapter(properties);
  }
}
