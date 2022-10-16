package com.example.auth.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.auth.AuthException;
import com.example.auth.account.Account;
import com.example.auth.account.AccountConfig;
import com.example.auth.account.AccountQuery;
import com.example.auth.account.AccountStatus;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.Device;
import com.example.auth.device.DeviceQuery;
import com.example.auth.device.DeviceRegistrationConfig;
import com.example.auth.token.access.JWKSSettings;
import com.example.auth.token.access.JwtTokenFactory;
import com.example.auth.token.access.keys.PrivateSigningKeyRepository;
import com.example.auth.token.access.keys.PublicSigningKey;
import com.example.auth.token.access.keys.PublicSigningKeyRepository;
import com.example.auth.token.access.keys.SigningKeyService;
import com.example.auth.token.refresh.RefreshToken;
import com.example.auth.token.refresh.RefreshTokenCommand;
import com.example.auth.token.refresh.RefreshTokenQuery;
import com.example.auth.token.refresh.RefreshTokenService;
import com.example.security.core.RSAUtil;
import com.example.spring.core.json.ErrorResponse;
import com.example.spring.web.GlobalErrorHandler;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxTest(
    controllers = TokenController.class,
    properties = {"app.request-verification.enabled=true"})
@Import(
    value = {
      TokenController.class,
      JwtTokenFactory.class,
      JWKSSettings.class,
      AccountConfig.class,
      SigningKeyService.class,
      DeviceRegistrationConfig.class,
      ClientConfiguration.class,
      GlobalErrorHandler.class,
      PrivateSigningKeyRepository.class,
    })
class TokenAPITest {

  @Autowired private WebTestClient webTestClient;
  @Autowired private DeviceRegistrationConfig deviceRegistrationConfig;
  @Autowired private AccountConfig accountConfig;
  @Autowired private JWKSSettings jwksSettings;
  @MockBean private ReactorQueryGateway reactorQueryGateway;
  @MockBean private ReactorCommandGateway reactorCommandGateway;
  @MockBean private PublicSigningKeyRepository publicSigningKeyRepository;
  @MockBean private RefreshTokenService refreshTokenService;

  private final String deviceId = UUID.randomUUID().toString();
  private final String password = UUID.randomUUID().toString();

  @BeforeEach
  public void beforeEach() {
    var hash = deviceRegistrationConfig.getHash(password);
    Assertions.assertTrue(deviceRegistrationConfig.verifyHash(password, hash));
    when(reactorQueryGateway.query(
            DeviceQuery.GetDeviceByIdQuery.builder().id(deviceId).build(), Device.class))
        .thenReturn(
            Mono.just(
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
                    .build()));
  }

