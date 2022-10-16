package com.example.gateway;

import com.example.spring.core.exceptions.Error;
import com.example.spring.core.json.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import reactor.core.publisher.Mono;

@Slf4j
@EnableWebFluxSecurity
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
public class SecurityConfiguration {

  private final ObjectMapper objectMapper;

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http, RouteConfiguration routeConfiguration) {
    var exchangeSpec = http.authorizeExchange();
    exchangeSpec
        .pathMatchers(
            routeConfiguration
                .getRoutesWithoutMethodNotRequireAuthentication()
                .toArray(new String[0]))
        .permitAll();
    for (var entry : routeConfiguration.getRoutesNotRequireAuthentication().entrySet()) {
      exchangeSpec
          .pathMatchers(entry.getKey(), entry.getValue().toArray(new String[0]))
          .permitAll();
    }

    exchangeSpec
        .matchers(signatureHeaderMatcher())
        .permitAll()
        .pathMatchers("/.well-known/jwks.json")
        .permitAll()
        .pathMatchers(
            "/swagger-ui.html/**",
            "/*/v3/api-docs/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/",
            "/api-docs")
        .permitAll()
        /*.pathMatchers("/actuator/metrics/**")
        .denyAll()*/
        .pathMatchers("/actuator/**")
        .permitAll()
        .pathMatchers("/*/actuator/**")
        .denyAll()
        // scope check for authorization related apis
        .pathMatchers("/organizations/**")
        .hasAuthority("SCOPE_institutions")
        .pathMatchers(
            "/internal/api/v1/order-book",
            "/internal/api/v1/topN",
            "/internal/api/v1/topN/exchange")
        .hasAuthority("SCOPE_market-data")
        .pathMatchers("/internal/api/vi/treasury/**", "/internal/api/v1/staking-pools/**")
        .hasAuthority("SCOPE_treasury")
        .anyExchange()
        .authenticated()
        .and()
        .csrf()
        .disable()
        .oauth2ResourceServer()
        .jwt()
        .jwtAuthenticationConverter(getCustomConverter())
        .and()
        .authenticationEntryPoint(
            (exchange, e) -> {
              var errorResponse = new ErrorResponse();
              var error = new Error();
              error.datetime = Instant.now();
              error.code = "invalid-access-token";
              error.description = "invalid access token";
              error.httpStatus = HttpStatus.UNAUTHORIZED;
              errorResponse.error = error;
              var response = exchange.getResponse();
              response.setStatusCode(HttpStatus.UNAUTHORIZED);
              response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
              try {
                return response.writeWith(
                    Mono.just(
                        response
                            .bufferFactory()
                            .allocateBuffer()
                            .write(objectMapper.writeValueAsBytes(errorResponse))));
              } catch (JsonProcessingException ex) {
                log.error("error in authenticationEntryPoint", ex);
                var result =
                    "{\"error\":{\"datetime\":\""
                        + Instant.now().toString()
                        + "\",\"code\":\"invalid-access-token\",\"description\":\"invalid access token\",\"parameters\":{},\"httpStatus\":\"UNAUTHORIZED\"}}";
                return response.writeWith(
                    Mono.just(response.bufferFactory().allocateBuffer().write(result.getBytes())));
              }
            });
    return http.build();
  }

  private Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>>
      getCustomConverter() {
    var converter = new JwtAuthenticationConverter();
    // converter.setPrincipalClaimName("account");
    converter.setJwtGrantedAuthoritiesConverter(getJwtGrantedAuthoritiesConverter());
    return new ReactiveJwtAuthenticationConverterAdapter(converter);
  }

  private Converter<Jwt, Collection<GrantedAuthority>> getJwtGrantedAuthoritiesConverter() {
    return new CustomAuthoritiesConverter();
  }

  private ServerWebExchangeMatcher signatureHeaderMatcher() {
    return (exchange) ->
        exchange.getRequest().getHeaders().containsKey("x-signature")
            ? ServerWebExchangeMatcher.MatchResult.match()
            : ServerWebExchangeMatcher.MatchResult.notMatch();
  }
}
