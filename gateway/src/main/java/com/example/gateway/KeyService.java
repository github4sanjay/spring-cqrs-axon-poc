package com.example.gateway;

import com.example.spring.core.exceptions.ApplicationException;
import com.example.spring.core.json.ErrorResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class KeyService {

  private final WebClient.Builder webClientBuilder;
  private final Cache<String, KeyResponse> keyCache;

  @Autowired
  public KeyService(WebClient.Builder webClientBuilder) {
    this.webClientBuilder = webClientBuilder;
    this.keyCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
  }

  public Mono<KeyResponse> getKey(String publicKey) {
    var keyResponse = keyCache.getIfPresent(publicKey);
    if (keyResponse != null) return Mono.just(keyResponse);
    else return get(publicKey).doOnNext(s -> keyCache.put(publicKey, s));
  }

  public Mono<KeyResponse> get(String keyId) {
    return webClientBuilder
        .build()
        .get()
        .uri("http://key//internal/api/v1/keys/{id}", keyId)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
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
        .bodyToMono(KeyResponse.class);
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class KeyResponse {
    private String publicKey;
    private String label;
    private UUID keyId;
    private UUID userId;
    private Instant createdAt;
  }
}
