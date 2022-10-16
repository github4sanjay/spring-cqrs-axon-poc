package com.example.spring.metrics;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;
import org.springframework.lang.NonNull;

public class CloudWatchMetricsConfigAdapter
    extends StepRegistryPropertiesConfigAdapter<CloudWatchMetricsProperties>
    implements CloudWatchConfig {

  public CloudWatchMetricsConfigAdapter(CloudWatchMetricsProperties properties) {
    super(properties);
  }

  @Override
  @NonNull
  public String prefix() {
    return "management.metrics.export.cloudwatch";
  }

  @Override
  @NonNull
  public String namespace() {
    return get(CloudWatchMetricsProperties::getNamespace, CloudWatchConfig.super::namespace);
  }
}
