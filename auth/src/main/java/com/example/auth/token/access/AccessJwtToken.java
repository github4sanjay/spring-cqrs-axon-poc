package com.example.auth.token.access;

import com.example.auth.token.Claims;
import lombok.Getter;

/** Raw representation of JWT Token */
@Getter
public final class AccessJwtToken implements JwtToken {
  private final String token;
  private final Claims claims;

  AccessJwtToken(final String token, final Claims claims) {
    this.token = token;
    this.claims = claims;
  }
}
