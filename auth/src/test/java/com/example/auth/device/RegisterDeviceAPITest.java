package com.example.auth.device;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = DeviceController.class)
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
class RegisterDeviceAPITest {

  @Autowired private WebTestClient webTestClient;
  @MockBean private ReactorQueryGateway reactorQueryGateway;
  @MockBean private ReactorCommandGateway reactorCommandGateway;
  @MockBean private PublicSigningKeyRepository publicSigningKeyRepository;
  @MockBean private RefreshTokenService refreshTokenService;

  @Test
  @DisplayName("test register device api expect status 200 OK")
  void testRegisterDeviceExpectOkStatus() {

    var request =
        DeviceController.DeviceRequest.builder()
            .client("web-app")
            .manufacturer("Windows")
            .os("windows")
            .model("DFFG-123")
            .name("Some Name")
            .build();

    when(reactorCommandGateway.send(any())).thenReturn(Mono.just(UUID.randomUUID()));
    when(reactorQueryGateway.subscriptionQuery(any(), any(Class.class)))
        .thenReturn(Flux.just(Device.builder().id(UUID.randomUUID().toString()).build()));

    webTestClient
        .post()
        .uri(DeviceController.DEVICE_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(Mono.just(request), DeviceController.DeviceRequest.class)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DeviceController.DeviceRegistrationResponse.class)
        .value(DeviceController.DeviceRegistrationResponse::getId, notNullValue())
        .value(DeviceController.DeviceRegistrationResponse::getKey, notNullValue());
  }
}
