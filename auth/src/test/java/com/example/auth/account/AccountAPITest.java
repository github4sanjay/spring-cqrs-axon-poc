package com.example.auth.account;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.auth.AuthException;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.Device;
import com.example.auth.device.DeviceQuery;
import com.example.auth.device.DeviceRegistrationConfig;
import com.example.auth.token.access.JWKSSettings;
import com.example.auth.token.access.JwtTokenFactory;
import com.example.auth.token.access.keys.PrivateSigningKeyRepository;
import com.example.auth.token.access.keys.PublicSigningKeyRepository;
import com.example.auth.token.access.keys.SigningKeyService;
import com.example.spring.core.json.ErrorResponse;
import com.example.spring.web.GlobalErrorHandler;
import java.util.UUID;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.junit.jupiter.api.Assertions;
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
@WebFluxTest(
    controllers = AccountController.class,
    properties = {"app.request-verification.enabled=true"})
@Import(
    value = {
      AccountConfig.class,
      DeviceRegistrationConfig.class,
      ClientConfiguration.class,
      JwtTokenFactory.class,
      JWKSSettings.class,
      SigningKeyService.class,
      PrivateSigningKeyRepository.class,
      GlobalErrorHandler.class,
    })
class AccountAPITest {

  @Autowired private WebTestClient webTestClient;
  @Autowired private DeviceRegistrationConfig deviceRegistrationConfig;
  @MockBean private ReactorQueryGateway reactorQueryGateway;
  @MockBean private ReactorCommandGateway reactorCommandGateway;
  @MockBean private PublicSigningKeyRepository publicSigningKeyRepository;

  @Test
  @DisplayName("test register account api expect status 200 OK")
  void testRegisterAccountExpectOkStatus() {

    var request =
        AccountController.AccountRequest.builder()
            .email("github4sanjay@gmail.com")
            .password("password")
            .build();

    var deviceId = UUID.randomUUID().toString();
    var password = UUID.randomUUID().toString();
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

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByEmailQuery.builder().email(request.getEmail()).build(),
            Account.class))
        .thenReturn(Mono.error(AuthException.ACCOUNT_NOT_FOUND.getEx()));

    when(reactorCommandGateway.send(any())).thenReturn(Mono.just(UUID.randomUUID()));

    when(reactorQueryGateway.subscriptionQuery(any(), any(Class.class)))
        .thenAnswer(
            invocation -> {
              var query = (AccountQuery.GetAccountByIdQuery) invocation.getArguments()[0];
              return Flux.just(Account.builder().id(query.getId().toString()).build());
            });

    webTestClient
        .post()
        .uri(AccountController.ACCOUNT_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .body(Mono.just(request), AccountController.AccountRequest.class)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(AccountController.AccountResponse.class)
        .value(AccountController.AccountResponse::getId, notNullValue());
  }

  @Test
  @DisplayName("test register account api when account creation not enabled expect status 403")
  void testRegisterAccountWhenAccountCreationNotEnabledExpect403() {

    var request =
        AccountController.AccountRequest.builder()
            .email("github4sanjay@gmail.com")
            .password("password")
            .build();

    var deviceId = UUID.randomUUID().toString();
    var password = UUID.randomUUID().toString();
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
                    .client("another-app")
                    .manufacturer("Windows")
                    .os("windows")
                    .model("DFFG-123")
                    .name("Some Name")
                    .build()));

    webTestClient
        .post()
        .uri(AccountController.ACCOUNT_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .body(Mono.just(request), AccountController.AccountRequest.class)
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals(
                  AuthException.ACCOUNT_CREATION_NOT_ENABLED.getEx().getCode(),
                  result.getResponseBody().error.code);
            });
  }

  @Test
  @DisplayName(
      "test register account api when account already exist expect status 403 account-already-exist")
  void testRegisterAccountWhenAccountAlreadyExistExpect403Status() {

    var request =
        AccountController.AccountRequest.builder()
            .email("github4sanjay@gmail.com")
            .password("password")
            .build();

    var deviceId = UUID.randomUUID().toString();
    var password = UUID.randomUUID().toString();
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

    when(reactorQueryGateway.query(
            AccountQuery.GetAccountByEmailQuery.builder().email(request.getEmail()).build(),
            Account.class))
        .thenReturn(Mono.just(Account.builder().id(UUID.randomUUID().toString()).build()));

    webTestClient
        .post()
        .uri(AccountController.ACCOUNT_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-client-code", deviceId + "." + password)
        .body(Mono.just(request), AccountController.AccountRequest.class)
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals("account-already-exist", result.getResponseBody().error.code);
            });
  }
}
