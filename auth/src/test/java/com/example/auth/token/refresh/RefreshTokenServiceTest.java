package com.example.auth.token.refresh;

import static org.junit.jupiter.api.Assertions.*;

import com.example.auth.AuthException;
import com.example.auth.token.Claims;
import com.example.spring.core.exceptions.ApplicationException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
@Import(value = {RefreshTokenService.class})
class RefreshTokenServiceTest {

  @Autowired private RefreshTokenService refreshTokenService;
  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Test
  @DisplayName("test refresh token created event handler should save data in db")
  public void testRefreshTokenCreatedEventHandlerShouldSaveDataInDb() {
    var event =
        RefreshTokenCommand.GenerateRefreshTokenCommand.builder()
            .identifier(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .subject("subject")
            .amr(Claims.AMR.pwd)
            .deviceId(UUID.randomUUID().toString())
            .token(UUID.randomUUID().toString())
            .createdAt(Instant.now())
            .expireAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .refreshChainExpiry(Duration.ofDays(10))
            .build();
    refreshTokenService.on(event);
    var mayBeRefreshToken = refreshTokenRepository.findById(event.getId());
    Assertions.assertTrue(mayBeRefreshToken.isPresent());
    var refreshToken = mayBeRefreshToken.get();
    Assertions.assertEquals(event.getToken(), refreshToken.getToken());
    Assertions.assertEquals(event.getAmr(), refreshToken.getAmr());
    Assertions.assertEquals(event.getSubject(), refreshToken.getSubject());
    Assertions.assertEquals(event.getDeviceId(), refreshToken.getDeviceId());
    Assertions.assertEquals(event.getId(), refreshToken.getId());
  }

  @Test
  @DisplayName(
      "test RefreshTokenRefreshedEvent when refresh token not present should return invalid token exception")
  public void
      testRefreshTokenRefreshedEventWhenRefreshTokenNotPresentShouldReturnInvalidTokenException() {
    var exception =
        Assertions.assertThrows(
            ApplicationException.class,
            () ->
                refreshTokenService.on(
                    RefreshTokenCommand.RefreshRefreshTokenCommand.builder()
                        .identifier(UUID.randomUUID().toString())
                        .deviceId(UUID.randomUUID().toString())
                        .token(UUID.randomUUID().toString())
                        .expireAt(Instant.now().plus(1, ChronoUnit.HOURS))
                        .build()));

    Assertions.assertEquals(AuthException.INVALID_TOKEN.getEx().getCode(), exception.getCode());
  }

  @Test
  @DisplayName(
      "test RefreshTokenRefreshedEvent when refresh token present but expired should return expired token exception")
  public void
      testRefreshTokenRefreshedEventWhenRefreshTokenPresentButExpiredShouldReturnExpiredTokenException() {

    var command =
        RefreshTokenCommand.RefreshRefreshTokenCommand.builder()
            .identifier(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .token(UUID.randomUUID().toString())
            .expireAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .build();

    var refreshToken =
        RefreshToken.builder()
            .amr(Claims.AMR.pwd)
            .token(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .id(command.getId())
            .subject(
                Claims.AccountSubject.builder()
                    .id(UUID.randomUUID().toString())
                    .email("github4sanjay@gmail.com")
                    .build()
                    .get())
            .expireAt(Instant.now().minus(1, ChronoUnit.HOURS))
            .refreshChainExpiry(Duration.ofDays(1))
            .createdAt(Instant.now().minus(2, ChronoUnit.HOURS))
            .build();

    refreshTokenRepository.save(refreshToken);

    var exception =
        Assertions.assertThrows(ApplicationException.class, () -> refreshTokenService.on(command));

    Assertions.assertEquals(AuthException.EXPIRED_TOKEN.getEx().getCode(), exception.getCode());
  }

  @Test
  @DisplayName(
      "test RefreshTokenRefreshedEvent when refresh token present but expired chain expiry should return expired token exception")
  public void
      testRefreshTokenRefreshedEventWhenRefreshTokenPresentButExpiredChainExpiryShouldReturnExpiredTokenException() {

    var command =
        RefreshTokenCommand.RefreshRefreshTokenCommand.builder()
            .identifier(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .token(UUID.randomUUID().toString())
            .expireAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .build();

    var refreshToken =
        RefreshToken.builder()
            .amr(Claims.AMR.pwd)
            .token(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .id(command.getId())
            .subject(
                Claims.AccountSubject.builder()
                    .id(UUID.randomUUID().toString())
                    .email("github4sanjay@gmail.com")
                    .build()
                    .get())
            .expireAt(Instant.now().minus(1, ChronoUnit.HOURS))
            .refreshChainExpiry(Duration.ofDays(1))
            .createdAt(Instant.now().minus(2, ChronoUnit.DAYS))
            .build();

    refreshTokenRepository.save(refreshToken);

    var exception =
        Assertions.assertThrows(ApplicationException.class, () -> refreshTokenService.on(command));

    Assertions.assertEquals(AuthException.EXPIRED_TOKEN.getEx().getCode(), exception.getCode());
  }

  @Test
  @DisplayName("test RefreshTokenRefreshedEvent when refresh token present should update the token")
  public void testRefreshTokenRefreshedEventWhenRefreshTokenPresentShouldUpdateTheToken() {

    var command =
        RefreshTokenCommand.RefreshRefreshTokenCommand.builder()
            .identifier(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .token(UUID.randomUUID().toString())
            .expireAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .build();

    var refreshToken =
        RefreshToken.builder()
            .amr(Claims.AMR.pwd)
            .token(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .id(command.getId())
            .subject(
                Claims.AccountSubject.builder()
                    .id(UUID.randomUUID().toString())
                    .email("github4sanjay@gmail.com")
                    .build()
                    .get())
            .expireAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .refreshChainExpiry(Duration.ofDays(1))
            .createdAt(Instant.now())
            .build();
    refreshTokenRepository.save(refreshToken);

    refreshTokenService.on(command);

    var mayBeUpdatedRefreshToken = refreshTokenRepository.findById(command.getId());
    assertTrue(mayBeUpdatedRefreshToken.isPresent());
    var updatedRefreshToken = mayBeUpdatedRefreshToken.get();
    assertEquals(command.getToken(), updatedRefreshToken.getToken());
  }

  @Test
  @DisplayName(
      "test GetRefreshTokenByDeviceIdAndTokenQuery when refresh token is valid should return refresh token")
  public void
      testGetRefreshTokenByDeviceIdAndTokenQueryWhenRefreshTokenNotPresentShouldReturnRefreshToken() {
    var refreshToken =
        RefreshToken.builder()
            .amr(Claims.AMR.pwd)
            .token(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .id(UUID.randomUUID().toString())
            .subject(
                Claims.AccountSubject.builder()
                    .id(UUID.randomUUID().toString())
                    .email("github4sanjay@gmail.com")
                    .build()
                    .get())
            .expireAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .refreshChainExpiry(Duration.ofDays(1))
            .createdAt(Instant.now())
            .build();
    refreshTokenRepository.save(refreshToken);

    var getRefreshToken =
        refreshTokenService.on(
            RefreshTokenQuery.GetRefreshTokenByDeviceIdAndTokenQuery.builder()
                .token(refreshToken.getToken())
                .deviceId(refreshToken.getDeviceId())
                .build());

    assertNotNull(getRefreshToken);
    assertEquals(refreshToken.getId(), getRefreshToken.getId());
  }

  @Test
  @DisplayName("test RefreshTokenDisabledEvent expect RefreshToken to be deleted from DB")
  public void testRefreshTokenDisabledEventExpectRefreshTokenToBeDeletedFromDB() {

    var accountId = UUID.randomUUID().toString();

    var command =
        RefreshTokenCommand.DisableRefreshTokenCommand.builder()
            .identifier(accountId)
            .deviceId(UUID.randomUUID().toString())
            .build();

    var refreshToken =
        RefreshToken.builder()
            .amr(Claims.AMR.pwd)
            .token(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .id(command.getId())
            .subject(
                Claims.AccountSubject.builder()
                    .id(accountId)
                    .email("github4sanjay@gmail.com")
                    .build()
                    .get())
            .expireAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .refreshChainExpiry(Duration.ofDays(1))
            .createdAt(Instant.now())
            .build();

    refreshTokenRepository.save(refreshToken);

    refreshTokenService.on(command);

    assertFalse(refreshTokenRepository.findById(refreshToken.getId()).isPresent());
  }
}
