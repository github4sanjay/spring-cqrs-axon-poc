package com.example.gateway;

import com.example.spring.core.exceptions.ApplicationException;
import com.example.spring.core.json.ErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OktaService {

  private final WebClient.Builder webClientBuilder;
  private final Cache<String, String> oktaIdCache;
  private final Cache<String, FactorsResponse> factorsCache;

  @Autowired
  public OktaService(WebClient.Builder webClientBuilder) {
    this.webClientBuilder = webClientBuilder;
    this.oktaIdCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
    this.factorsCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
  }

  public Mono<String> getUserId(String oktaId) {
    var userId = oktaIdCache.getIfPresent(oktaId);
    if (userId != null) return Mono.just(userId);
    else return get(oktaId).doOnNext(s -> oktaIdCache.put(oktaId, s));
  }

  private Mono<String> get(String oktaId) {
    return webClientBuilder
        .build()
        .get()
        .uri("http://user//internal/users/{oktaId}/userId", oktaId)
        .retrieve()
        .onStatus(
            HttpStatus::is4xxClientError,
            error ->
                error
                    .bodyToMono(ErrorResponse.class)
                    .map(
                        errorResponse ->
                            new ApplicationException(
                                errorResponse.error.code,
                                errorResponse.error.description,
                                errorResponse.error.httpStatus)))
        .onStatus(
            HttpStatus::is5xxServerError,
            error ->
                error
                    .bodyToMono(ErrorResponse.class)
                    .map(
                        errorResponse ->
                            new ApplicationException(
                                errorResponse.error.code,
                                errorResponse.error.description,
                                errorResponse.error.httpStatus)))
        .bodyToMono(UserIdResponse.class)
        .map(userIdResponse -> userIdResponse.getUserId().toString());
  }

  public Mono<SuccessResponse> verifyFactor(String factor, String code, String userId) {
    return webClientBuilder
        .build()
        .post()
        .uri("http://user//me/mfa/{factor}/verify", factor)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header("X-AUTHORIZATION-ID", userId)
        .body(Mono.just(VerifyOtpRequest.builder().code(code).build()), VerifyOtpRequest.class)
        .retrieve()
        .onStatus(
            HttpStatus::is4xxClientError,
            error ->
                error
                    .bodyToMono(ErrorResponse.class)
                    .map(
                        errorResponse ->
                            new ApplicationException(
                                errorResponse.error.code,
                                errorResponse.error.description,
                                errorResponse.error.httpStatus)))
        .onStatus(
            HttpStatus::is5xxServerError,
            error ->
                error
                    .bodyToMono(ErrorResponse.class)
                    .map(
                        errorResponse ->
                            new ApplicationException(
                                errorResponse.error.code,
                                errorResponse.error.description,
                                errorResponse.error.httpStatus)))
        .bodyToMono(SuccessResponse.class);
  }

  public Mono<FactorsResponse> getFactors(String userId) {
    var factorsResponse = factorsCache.getIfPresent(userId);
    if (factorsResponse != null) return Mono.just(factorsResponse);
    return webClientBuilder
        .build()
        .get()
        .uri("http://user//me/mfa/factors")
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header("X-AUTHORIZATION-ID", userId)
        .retrieve()
        .onStatus(
            HttpStatus::is4xxClientError,
            error ->
                error
                    .bodyToMono(ErrorResponse.class)
                    .map(
                        errorResponse ->
                            new ApplicationException(
                                errorResponse.error.code,
                                errorResponse.error.description,
                                errorResponse.error.httpStatus)))
        .onStatus(
            HttpStatus::is4xxClientError,
            error ->
                error
                    .bodyToMono(ErrorResponse.class)
                    .map(
                        errorResponse ->
                            new ApplicationException(
                                errorResponse.error.code,
                                errorResponse.error.description,
                                errorResponse.error.httpStatus)))
        .bodyToMono(FactorsResponse.class)
        .doOnNext(newFactorsResponse -> factorsCache.put(userId, newFactorsResponse));
  }

  public void clearFromCache(String userId) {
    factorsCache.invalidate(userId);
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserIdResponse {
    private UUID userId;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SuccessResponse {
    private String status;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VerifyOtpRequest {
    public String code;
  }

  @Getter
  @Setter
  @Builder
  @ToString
  @AllArgsConstructor
  @NoArgsConstructor
  public static class FactorsResponse {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Factor email;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Factor sms;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Factor authenticator;
  }

  @Getter
  @Setter
  @Builder
  @ToString
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Factor {
    private String status;
  }
}
