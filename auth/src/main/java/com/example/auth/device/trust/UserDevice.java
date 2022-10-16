package com.example.auth.device.trust;

import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.*;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "user_devices")
public class UserDevice {

  @Id private String id;
  private String deviceId;
  private String accountId;
  private Boolean trusted;
  private String biometric;
  private Instant expireAt;
  private Instant lastLoginAt;
}
