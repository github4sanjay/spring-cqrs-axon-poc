package com.example.gateway;

import java.util.ArrayList;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@Slf4j
public final class CustomAuthoritiesConverter
    implements Converter<Jwt, Collection<GrantedAuthority>> {

  private static final String DEFAULT_AUTHORITY_PREFIX = "SCOPE_";
  private static final String SCOPE_INSTITUTIONS = "institutions";
  private static final String SCOPE_MARKET_DATA = "market-data";
  private static final String SCOPE_TREASURY = "treasury";

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
    var isInstitutions = (Boolean) jwt.getClaim(SCOPE_INSTITUTIONS);
    if (isInstitutions != null && isInstitutions) {
      grantedAuthorities.add(
          new SimpleGrantedAuthority(DEFAULT_AUTHORITY_PREFIX + SCOPE_INSTITUTIONS));
    }

    var isMarketData = (Boolean) jwt.getClaim(SCOPE_MARKET_DATA);
    if (isMarketData != null && isMarketData) {
      grantedAuthorities.add(
          new SimpleGrantedAuthority(DEFAULT_AUTHORITY_PREFIX + SCOPE_MARKET_DATA));
    }

    var isTreasury = (Boolean) jwt.getClaim(SCOPE_TREASURY);
    if (isTreasury != null && isTreasury) {
      grantedAuthorities.add(new SimpleGrantedAuthority(DEFAULT_AUTHORITY_PREFIX + SCOPE_TREASURY));
    }
    return grantedAuthorities;
  }
}
