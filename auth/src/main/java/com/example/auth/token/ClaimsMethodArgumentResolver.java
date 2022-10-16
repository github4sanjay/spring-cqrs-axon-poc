package com.example.auth.token;

import com.example.auth.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class ClaimsMethodArgumentResolver implements WebFluxConfigurer {

  @Override
  public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
    configurer.addCustomResolver(
        new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(@NonNull MethodParameter methodParameter) {
            var parameterType = methodParameter.getParameterType();
            return Claims.class.isAssignableFrom(parameterType);
          }

          @Override
          @NonNull
          public Mono<Object> resolveArgument(
              @NonNull MethodParameter parameter,
              @NonNull BindingContext bindingContext,
              @NonNull ServerWebExchange exchange) {
            var claims = (Claims) exchange.getAttribute("claims");
            if (claims == null) {
              throw AuthException.INVALID_TOKEN.getEx();
            }
            return Mono.just(claims);
          }
        });
  }
}
