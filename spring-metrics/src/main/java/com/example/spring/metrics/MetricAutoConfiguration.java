package com.example.spring.metrics;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({CloudWatchMetricsAutoConfiguration.class})
public class MetricAutoConfiguration {}
