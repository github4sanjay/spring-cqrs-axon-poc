package com.example.auth.token;

import com.example.auth.AuthException;
import com.example.auth.account.Account;
import com.example.auth.account.AccountConfig;
import com.example.auth.account.AccountQuery;
import com.example.auth.account.AccountStatus;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.Device;
import com.example.auth.device.trust.UserDeviceCommand;
import com.example.auth.token.access.JwtTokenFactory;
import com.example.auth.token.access.keys.SigningKeyService;
import com.example.auth.token.refresh.RefreshTokenCommand;
import com.example.auth.token.refresh.RefreshTokenQuery;
import com.example.auth.token.refresh.RefreshTokenService;
import com.example.spring.core.exceptions.CoreExceptions;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TokenController implements TokenAPI {

  private final JwtTokenFactory tokenFactory;
  private final ReactorQueryGateway reactorQueryGateway;
  private final ReactorCommandGateway reactorCommandGateway;
  private final AccountConfig accountConfig;
  private final ClientConfiguration clientConfiguration;
  private final SigningKeyService signingKeyService;
  private final RefreshTokenService refreshTokenService;

  @Override
  public Mono<TokenResponse> accountToken(AccountTokenRequest request, Device device) {
    var jwtConfig = clientConfiguration.getCurrentClient(device.getClient()).getJwt();
    return reactorQueryGateway
        .query(
            AccountQuery.GetAccountByEmailQuery.builder().email(request.getEmail()).build(),
            Account.class)
        .map(
            account -> {
              if (accountConfig.verifyHash(request.getPassword(), account.getPassword())) {
                return account;
              } else {
                throw AuthException.INVALID_CREDENTIAL.getEx();
              }
            })
        .flatMap(
            account ->
                reactorCommandGateway
                    .send(
                        UserDeviceCommand.RegisterUserDeviceCommand.builder()
                            .deviceId(device.getId())
                            .accountId(account.getId())
                            .lastLoginAt(Instant.now())
                            .build())
                    .thenReturn(account))
        .flatMap(
            account -> {
              var subject =
                  Claims.AccountSubject.builder()
                      .email(account.getEmail())
                      .id(account.getId())
                      .build();
              var claims = new Claims(subject, device, Claims.AMR.pwd);
              var accessJwtToken =
                  tokenFactory.createAccessJwtToken(claims, device.getClient(), account.getId());
              var id = UUID.randomUUID().toString();
              var command =
                  RefreshTokenCommand.GenerateRefreshTokenCommand.builder()
                      .identifier(account.getId())
                      .subject(claims.getSubject().get())
                      .amr(claims.getAmr())
                      .deviceId(device.getId())
                      .token(id)
                      .createdAt(Instant.now())
                      .expireAt(
                          Instant.now()
                              .plus(getRefreshTokenExpiry(claims.getAmr(), device.getClient())))
                      .refreshChainExpiry(jwtConfig.getRefreshChainExpiry())
                      .build();
              return Mono.zip(
                  Mono.just(refreshTokenService.on(command)),
                  Mono.just(accessJwtToken.getToken()),
                  Mono.just(id));
            })
        .map(
            objects ->
                TokenResponse.builder()
                    .accessToken(objects.getT2())
                    .refreshToken(objects.getT3())
                    .build());
  }

  @Override
  public Mono<TokenResponse> refreshToken(RefreshTokenRequest request, Device device) {
    return Mono.just(
            refreshTokenService.on(
                RefreshTokenQuery.GetRefreshTokenByDeviceIdAndTokenQuery.builder()
                    .token(request.getRefreshToken())
                    .deviceId(device.getId())
                    .build()))
        .flatMap(
            refreshToken -> {
              var claims =
                  Claims.getClaims(refreshToken.getSubject(), refreshToken.getAmr()).toBuilder()
                      .aud(device.getClient())
                      .build();
              return Mono.zip(Mono.just(claims), getIdentifier(claims.getSubject()));
            })
        .flatMap(
            objects -> {
              var claims = objects.getT1();
              var id = objects.getT2();
              var accessJwtToken =
                  tokenFactory.createAccessJwtToken(claims, device.getClient(), id);
              var newRefreshToken = UUID.randomUUID().toString();
              return Mono.zip(
                  Mono.just(
                      refreshTokenService.on(
                          RefreshTokenCommand.RefreshRefreshTokenCommand.builder()
                              .identifier(id)
                              .deviceId(device.getId())
                              .token(newRefreshToken)
                              .expireAt(
                                  Instant.now()
                                      .plus(
                                          getRefreshTokenExpiry(
                                              claims.getAmr(), device.getClient())))
                              .build())),
                  Mono.just(accessJwtToken.getToken()),
                  Mono.just(newRefreshToken));
            })
        .map(
            objects ->
                TokenResponse.builder()
                    .accessToken(objects.getT2())
                    .refreshToken(objects.getT3())
                    .build());
  }

  @Override
  public Mono<JwksResponse> jwks() {
    return Mono.just(
        JwksResponse.builder()
            .keys(
                signingKeyService
                    .getKeys()
                    .map(
                        signingKey -> {
                          var rsaKey = signingKey.toPublicJWK();
                          return Jwk.builder()
                              .e(rsaKey.getPublicExponent().toString())
                              .n(rsaKey.getModulus().toString())
                              .kty(rsaKey.getKeyType().getValue())
                              .use(rsaKey.getKeyUse().getValue())
                              .kid(rsaKey.getKeyID())
                              .build();
                        })
                    .collect(Collectors.toList()))
            .build());
  }

  private Mono<String> getIdentifier(Claims.Subject subject) {
    if (subject instanceof Claims.AccountSubject s) {
      return reactorQueryGateway
          .query(AccountQuery.GetAccountByIdQuery.builder().id(s.getId()).build(), Account.class)
          .map(
              account -> {
                if (account.getStatus() == AccountStatus.INACTIVE) {
                  throw AuthException.ACCOUNT_INACTIVE_STATUS.getEx();
                }
                return account.getId();
              });
    } else if (subject instanceof Claims.EMAILSubject e) {
      return Mono.just(e.getEmail());
    } else if (subject instanceof Claims.PhoneNumberSubject m) {
      return Mono.just(m.getPhoneNumber());
    } else {
      throw CoreExceptions.INTERNAL_SERVER_ERROR.getEx();
    }
  }

  private Duration getRefreshTokenExpiry(Claims.AMR amr, String client) {
    var jwtConfig = clientConfiguration.getCurrentClient(client).getJwt();
    var refreshTokenExpiryConfig = jwtConfig.getRefreshTokenExpiry();
    return switch (amr) {
      case bio -> refreshTokenExpiryConfig.getBio();
      case otp -> refreshTokenExpiryConfig.getOtp();
      case pwd -> refreshTokenExpiryConfig.getPwd();
      case mfa -> refreshTokenExpiryConfig.getMfa();
      case net -> refreshTokenExpiryConfig.getNet();
    };
  }
}
