package com.example.auth.device;

import static org.mockito.Mockito.when;

import com.example.auth.common.ClientConfiguration;
import com.example.auth.token.access.JWKSSettings;
import com.example.auth.token.access.JwtTokenFactory;
import com.example.auth.token.access.keys.PrivateSigningKeyRepository;
import com.example.auth.token.access.keys.PublicSigningKeyRepository;
import com.example.auth.token.access.keys.SigningKeyService;
import com.example.auth.token.refresh.RefreshTokenService;
import com.example.spring.web.GlobalErrorHandler;
import java.util.UUID;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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
    })
public class UpdateDeviceAPITest {

  @Autowired private WebTestClient webTestClient;
  @Autowired private DeviceRegistrationConfig deviceRegistrationConfig;
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
  @DisplayName("test update device expect ok")
  void testUpdateDeviceExpectOk() {

    var request = DeviceController.DeviceUpdateRequest.builder().name("random-name").build();

    Mockito.when(
            reactorCommandGateway.send(
                DeviceCommand.UpdateDeviceNameCommand.builder()
                    .id(deviceId)
                    .name(request.getName())
                    .build()))
        .thenReturn(Mono.just(deviceId));

    webTestClient
        .put()
        .uri(DeviceController.DEVICE_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(Mono.just(request), DeviceController.DeviceUpdateRequest.class)
        .header("x-client-code", deviceId + "." + password)
        .exchange()
        .expectStatus()
        .isNoContent();
  }
}
