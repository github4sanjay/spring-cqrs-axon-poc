package com.example.auth.common;

import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnapshotConfiguration {

  @Bean
  public SnapshotTriggerDefinition snapshotTriggerDefinition(Snapshotter snapshotter) {
    return new EventCountSnapshotTriggerDefinition(snapshotter, 10);
  }
}
