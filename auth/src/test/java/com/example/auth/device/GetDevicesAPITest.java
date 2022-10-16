package com.example.auth.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.trust.UserDevice;
import com.example.auth.device.trust.UserDeviceQuery;
import com.example.auth.token.Claims;
import com.example.auth.token.ClaimsMethodArgumentResolver;
import com.example.auth.token.access.JWKSSettings;
import com.example.auth.token.access.JwtTokenFactory;
import com.example.auth.token.access.keys.PrivateSigningKeyRepository;
import com.example.auth.token.access.keys.PublicSigningKey;
import com.example.auth.token.access.keys.PublicSigningKeyRepository;
import com.example.auth.token.access.keys.SigningKeyService;
import com.example.auth.token.refresh.RefreshTokenService;
import com.example.spring.web.GlobalErrorHandler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
public class GetDevicesAPITest {

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
  void testGetDevicesAPIExpect200() {
    var deviceId1 = UUID.randomUUID().toString();
    var deviceId2 = UUID.randomUUID().toString();
    when(reactorQueryGateway.query(
            UserDeviceQuery.GetUserDeviceByAccountIdQuery.builder().accountId(accountId).build(),
            ResponseTypes.multipleInstancesOf(UserDevice.class)))
        .thenReturn(
            Mono.just(
                List.of(
                    UserDevice.builder()
                        .id(UUID.randomUUID().toString())
                        .deviceId(deviceId1)
                        .accountId(accountId)
                        .lastLoginAt(Instant.now())
                        .trusted(true)
                        .build(),
                    UserDevice.builder()
                        .id(UUID.randomUUID().toString())
                        .deviceId(deviceId2)
                        .accountId(accountId)
                        .lastLoginAt(Instant.now())
                        .trusted(true)
                        .build())));

    when(reactorQueryGateway.query(
            DeviceQuery.GetDevicesByIdsQuery.builder().ids(List.of(deviceId1, deviceId2)).build(),
            ResponseTypes.multipleInstancesOf(Device.class)))
        .thenReturn(
            Mono.just(
                List.of(
                    Device.builder()
                        .publicKey("publicKey")
                        .id(deviceId1)
                        .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
                        .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
                        .hash("some-hash")
                        .client("web-app")
                        .manufacturer("Windows")
                        .os("windows")
                        .model("DFFG-123")
                        .name("Some Name")
                        .build(),
                    Device.builder()
                        .publicKey("publicKey")
                        .id(deviceId2)
                        .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
                        .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
                        .hash("some-hash")
                        .client("web-app")
                        .manufacturer("Windows")
                        .os("windows")
                        .model("DFFG-123")
                        .name("Some Name")
                        .build())));

    webTestClient
        .get()
        .uri(DeviceController.DEVICE_API)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(DeviceController.DeviceResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              var deviceResponseList = result.getResponseBody();
              assertEquals(2, deviceResponseList.size());
            });
  }

  @Test
  @DisplayName("test get devices API with filter trusted true expect200")
  void testGetDevicesAPIWithFilterTrustedTrueExpect200() {
    var deviceId1 = UUID.randomUUID().toString();
    var deviceId2 = UUID.randomUUID().toString();
    when(reactorQueryGateway.query(
            UserDeviceQuery.GetUserDeviceByAccountIdQuery.builder().accountId(accountId).build(),
            ResponseTypes.multipleInstancesOf(UserDevice.class)))
        .thenReturn(
            Mono.just(
                List.of(
                    UserDevice.builder()
                        .id(UUID.randomUUID().toString())
                        .deviceId(deviceId1)
                        .accountId(accountId)
                        .lastLoginAt(Instant.now())
                        .trusted(true)
                        .build(),
                    UserDevice.builder()
                        .id(UUID.randomUUID().toString())
                        .deviceId(deviceId2)
                        .accountId(accountId)
                        .lastLoginAt(Instant.now())
                        .trusted(false)
                        .build())));

    when(reactorQueryGateway.query(
            DeviceQuery.GetDevicesByIdsQuery.builder().ids(List.of(deviceId1, deviceId2)).build(),
            ResponseTypes.multipleInstancesOf(Device.class)))
        .thenReturn(
            Mono.just(
                List.of(
                    Device.builder()
                        .publicKey("publicKey")
                        .id(deviceId1)
                        .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
                        .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
                        .hash("some-hash")
                        .client("web-app")
                        .manufacturer("Windows")
                        .os("windows")
                        .model("DFFG-123")
                        .name("Some Name")
                        .build())));

    webTestClient
        .get()
        .uri(DeviceController.DEVICE_API)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .header("Authorization", "Bearer " + ACCESS_TOKEN)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(DeviceController.DeviceResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              var deviceResponseList = result.getResponseBody();
              assertEquals(1, deviceResponseList.size());
            });
  }
}
