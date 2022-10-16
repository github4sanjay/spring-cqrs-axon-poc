package com.example.auth.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.auth.AuthException;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.trust.UserDeviceCommand;
import com.example.auth.token.Claims;
import com.example.auth.token.ClaimsMethodArgumentResolver;
import com.example.auth.token.access.JWKSSettings;
import com.example.auth.token.access.JwtTokenFactory;
import com.example.auth.token.access.keys.PrivateSigningKeyRepository;
import com.example.auth.token.access.keys.PublicSigningKey;
import com.example.auth.token.access.keys.PublicSigningKeyRepository;
import com.example.auth.token.access.keys.SigningKeyService;
import com.example.auth.token.refresh.RefreshTokenCommand;
import com.example.auth.token.refresh.RefreshTokenService;
import com.example.spring.core.exceptions.CoreExceptions;
import com.example.spring.core.json.ErrorResponse;
import com.example.spring.web.GlobalErrorHandler;
import java.util.Optional;
import java.util.UUID;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    controllers = DeviceController.class,
    properties = {"app.request-verification.enabled=true"})
@Import(
    value = {
      DeviceRegistrationConfig.class,
      ClientConfiguration.class,
      JwtTokenFactory.class,
      JWKSSettings.class,
      SigningKeyService.class,
      PrivateSigningKeyRepository.class,
      GlobalErrorHandler.class,
      ClaimsMethodArgumentResolver.class
    })
public class TrustDeviceAPITest {

  @Autowired private WebTestClient webTestClient;
  @Autowired private DeviceRegistrationConfig deviceRegistrationConfig;
  @Autowired private JwtTokenFactory jwtTokenFactory;
  @Autowired private PrivateSigningKeyRepository privateSigningKeyRepository;
  @MockBean private ReactorQueryGateway reactorQueryGateway;
  @MockBean private ReactorCommandGateway reactorCommandGateway;
  @MockBean private PublicSigningKeyRepository publicSigningKeyRepository;
  @MockBean private RefreshTokenService refreshTokenService;

  private final String deviceId = UUID.randomUUID().toString();
  private final String password = UUID.randomUUID().toString();
  private final String accountId = UUID.randomUUID().toString();

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
    var subject =
        Claims.AccountSubject.builder().id(accountId).email("github4sanjay@gmail.com").build();

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
  @DisplayName("test trust device when valid authorization token expect 204")
  void testTrustDeviceWhenValidAuthorizationTokenExpect204() {

    when(reactorCommandGateway.send(
            UserDeviceCommand.TrustUserDeviceCommand.builder()
                .accountId(accountId)
                .deviceId(deviceId)
                .build()))
        .thenReturn(Mono.just(deviceId));

    webTestClient
        .post()
        .uri(DeviceController.DEVICE_TRUST_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  @Test
  @DisplayName("test trust device when invalid authorization token expect 401 invalid-token")
  void testTrustDeviceWhenValidAuthorizationTokenExpect401() {

    webTestClient
        .post()
        .uri(DeviceController.DEVICE_TRUST_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer sdfdsfsdf")
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
  @DisplayName("test delete trust device when invalid authorization token expect 401 invalid-token")
  void testDeleteTrustDeviceWhenInvalidAuthorizationTokenExpect401() {

    webTestClient
        .delete()
        .uri("/api/v1/devices/" + deviceId + "/trust")
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer sdfdsfsdf")
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
  @DisplayName("test delete trust device when invalid device id expect 404 device-not-found")
  void testDeleteTrustDeviceWhenInvalidDeviceIdExpect404() {

    when(reactorCommandGateway.send(
            UserDeviceCommand.RemoveTrustUserDeviceCommand.builder()
                .accountId(accountId)
                .deviceId(deviceId)
                .build()))
        .thenReturn(Mono.error(CoreExceptions.AGGREGATE_NOT_FOUND.getEx()));

    webTestClient
        .delete()
        .uri("/api/v1/devices/" + deviceId + "/trust")
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
                  AuthException.DEVICE_NOT_FOUND.getEx().code, result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName("test delete trust device when valid authorization token expect 204")
  void testDeleteTrustDeviceWhenValidAuthorizationTokenExpect204() {

    when(reactorCommandGateway.send(
            UserDeviceCommand.RemoveTrustUserDeviceCommand.builder()
                .accountId(accountId)
                .deviceId(deviceId)
                .build()))
        .thenReturn(Mono.just(UUID.randomUUID().toString()));

    when(refreshTokenService.on(
            RefreshTokenCommand.DisableRefreshTokenCommand.builder()
                .identifier(accountId)
                .deviceId(deviceId)
                .build()))
        .thenReturn(UUID.randomUUID().toString());

    webTestClient
        .delete()
        .uri("/api/v1/devices/" + deviceId + "/trust")
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isNoContent();
  }
}
