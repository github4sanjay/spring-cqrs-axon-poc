package com.example.spring.web;

import com.amazonaws.util.EC2MetadataUtils;
import java.util.Arrays;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
public class BootstrapConfig {

  private final Environment environment;

  @Autowired
  public BootstrapConfig(Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  public void postConstruct() {
    if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
      var region = EC2MetadataUtils.getEC2InstanceRegion();
      if (region == null) {
        log.info("ec2 metadata region is null so taking default ap-southeast-1");
        System.setProperty("aws.region", "ap-southeast-1");
      } else {
        log.info("ec2 metadata region is {}", region);
        System.setProperty("aws.region", region);
      }
    } else {
      System.setProperty("aws.region", "ap-southeast-1");
    }
  }
}
