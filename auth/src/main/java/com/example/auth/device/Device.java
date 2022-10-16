package com.example.auth.device;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "devices")
public class Device {

  @Id private String id;
  private Integer saltLength;
  private Integer hashLength;
  private String hash;
  @Lob private String publicKey;
  private String name;
  private String client;
  private String model;
  private String manufacturer;
  private String os;
}
