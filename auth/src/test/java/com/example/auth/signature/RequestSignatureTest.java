package com.example.auth.signature;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.auth.common.ClientConfiguration;
import com.example.auth.common.OpenApi;
import com.example.auth.device.Device;
import com.example.auth.device.DeviceController;
import com.example.auth.device.DeviceQuery;
import com.example.auth.device.DeviceRegistrationConfig;
import com.example.auth.device.trust.UserDeviceCommand;
import com.example.auth.token.Claims;
import com.example.auth.token.ClaimsMethodArgumentResolver;
import com.example.auth.token.access.JWKSSettings;
import com.example.auth.token.access.JwtTokenFactory;
import com.example.auth.token.access.keys.PrivateSigningKeyRepository;
import com.example.auth.token.access.keys.PublicSigningKey;
import com.example.auth.token.access.keys.PublicSigningKeyRepository;
import com.example.auth.token.access.keys.SigningKeyService;
import com.example.auth.token.refresh.RefreshTokenService;
import com.example.security.core.RSAUtil;
import com.example.spring.core.json.ErrorResponse;
import com.example.spring.web.GlobalErrorHandler;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
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

@Slf4j
@ExtendWith(SpringExtension.class)
@WebFluxTest(
    controllers = DeviceController.class,
    properties = {
      "app.request-verification.enabled=true",
      "auth.client-config.web-app.requestSigningEnabled=true"
    })
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
class RequestSignatureTest {

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
  private String PRIVATE_KEY;

  @BeforeEach
  public void beforeEach() {
    privateSigningKeyRepository.clearKeys();
    var hash = deviceRegistrationConfig.getHash(password);
    Assertions.assertTrue(deviceRegistrationConfig.verifyHash(password, hash));

    var keyPairGenerator = RSAUtil.generateKeyPair();
    PRIVATE_KEY = Base64.getEncoder().encodeToString(keyPairGenerator.getPrivate().getEncoded());
    var publicKey = Base64.getEncoder().encodeToString(keyPairGenerator.getPublic().getEncoded());

    var device =
        Device.builder()
            .publicKey(publicKey)
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
  @DisplayName("test request signature when valid expect success")
  void testRequestSignatureWhenValidExpectSuccess()
      throws NoSuchAlgorithmException, SignatureException, InvalidKeySpecException,
          InvalidKeyException {

    when(reactorCommandGateway.send(
            UserDeviceCommand.TrustUserDeviceCommand.builder()
                .accountId(accountId)
                .deviceId(deviceId)
                .build()))
        .thenReturn(Mono.just(deviceId));

    var clientCode = deviceId + "." + password;
    var authorization = "Bearer " + ACCESS_TOKEN;
    var signature = new RequestSignature(clientCode, authorization, null, null);
    webTestClient
        .post()
        .uri(DeviceController.DEVICE_TRUST_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(OpenApi.CLIENT_CODE, clientCode)
        .header(OpenApi.JWT, authorization)
        .header(OpenApi.SIGNATURE_HEADER, signature.sign(PRIVATE_KEY))
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  @Test
  @DisplayName("test request signature when invalid expect invalid-signature error")
  void testRequestSignatureWhenInvalidExpect401()
      throws NoSuchAlgorithmException, SignatureException, InvalidKeySpecException,
          InvalidKeyException {

    when(reactorCommandGateway.send(
            UserDeviceCommand.TrustUserDeviceCommand.builder()
                .accountId(accountId)
                .deviceId(deviceId)
                .build()))
        .thenReturn(Mono.just(deviceId));

    var clientCode = deviceId + "." + password;
    var authorization = "Bearer " + ACCESS_TOKEN;
    var signature = new RequestSignature(clientCode, authorization, "something", null);

    webTestClient
        .post()
        .uri(DeviceController.DEVICE_TRUST_API)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(OpenApi.CLIENT_CODE, clientCode)
        .header(OpenApi.JWT, authorization)
        .header(OpenApi.SIGNATURE_HEADER, signature.sign(PRIVATE_KEY))
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody(ErrorResponse.class)
        .consumeWith(
            result -> {
              assertNotNull(result.getResponseBody());
              assertEquals("invalid-signature", result.getResponseBody().error.code);
            });
  }
}
