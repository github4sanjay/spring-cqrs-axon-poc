package com.example.auth.factors;

import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "factors")
public class Factor {

  @Id private String id;
  private String accountId;
  private FactorType factorType;
  private Boolean enabled;
}
