package com.example.auth.token;

import com.example.auth.token.access.JwtTokenFactory;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Converts the Authorization header into a Claims object and stores it as a request attribute. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationConvertor implements WebFilter {

  public static final String HEADER_PREFIX = "Bearer ";
  public static final String AUTH_HEADER = "Authorization";

  private final JwtTokenFactory jwtTokenFactory;

  @Override
  @NonNull
  public Mono<Void> filter(
      ServerWebExchange serverWebExchange, @NonNull WebFilterChain webFilterChain) {
    var serverRequest = serverWebExchange.getRequest();
    var headers = serverRequest.getHeaders();
    var authHeaders = headers.get(AUTH_HEADER);
    var uri = serverRequest.getPath().value();
    if (authHeaders == null || authHeaders.isEmpty())
      return webFilterChain.filter(serverWebExchange);
    var authHeader = authHeaders.get(0);
    if (!StringUtils.isBlank(authHeader)
        && authHeader.length() > HEADER_PREFIX.length()
        && authHeader.startsWith(HEADER_PREFIX)
        && !uri.equals("/api/v1/introspect")) {
      var token = authHeader.substring(HEADER_PREFIX.length());
      var claims = jwtTokenFactory.decodeToken(token);
      serverWebExchange.getAttributes().put("claims", claims);
    }
    return webFilterChain.filter(serverWebExchange);
  }
}
