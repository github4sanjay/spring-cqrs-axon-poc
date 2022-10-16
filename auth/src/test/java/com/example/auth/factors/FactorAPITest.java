package com.example.auth.factors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.api.otp.*;
import com.example.auth.AuthException;
import com.example.auth.account.Account;
import com.example.auth.account.AccountQuery;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.Device;
import com.example.auth.device.DeviceController;
import com.example.auth.device.DeviceQuery;
import com.example.auth.device.DeviceRegistrationConfig;
import com.example.auth.factors.totp.Totp;
import com.example.auth.factors.totp.TotpCommand;
import com.example.auth.factors.totp.TotpQuery;
import com.example.auth.token.Claims;
import com.example.auth.token.ClaimsMethodArgumentResolver;
import com.example.auth.token.access.JWKSSettings;
import com.example.auth.token.access.JwtTokenFactory;
import com.example.auth.token.access.keys.PrivateSigningKeyRepository;
import com.example.auth.token.access.keys.PublicSigningKey;
import com.example.auth.token.access.keys.PublicSigningKeyRepository;
import com.example.auth.token.access.keys.SigningKeyService;
import com.example.spring.core.exceptions.CoreExceptions;
import com.example.spring.core.json.ErrorResponse;
import com.example.spring.web.GlobalErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.spring.autoconfigure.TotpAutoConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.thymeleaf.spring5.SpringTemplateEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxTest(
    controllers = FactorController.class,
    properties = {"app.request-verification.enabled=true"})
@ImportAutoConfiguration(value = {TotpAutoConfiguration.class})
@Import(
    value = {
      DeviceRegistrationConfig.class,
      ClientConfiguration.class,
      JwtTokenFactory.class,
      JWKSSettings.class,
      SigningKeyService.class,
      PrivateSigningKeyRepository.class,
      GlobalErrorHandler.class,
      ClaimsMethodArgumentResolver.class,
      SpringTemplateEngine.class
    })
public class FactorAPITest {

  @Autowired private WebTestClient webTestClient;
  @Autowired private DeviceRegistrationConfig deviceRegistrationConfig;
  @Autowired private JwtTokenFactory jwtTokenFactory;
  @Autowired private PrivateSigningKeyRepository privateSigningKeyRepository;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private ReactorQueryGateway reactorQueryGateway;
  @MockBean private ReactorCommandGateway reactorCommandGateway;
  @MockBean private PublicSigningKeyRepository publicSigningKeyRepository;

  private final String deviceId = UUID.randomUUID().toString();
  private final String password = UUID.randomUUID().toString();
  private final String accountId = UUID.randomUUID().toString();
  private final String email = "github4sanjay@gmail.com";

  private String ACCESS_TOKEN;

  @BeforeEach
  public void beforeEach() {
    privateSigningKeyRepository.clearKeys();
    var hash = deviceRegistrationConfig.getHash(password);
    Assertions.assertTrue(deviceRegistrationConfig.verifyHash(password, hash));
    var device =
        Device.builder()
            .publicKey("publicKey")
            .id(deviceId)
            .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
            .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
            .hash(hash)
            .client("web-app")
            .manufacturer("Windows")
            .os("windows")
            .model("DFFG-123")
            .name("Some Name")
            .build();
    when(reactorQueryGateway.query(
            DeviceQuery.GetDeviceByIdQuery.builder().id(deviceId).build(), Device.class))
        .thenReturn(Mono.just(device));
    var subject = Claims.AccountSubject.builder().id(accountId).email(email).build();

    var claims =
        Claims.builder()
            .customClaims(subject.getCustomClaims())
            .subject(subject)
            .amr(Claims.AMR.pwd)
            .aud("web-app")
            .build();

    when(publicSigningKeyRepository.save(any())).then(returnsFirstArg());

    ACCESS_TOKEN =
        jwtTokenFactory.createAccessJwtToken(claims, device.getClient(), accountId).getToken();

    ArgumentCaptor<PublicSigningKey> captor = ArgumentCaptor.forClass(PublicSigningKey.class);
    verify(publicSigningKeyRepository, times(1)).save(captor.capture());

    var publicSigningKey = captor.getValue();

    when(publicSigningKeyRepository.findById(publicSigningKey.getId()))
        .thenReturn(Optional.of(publicSigningKey));
  }