  @Test
  @DisplayName(
      "test token API when invalid credential should return unauthorized with error code invalid-credential")
  public void testTokenAPIWhenInvalidCredentialShouldReturnUnauthorized() {
    var request =
        TokenController.AccountTokenRequest.builder()
            .email("github4sanjay@gmail.com")
            .password("password")
            .build();

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByEmailQuery.builder().email(request.getEmail()).build(),
            Account.class))
        .thenReturn(
            Mono.just(
                Account.builder()
                    .id(UUID.randomUUID().toString())
                    .password("password")
                    .status(AccountStatus.ACTIVE)
                    .email(request.getEmail())
                    .build()));

    webTestClient
        .post()
        .uri(TokenController.TOKEN_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .body(Mono.just(request), TokenController.AccountTokenRequest.class)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals("invalid-credential", result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName("test token API when valid credential should return tokens")
  public void testTokenAPIWhenValidCredentialShouldReturnTokens() {
    var request =
        TokenController.AccountTokenRequest.builder()
            .email("github4sanjay@gmail.com")
            .password("password")
            .build();

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByEmailQuery.builder().email(request.getEmail()).build(),
            Account.class))
        .thenReturn(
            Mono.just(
                Account.builder()
                    .id(UUID.randomUUID().toString())
                    .password(accountConfig.getHash(request.getPassword()))
                    .status(AccountStatus.ACTIVE)
                    .email(request.getEmail())
                    .build()));
    when(reactorCommandGateway.send(any())).thenReturn(Mono.just(UUID.randomUUID()));
    when(publicSigningKeyRepository.save(any()))
        .thenReturn(PublicSigningKey.builder().id(UUID.randomUUID().toString()).build());

    when(refreshTokenService.on(any(RefreshTokenCommand.GenerateRefreshTokenCommand.class)))
        .thenReturn(UUID.randomUUID().toString());

    webTestClient
        .post()
        .uri(TokenController.TOKEN_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .body(Mono.just(request), TokenController.AccountTokenRequest.class)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(TokenController.TokenResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              var tokenResponse = result.getResponseBody();
              assertNotNull(tokenResponse.getAccessToken());
              assertNotNull(tokenResponse.getRefreshToken());
            });
  }

  @Test
  @DisplayName("test refresh token API when expired token should return 401 expired-token")
  public void testRefreshTokenAPIWhenExpiredTokenShouldReturn401() {

    var request =
        TokenController.RefreshTokenRequest.builder()
            .refreshToken(UUID.randomUUID().toString())
            .build();

    when(refreshTokenService.on(
            RefreshTokenQuery.GetRefreshTokenByDeviceIdAndTokenQuery.builder()
                .token(request.getRefreshToken())
                .deviceId(deviceId)
                .build()))
        .thenThrow(AuthException.EXPIRED_TOKEN.getEx());

    webTestClient
        .post()
        .uri(TokenController.TOKEN_REFRESH_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .body(Mono.just(request), TokenController.RefreshTokenRequest.class)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals("expired-token", result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName("test refresh token API when invalid token should return 401 invalid-token")
  public void testRefreshTokenAPIWhenInvalidTokenShouldReturn401() {

    var request =
        TokenController.RefreshTokenRequest.builder()
            .refreshToken(UUID.randomUUID().toString())
            .build();

    when(refreshTokenService.on(
            RefreshTokenQuery.GetRefreshTokenByDeviceIdAndTokenQuery.builder()
                .token(request.getRefreshToken())
                .deviceId(deviceId)
                .build()))
        .thenThrow(AuthException.INVALID_TOKEN.getEx());

    webTestClient
        .post()
        .uri(TokenController.TOKEN_REFRESH_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .body(Mono.just(request), TokenController.RefreshTokenRequest.class)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals("invalid-token", result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName("test refresh token API when valid token should return 200 with tokens")
  public void testRefreshTokenAPIWhenValidTokenShouldReturnTokens() {

    var request =
        TokenController.RefreshTokenRequest.builder()
            .refreshToken(UUID.randomUUID().toString())
            .build();

    var accountId = UUID.randomUUID().toString();
    when(refreshTokenService.on(
            RefreshTokenQuery.GetRefreshTokenByDeviceIdAndTokenQuery.builder()
                .token(request.getRefreshToken())
                .deviceId(deviceId)
                .build()))
        .thenReturn(
            RefreshToken.builder()
                .amr(Claims.AMR.pwd)
                .token(request.getRefreshToken())
                .deviceId(deviceId)
                .id(UUID.randomUUID().toString())
                .subject(
                    Claims.AccountSubject.builder()
                        .id(accountId)
                        .email("github4sanjay@gmail.com")
                        .build()
                        .get())
                .build());

    when(reactorCommandGateway.send(any())).thenReturn(Mono.just(UUID.randomUUID()));
    when(publicSigningKeyRepository.save(any()))
        .thenReturn(PublicSigningKey.builder().id(UUID.randomUUID().toString()).build());

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByIdQuery.builder().id(accountId).build(), Account.class))
        .thenReturn(
            Mono.just(
                Account.builder()
                    .id(accountId)
                    .hashLength(16)
                    .email("github4sanjay@gmail.com")
                    .saltLength(32)
                    .password("password")
                    .status(AccountStatus.ACTIVE)
                    .build()));

    when(refreshTokenService.on(any(RefreshTokenCommand.RefreshRefreshTokenCommand.class)))
        .thenReturn(UUID.randomUUID().toString());

    webTestClient
        .post()
        .uri(TokenController.TOKEN_REFRESH_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .body(Mono.just(request), TokenController.RefreshTokenRequest.class)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(TokenController.TokenResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              var tokenResponse = result.getResponseBody();
              assertNotNull(tokenResponse.getAccessToken());
              assertNotNull(tokenResponse.getRefreshToken());
            });
  }

  @Test
  @DisplayName("test jwks API should return list of jwks")
  public void testJwksAPIShouldReturnListOfJwk() {

    var keyPair = RSAUtil.generateKeyPair();
    var publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    var publicSigningKey =
        PublicSigningKey.builder()
            .id(UUID.randomUUID().toString())
            .expireAt(
                Instant.now()
                    .plus(jwksSettings.getKeyRotationPeriod())
                    .plus(jwksSettings.getCoolDownPeriod()))
            .publicKey(publicKey)
            .build();

    when(publicSigningKeyRepository.findAllByExpireAtAfter(any()))
        .thenReturn(List.of(publicSigningKey));

    webTestClient
        .get()
        .uri(TokenController.WELL_KNOWN_JWKS_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(TokenController.JwksResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              var jwksResponse = result.getResponseBody();
              var jwks = jwksResponse.getKeys();
              assertEquals(1, jwks.size());
              var jwk = jwks.get(0);
              assertNotNull(jwk.getE());
              assertNotNull(jwk.getKid());
              assertNotNull(jwk.getKty());
              assertNotNull(jwk.getN());
              assertNotNull(jwk.getUse());
            });
  }
}
