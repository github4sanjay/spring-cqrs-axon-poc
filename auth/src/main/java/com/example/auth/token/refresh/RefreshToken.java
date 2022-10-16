package com.example.auth.token.refresh;

import com.example.auth.token.Claims;
import java.time.Duration;
import java.time.Instant;
import javax.persistence.*;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "refresh_tokens")
public class RefreshToken {

  @Id private String id;
  private String deviceId;

  private String subject;

  @Enumerated(EnumType.STRING)
  private Claims.AMR amr;

  private String token;

  private Instant expireAt;
  private Instant createdAt;
  private Duration refreshChainExpiry;
}