  @Test
  @DisplayName("test create email factor when SendOtpOutcomeResult Ok expect 200")
  void testCreateEmailFactorWhenSendOtpOutcomeResultOkExpect200() {

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByIdQuery.builder().id(accountId).build(), Account.class))
        .thenReturn(Mono.just(Account.builder().email(email).id(accountId).build()));

    var token = UUID.randomUUID().toString();
    var retryAfter = 10L;
    when(reactorCommandGateway.sendAll(any()))
        .thenReturn(
            Flux.just(
                UUID.randomUUID().toString(),
                SendOtpOutcome.builder()
                    .token(token)
                    .retryAfter(retryAfter)
                    .result(SendOtpResult.Ok)
                    .build()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_EMAIL_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(FactorAPI.ChallengeEmailFactorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              var challengeEmailFactorResponse = result.getResponseBody();
              assertEquals(token, challengeEmailFactorResponse.getChallengeToken());
              assertEquals(retryAfter, challengeEmailFactorResponse.getRetryAfter());
            });
  }

  @Test
  @DisplayName("test create email factor when SendOtpOutcomeResult BlockedEarlyRequest expect 401")
  void testCreateEmailFactorWhenSendOtpOutcomeResultBlockedEarlyRequestExpect401() {

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByIdQuery.builder().id(accountId).build(), Account.class))
        .thenReturn(Mono.just(Account.builder().email(email).id(accountId).build()));

    var token = UUID.randomUUID().toString();
    var retryAfter = 10L;
    when(reactorCommandGateway.sendAll(any()))
        .thenReturn(
            Flux.just(
                UUID.randomUUID().toString(),
                SendOtpOutcome.builder()
                    .token(token)
                    .retryAfter(retryAfter)
                    .result(SendOtpResult.BlockedEarlyRequest)
                    .build()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_EMAIL_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals(
                  AuthException.TOO_EARLY_OTP_REQUEST.getEx().code,
                  result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName(
      "test create email factor when SendOtpOutcomeResult BlockedTooManyRequests expect 401")
  void testCreateEmailFactorWhenSendOtpOutcomeResultBlockedTooManyRequestsExpect401() {

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByIdQuery.builder().id(accountId).build(), Account.class))
        .thenReturn(Mono.just(Account.builder().email(email).id(accountId).build()));

    var token = UUID.randomUUID().toString();
    var retryAfter = 10L;
    when(reactorCommandGateway.sendAll(any()))
        .thenReturn(
            Flux.just(
                UUID.randomUUID().toString(),
                SendOtpOutcome.builder()
                    .token(token)
                    .retryAfter(retryAfter)
                    .result(SendOtpResult.BlockedTooManyRequests)
                    .build()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_EMAIL_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals(
                  AuthException.TOO_MANY_OTP_REQUEST.getEx().code,
                  result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName(
      "test challenge email factor when factor is enabled SendOtpOutcomeResult Ok expect 200")
  void testChallengeEmailFactorWhenSendOtpOutcomeResultOkExpect200() {

    when(reactorQueryGateway.query(
            FactorQuery.GetFactorByAccountIdAndFactorTypeQuery.builder()
                .factorType(FactorType.email)
                .accountId(accountId)
                .build(),
            Factor.class))
        .thenReturn(
            Mono.just(
                Factor.builder()
                    .id(UUID.randomUUID().toString())
                    .factorType(FactorType.email)
                    .accountId(accountId)
                    .enabled(true)
                    .build()));

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByIdQuery.builder().id(accountId).build(), Account.class))
        .thenReturn(Mono.just(Account.builder().email(email).id(accountId).build()));

    var token = UUID.randomUUID().toString();
    var retryAfter = 10L;
    when(reactorCommandGateway.send(any()))
        .thenReturn(
            Mono.just(
                SendOtpOutcome.builder()
                    .token(token)
                    .retryAfter(retryAfter)
                    .result(SendOtpResult.Ok)
                    .build()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_CHALLENGE_EMAIL_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(FactorAPI.ChallengeEmailFactorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              var challengeEmailFactorResponse = result.getResponseBody();
              assertEquals(token, challengeEmailFactorResponse.getChallengeToken());
              assertEquals(retryAfter, challengeEmailFactorResponse.getRetryAfter());
            });
  }

  @Test
  @DisplayName(
      "test challenge email factor when factor not enabled expect error FACTOR_NOT_FOUND 404")
  void testChallengeEmailFactorWhenFactorNotEnabledExpectErrorFactorNotFound404() {

    when(reactorQueryGateway.query(
            FactorQuery.GetFactorByAccountIdAndFactorTypeQuery.builder()
                .factorType(FactorType.email)
                .accountId(accountId)
                .build(),
            Factor.class))
        .thenReturn(
            Mono.just(
                Factor.builder()
                    .id(UUID.randomUUID().toString())
                    .factorType(FactorType.email)
                    .accountId(accountId)
                    .enabled(false)
                    .build()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_CHALLENGE_EMAIL_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals(
                  AuthException.FACTOR_NOT_FOUND.getEx().code, result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName("test delete email factor when factor not exist expect error FACTOR_NOT_FOUND 404")
  void testDeleteEmailFactorWhenFactorNotExistExpectErrorFactorNotFound404() {

    when(reactorCommandGateway.send(
            FactorCommand.DeactivateFactorCommand.builder()
                .factorType(FactorType.email)
                .accountId(accountId)
                .build()))
        .thenReturn(Mono.error(CoreExceptions.AGGREGATE_NOT_FOUND.getEx()));

    webTestClient
        .delete()
        .uri("/api/v1/factors/email")
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals(
                  AuthException.FACTOR_NOT_FOUND.getEx().code, result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName("test delete email factor when factor exist expect 204")
  void testDeleteEmailFactorWhenFactorExistExpect204() {

    when(reactorCommandGateway.send(
            FactorCommand.DeactivateFactorCommand.builder()
                .factorType(FactorType.email)
                .accountId(accountId)
                .build()))
        .thenReturn(Mono.just(UUID.randomUUID().toString()));

    webTestClient
        .delete()
        .uri("/api/v1/factors/email")
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  @Test
  @DisplayName("test verify email when factor not exist expect error FACTOR_NOT_FOUND 404")
  void testVerifyEmailFactorWhenFactorNotExistExpectErrorFactorNotFound404() {

    var challengeToken = UUID.randomUUID().toString();
    var otp = "123456";

    when(reactorCommandGateway.send(
            OtpCommand.VerifyOtpCommand.builder().token(challengeToken).otp(otp).build()))
        .thenReturn(
            Mono.just(
                VerifyOtpOutcome.builder()
                    .result(VerifyOtpResult.Valid)
                    .state(
                        FactorController.OtpState.builder()
                            .factorType(FactorType.email)
                            .accountId(accountId)
                            .build()
                            .get(objectMapper))
                    .build()));

    when(reactorCommandGateway.send(
            FactorCommand.ActivateFactorCommand.builder()
                .factorType(FactorType.email)
                .accountId(accountId)
                .build()))
        .thenReturn(Mono.error(CoreExceptions.AGGREGATE_NOT_FOUND.getEx()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_VERIFY_EMAIL_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .body(
            Mono.just(
                FactorAPI.VerifyEmailFactorRequest.builder()
                    .challengeToken(challengeToken)
                    .otp(otp)
                    .build()),
            DeviceController.DeviceRequest.class)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals(
                  AuthException.FACTOR_NOT_FOUND.getEx().code, result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName("test verify email factor when VerifyOtpResult Valid expect 204")
  void testVerifyEmailFactorWhenVerifyOtpResultValidExpect204() {

    var challengeToken = UUID.randomUUID().toString();
    var otp = "123456";

    when(reactorCommandGateway.send(
            OtpCommand.VerifyOtpCommand.builder().token(challengeToken).otp(otp).build()))
        .thenReturn(
            Mono.just(
                VerifyOtpOutcome.builder()
                    .result(VerifyOtpResult.Valid)
                    .state(
                        FactorController.OtpState.builder()
                            .factorType(FactorType.email)
                            .accountId(accountId)
                            .build()
                            .get(objectMapper))
                    .build()));

    when(reactorCommandGateway.send(
            FactorCommand.ActivateFactorCommand.builder()
                .factorType(FactorType.email)
                .accountId(accountId)
                .build()))
        .thenReturn(Mono.just(UUID.randomUUID().toString()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_VERIFY_EMAIL_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .body(
            Mono.just(
                FactorAPI.VerifyEmailFactorRequest.builder()
                    .challengeToken(challengeToken)
                    .otp(otp)
                    .build()),
            DeviceController.DeviceRequest.class)
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  @Test
  @DisplayName("test verify email when VerifyOtpResult Invalid expect error INVALID_OTP 401")
  void testVerifyEmailFactorWhenVerifyOtpResultInvalidExpectErrorInvalidOtp401() {

    var challengeToken = UUID.randomUUID().toString();
    var otp = "123456";

    when(reactorCommandGateway.send(
            OtpCommand.VerifyOtpCommand.builder().token(challengeToken).otp(otp).build()))
        .thenReturn(
            Mono.just(
                VerifyOtpOutcome.builder()
                    .result(VerifyOtpResult.Invalid)
                    .state(
                        FactorController.OtpState.builder()
                            .factorType(FactorType.email)
                            .accountId(accountId)
                            .build()
                            .get(objectMapper))
                    .build()));

    when(reactorCommandGateway.send(
            FactorCommand.ActivateFactorCommand.builder()
                .factorType(FactorType.email)
                .accountId(accountId)
                .build()))
        .thenReturn(Mono.just(UUID.randomUUID().toString()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_VERIFY_EMAIL_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .body(
            Mono.just(
                FactorAPI.VerifyEmailFactorRequest.builder()
                    .challengeToken(challengeToken)
                    .otp(otp)
                    .build()),
            DeviceController.DeviceRequest.class)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals(
                  AuthException.INVALID_OTP.getEx().code, result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName(
      "test verify email when VerifyOtpResult Blocked expect error BLOCKED_OTP_VERIFICATION 401")
  void testVerifyEmailFactorWhenVerifyOtpResultBlockedExpectErrorBlockedOtpVerification401() {

    var challengeToken = UUID.randomUUID().toString();
    var otp = "123456";

    when(reactorCommandGateway.send(
            OtpCommand.VerifyOtpCommand.builder().token(challengeToken).otp(otp).build()))
        .thenReturn(
            Mono.just(
                VerifyOtpOutcome.builder()
                    .result(VerifyOtpResult.Blocked)
                    .state(
                        FactorController.OtpState.builder()
                            .factorType(FactorType.email)
                            .accountId(accountId)
                            .build()
                            .get(objectMapper))
                    .build()));

    when(reactorCommandGateway.send(
            FactorCommand.ActivateFactorCommand.builder()
                .factorType(FactorType.email)
                .accountId(accountId)
                .build()))
        .thenReturn(Mono.just(UUID.randomUUID().toString()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_VERIFY_EMAIL_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .body(
            Mono.just(
                FactorAPI.VerifyEmailFactorRequest.builder()
                    .challengeToken(challengeToken)
                    .otp(otp)
                    .build()),
            DeviceController.DeviceRequest.class)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals(
                  AuthException.BLOCKED_OTP_VERIFICATION.getEx().code,
                  result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName("test verify email when VerifyOtpResult Blocked expect error EXPIRED_OTP 401")
  void testVerifyEmailFactorWhenVerifyOtpResultBlockedExpectErrorExpiredOtp401() {

    var challengeToken = UUID.randomUUID().toString();
    var otp = "123456";

    when(reactorCommandGateway.send(
            OtpCommand.VerifyOtpCommand.builder().token(challengeToken).otp(otp).build()))
        .thenReturn(
            Mono.just(
                VerifyOtpOutcome.builder()
                    .result(VerifyOtpResult.Expired)
                    .state(
                        FactorController.OtpState.builder()
                            .factorType(FactorType.email)
                            .accountId(accountId)
                            .build()
                            .get(objectMapper))
                    .build()));

    when(reactorCommandGateway.send(
            FactorCommand.ActivateFactorCommand.builder()
                .factorType(FactorType.email)
                .accountId(accountId)
                .build()))
        .thenReturn(Mono.just(UUID.randomUUID().toString()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_VERIFY_EMAIL_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .body(
            Mono.just(
                FactorAPI.VerifyEmailFactorRequest.builder()
                    .challengeToken(challengeToken)
                    .otp(otp)
                    .build()),
            DeviceController.DeviceRequest.class)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals(
                  AuthException.EXPIRED_OTP.getEx().code, result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName("test create totp factor Ok expect 200")
  void testCreateTotpFactorExpect200() {

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByIdQuery.builder().id(accountId).build(), Account.class))
        .thenReturn(Mono.just(Account.builder().email(email).id(accountId).build()));

    when(reactorCommandGateway.sendAll(any()))
        .thenReturn(Flux.just(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_TOTP_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(FactorAPI.TotpFactorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              var challengeEmailFactorResponse = result.getResponseBody();
              assertNotNull(challengeEmailFactorResponse.getQrCode());
              assertNotNull(challengeEmailFactorResponse.getData());
              assertNotNull(challengeEmailFactorResponse.getRecoveryCode());
            });
  }

  @Test
  @DisplayName("test recover totp factor when recovery code is valid expect 200")
  void testRecoverTotpFactorWhenRecoveryCodeIsValidExpect200() {

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByIdQuery.builder().id(accountId).build(), Account.class))
        .thenReturn(Mono.just(Account.builder().email(email).id(accountId).build()));

    when(reactorCommandGateway.sendAll(any()))
        .thenReturn(Flux.just(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

    var totpFactorResponse =
        webTestClient
            .post()
            .uri(FactorAPI.FACTOR_TOTP_API)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header("x-client-code", deviceId + "." + password)
            .header("Authorization", "Bearer " + ACCESS_TOKEN)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FactorAPI.TotpFactorResponse.class)
            .getResponseBody()
            .blockFirst();

    assertNotNull(totpFactorResponse);

    ArgumentCaptor<Flux> captor = ArgumentCaptor.forClass(Flux.class);
    verify(reactorCommandGateway, times(1)).sendAll(captor.capture());
    var list = (List) captor.getValue().collectList().block();
    var createTotpCommand = (TotpCommand.CreateTotpCommand) list.get(1);

    when(reactorQueryGateway.query(
            TotpQuery.GetTotpByAccountIdQuery.builder().accountId(accountId).build(), Totp.class))
        .thenReturn(
            Mono.just(
                Totp.builder()
                    .accountId(accountId)
                    .iv(createTotpCommand.getIv())
                    .recoveryCode(createTotpCommand.getRecoveryCode())
                    .secret(createTotpCommand.getSecret())
                    .build()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_TOTP_RECOVERY_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .body(
            Mono.just(
                FactorAPI.TotpFactorRecoveryRequest.builder()
                    .recoveryCode(totpFactorResponse.getRecoveryCode())
                    .build()),
            FactorAPI.TotpFactorRecoveryRequest.class)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(FactorAPI.TotpFactorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              var challengeEmailFactorResponse = result.getResponseBody();
              assertNotNull(challengeEmailFactorResponse.getQrCode());
              assertNotNull(challengeEmailFactorResponse.getData());
              assertNotNull(challengeEmailFactorResponse.getRecoveryCode());
            });
  }

  @Test
  @DisplayName(
      "test recover totp factor when recovery code is invalid expect error INVALID_RECOVERY_CODE 401")
  void testRecoverTotpFactorWhenRecoveryCodeIsInValidExpect401() {

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByIdQuery.builder().id(accountId).build(), Account.class))
        .thenReturn(Mono.just(Account.builder().email(email).id(accountId).build()));

    when(reactorCommandGateway.sendAll(any()))
        .thenReturn(Flux.just(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_TOTP_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isOk();

    ArgumentCaptor<Flux> captor = ArgumentCaptor.forClass(Flux.class);
    verify(reactorCommandGateway, times(1)).sendAll(captor.capture());
    var list = (List) captor.getValue().collectList().block();
    var createTotpCommand = (TotpCommand.CreateTotpCommand) list.get(1);

    when(reactorQueryGateway.query(
            TotpQuery.GetTotpByAccountIdQuery.builder().accountId(accountId).build(), Totp.class))
        .thenReturn(
            Mono.just(
                Totp.builder()
                    .accountId(accountId)
                    .iv(createTotpCommand.getIv())
                    .recoveryCode(createTotpCommand.getRecoveryCode())
                    .secret(createTotpCommand.getSecret())
                    .build()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_TOTP_RECOVERY_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .body(
            Mono.just(
                FactorAPI.TotpFactorRecoveryRequest.builder()
                    .recoveryCode(
                        new String[] {
                          "4t03-orb6-f72z-e2th",
                          "axts-hibe-mvos-dht9",
                          "wmwy-vq8k-em1i-avvh",
                          "o3pv-718q-56ux-lttt",
                          "39m7-25sv-ss0s-6cg5",
                          "8ti8-d9iw-08w0-bi7e",
                          "q6po-atai-sbw7-s8wr",
                          "gkbd-rsjz-73va-z6io",
                          "ag3q-8u36-gade-57ie",
                          "uzmf-oiy1-ceoq-kfup",
                          "tbgm-105s-nyp8-b44m",
                          "a09i-cjgh-tjll-1e7m",
                          "ekyd-jdv7-el2z-5p4a",
                          "p1jq-c2r3-jy0k-yyzm",
                          "x28a-vo23-5z6y-266h",
                          "ky7r-sdan-ly4l-9mns"
                        })
                    .build()),
            FactorAPI.TotpFactorRecoveryRequest.class)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals(
                  AuthException.INVALID_RECOVERY_CODE.getEx().code,
                  result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName("test verify totp factor when totp is invalid expect error INVALID_OTP 401")
  void testVerifyTotpFactorWhenTotpCodeIsInValidExpect401() {

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByIdQuery.builder().id(accountId).build(), Account.class))
        .thenReturn(Mono.just(Account.builder().email(email).id(accountId).build()));

    when(reactorCommandGateway.sendAll(any()))
        .thenReturn(Flux.just(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_TOTP_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isOk();

    ArgumentCaptor<Flux> captor = ArgumentCaptor.forClass(Flux.class);
    verify(reactorCommandGateway, times(1)).sendAll(captor.capture());
    var list = (List) captor.getValue().collectList().block();
    var createTotpCommand = (TotpCommand.CreateTotpCommand) list.get(1);

    when(reactorQueryGateway.query(
            TotpQuery.GetTotpByAccountIdQuery.builder().accountId(accountId).build(), Totp.class))
        .thenReturn(
            Mono.just(
                Totp.builder()
                    .accountId(accountId)
                    .iv(createTotpCommand.getIv())
                    .recoveryCode(createTotpCommand.getRecoveryCode())
                    .secret(createTotpCommand.getSecret())
                    .build()));

    webTestClient
        .post()
        .uri(FactorAPI.FACTOR_VERIFY_TOTP_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .body(
            Mono.just(FactorAPI.VerifyTotpFactorRequest.builder().otp("123456").build()),
            FactorAPI.TotpFactorRecoveryRequest.class)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals(
                  AuthException.INVALID_OTP.getEx().code, result.getResponseBody().error.code);
            });
  }
}
