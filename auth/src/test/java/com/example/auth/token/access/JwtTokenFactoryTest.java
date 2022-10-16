package com.example.auth.token.access;

import static org.junit.jupiter.api.Assertions.*;

import com.auth0.jwt.JWT;
import com.example.auth.AuthException;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.Device;
import com.example.auth.device.DeviceRegistrationConfig;
import com.example.auth.token.Claims;
import com.example.auth.token.access.keys.PrivateSigningKeyRepository;
import com.example.auth.token.access.keys.SigningKeyService;
import com.example.spring.core.exceptions.ApplicationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@Import(
    value = {
      JwtTokenFactory.class,
      JWKSSettings.class,
      SigningKeyService.class,
      ClientConfiguration.class,
      PrivateSigningKeyRepository.class
    })
class JwtTokenFactoryTest {

  @Autowired private JwtTokenFactory jwtTokenFactory;
  @Autowired private SigningKeyService signingKeyService;
  @Autowired private JWKSSettings jwksSettings;

  @Test
  @DisplayName("test decode token when access token is valid should return expected claims")
  public void testDecodeTokenWhenAccessTokenIsValidShouldReturnExpectedClaims() {
    var accountId = UUID.randomUUID().toString();
    var accountSubject =
        Claims.AccountSubject.builder().id(accountId).email("github4sanjay@gmail.com").build();
    var device =
        Device.builder()
            .publicKey("publicKey")
            .id(UUID.randomUUID().toString())
            .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
            .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
            .hash("some hash")
            .client("web-app")
            .manufacturer("Windows")
            .os("windows")
            .model("DFFG-123")
            .name("Some Name")
            .build();

    var expectedClaims = new Claims(accountSubject, device, Claims.AMR.pwd);
    var accessToken =
        jwtTokenFactory.createAccessJwtToken(expectedClaims, "web-app", accountId.toString());
    var actualClaims = jwtTokenFactory.decodeToken(accessToken.getToken());

    assertEquals(expectedClaims.getSubject().get(), actualClaims.getSubject().get());
    assertEquals(expectedClaims.getAud(), actualClaims.getAud());
    assertEquals(expectedClaims.getAmr(), actualClaims.getAmr());
  }

  @Test
  @DisplayName(
      "test decode token when access token is invalid should return invalid token exception")
  public void testDecodeTokenWhenAccessTokenIsValidShouldReturnInvalidTokenException() {

    var exception =
        Assertions.assertThrows(
            ApplicationException.class,
            () -> jwtTokenFactory.decodeToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"));
    Assertions.assertEquals(AuthException.INVALID_TOKEN.getEx().getCode(), exception.getCode());
  }

  @Test
  @DisplayName(
      "test decode token when access token is expired should return expired token exception")
  public void testDecodeTokenWhenAccessTokenIsExpiredShouldReturnExpiredTokenException() {

    var accountId = UUID.randomUUID().toString();
    var accountSubject =
        Claims.AccountSubject.builder().id(accountId).email("github4sanjay@gmail.com").build();
    var device =
        Device.builder()
            .publicKey("publicKey")
            .id(UUID.randomUUID().toString())
            .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
            .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
            .hash("some hash")
            .client("web-app")
            .manufacturer("Windows")
            .os("windows")
            .model("DFFG-123")
            .name("Some Name")
            .build();

    var claims = new Claims(accountSubject, device, Claims.AMR.pwd);

    var signingKey = signingKeyService.getCurrentSigningKey();

    var tokenBuilder =
        JWT.create()
            .withKeyId(signingKey.getId())
            .withIssuer(jwksSettings.getIssuer())
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)))
            .withAudience(claims.getAud())
            .withSubject(claims.getSubject().get())
            .withClaim("amr", List.of(claims.getAmr().name()));

    var token = tokenBuilder.sign(signingKey.getSignAlgorithm());

    var exception =
        Assertions.assertThrows(
            ApplicationException.class, () -> jwtTokenFactory.decodeToken(token));
    Assertions.assertEquals(AuthException.EXPIRED_TOKEN.getEx().getCode(), exception.getCode());
  }
}
