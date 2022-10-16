package com.example.spring.metrics;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "management.metrics.export.cloudwatch")
public class CloudWatchMetricsProperties extends StepRegistryProperties {

  public CloudWatchMetricsProperties() {
    setBatchSize(CloudWatchConfig.MAX_BATCH_SIZE);
  }

  /** Custom namespace */
  private String namespace = "Actuator";

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
}
