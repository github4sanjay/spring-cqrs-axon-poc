package com.example.spring.web;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import java.util.Map;

public class CustomJsonLayout extends JsonLayout {
  @Override
  protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
    super.addCustomDataToJsonMap(map, event);
    var context = SpringContext.getContext();
    map.put("serviceId", context.getEnvironment().getProperty("spring.application.name"));
  }
}
