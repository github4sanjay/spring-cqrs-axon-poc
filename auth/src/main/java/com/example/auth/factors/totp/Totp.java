package com.example.auth.factors.totp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "totps")
public class Totp {
  @Id private String accountId;
  private String secret;
  @Lob private String recoveryCode;
  @Lob private byte[] iv;
}
