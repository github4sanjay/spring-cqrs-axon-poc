package com.example.gateway;

import com.example.spring.core.exceptions.ApplicationException;
import com.example.spring.core.exceptions.Error;
import com.example.spring.core.json.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MFAUtil {

  private final ObjectMapper objectMapper;
  private final OktaService oktaService;

  public MFAUtil(ObjectMapper objectMapper, OktaService oktaService) {
    this.objectMapper = objectMapper;
    this.oktaService = oktaService;
  }

  public Mono<Response> verifyFactor(String passcode, String userId, String factorType) {
    return oktaService
        .verifyFactor(factorType, passcode, userId)
        .map(successResponse -> Response.builder().successResponse(successResponse).build())
        .onErrorResume(
            throwable -> {
              var e = (ApplicationException) throwable;
              var error = new Error();
              error.code = e.getCode();
              error.description = e.getMessage();
              error.datetime = Instant.now();
              error.httpStatus = e.status;
              return Mono.just(Response.builder().errorResponse(new ErrorResponse(error)).build());
            });
  }

  public Mono<Void> errorResult(Object responseVO, ServerHttpResponse response) {
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
    DataBuffer buffer = null;
    try {
      buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(responseVO));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return response.writeWith(Flux.just(buffer));
  }

  @ToString
  @Getter
  @Setter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MfaErrorResponse {
    private MfaError error;
  }

  @ToString
  @Getter
  @Setter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MfaError extends Error {
    private OktaService.FactorsResponse options;
  }

  @ToString
  @Getter
  @Setter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Response {
    private ErrorResponse errorResponse;
    private OktaService.SuccessResponse successResponse;
  }

  public MfaErrorResponse getErrorResponse(OktaService.FactorsResponse options, String code) {
    var mfaError = MfaError.builder().options(options).build();
    mfaError.code = code;
    mfaError.description = "one or more factor is required for this operation";
    mfaError.datetime = Instant.now();
    mfaError.httpStatus = HttpStatus.UNAUTHORIZED;
    return MfaErrorResponse.builder().error(mfaError).build();
  }
}
