package com.example.api.otp;

import javax.validation.ConstraintViolationException;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class PhoneNumber {
  private String value;

  @Builder
  public PhoneNumber(String value) {
    if (!value.startsWith("+")) {
      throw new ConstraintViolationException("should start with '+'", null);
    }
    this.value = value;
  }

  public String withoutPlus() {
    return value.substring(1);
  }
}
