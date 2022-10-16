package com.example.auth.account;

import javax.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "accounts")
public class Account {

  @Id private String id;
  private Integer saltLength;
  private Integer hashLength;
  private String password;

  @Column(unique = true)
  private String email;

  @Enumerated(EnumType.STRING)
  private AccountStatus status;
}
