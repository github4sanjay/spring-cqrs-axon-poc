package com.example.auth.factors;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

import com.example.api.otp.OtpCommand;
import com.example.api.otp.OtpOptions;
import com.example.api.otp.SendOtpOutcome;
import com.example.api.otp.VerifyOtpOutcome;
import com.example.auth.AuthException;
import com.example.auth.account.Account;
import com.example.auth.account.AccountQuery;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.Device;
import com.example.auth.factors.totp.Totp;
import com.example.auth.factors.totp.TotpCommand;
import com.example.auth.factors.totp.TotpQuery;
import com.example.auth.token.Claims;
import com.example.security.core.AESUtil;
import com.example.spring.core.exceptions.ApplicationException;
import com.example.spring.core.exceptions.CoreExceptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import java.util.Arrays;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FactorController implements FactorAPI {
  private final SecretGenerator secretGenerator;
  private final QrDataFactory qrDataFactory;
  private final QrGenerator qrGenerator;
  private final RecoveryCodeGenerator recoveryCodeGenerator;
  private final CodeVerifier verifier;
  private final ReactorQueryGateway queryGateway;
  private final ReactorCommandGateway commandGateway;
  private final ClientConfiguration clientConfiguration;
  private final SpringTemplateEngine htmlTemplateEngine;
  private final ObjectMapper objectMapper;

  @Override
  public Mono<ChallengeEmailFactorResponse> createEmailFactor(Claims claims, Device device) {
    var accountSubject = claims.getAccountSubject();
    var clientConfig = clientConfiguration.getCurrentClient(device.getClient());
    var factor = clientConfig.getFactors();
    var emailFactor = factor.getEmail();
    final var ctx = new Context();
    ctx.setVariable("expiry", factor.getExpiry().toMinutes());
    final var htmlContent = this.htmlTemplateEngine.process(emailFactor.getTemplate(), ctx);
    return queryGateway
        .query(
            AccountQuery.GetAccountByIdQuery.builder().id(accountSubject.getId()).build(),
            Account.class)
        .flatMap(
            account ->
                commandGateway
                    .sendAll(
                        Flux.just(
                            FactorCommand.CreateFactorCommand.builder()
                                .factorType(FactorType.email)
                                .accountId(account.getId())
                                .build(),
                            createSendEmailOtpCommand(
                                clientConfig, factor, emailFactor, htmlContent, account)))
                    .collectList())
        .flatMap(
            sendOtpOutcomeObj -> {
              var sendOtpOutcome = (SendOtpOutcome) sendOtpOutcomeObj.get(1);
              return createChallengeEmailFactorResponse(sendOtpOutcome);
            });
  }

  private OtpCommand.SendEmailOtpCommand createSendEmailOtpCommand(
      ClientConfiguration.Client clientConfig,
      ClientConfiguration.Factors factor,
      ClientConfiguration.Email emailFactor,
      String htmlContent,
      Account account) {
    return OtpCommand.SendEmailOtpCommand.builder()
        .reference(String.format("%s/%s", factor.getReference(), account.getEmail()))
        .toEmail(account.getEmail())
        .body(htmlContent)
        .fromEmail(emailFactor.getFrom())
        .subject(emailFactor.getSubject())
        .options(
            OtpOptions.builder()
                .rateLimitExpiry((int) factor.getDurationForMaxAllowedOTP().toSeconds())
                .state(
                    OtpState.builder()
                        .factorType(FactorType.email)
                        .accountId(account.getId())
                        .build()
                        .get(objectMapper))
                .rateLimitCount(factor.getMaxAllowedOTP())
                .resendAfter((int) factor.getResendAfter().getSeconds())
                .expiration((int) factor.getExpiry().getSeconds())
                .profile(clientConfig.getMessageProfile())
                .verifyLimitCount(factor.getMaxAttemptForVerification())
                .build())
        .build();
  }

  @NotNull
  private static Mono<ChallengeEmailFactorResponse> createChallengeEmailFactorResponse(
      SendOtpOutcome sendOtpOutcome) {
    return switch (sendOtpOutcome.getResult()) {
      case Ok -> Mono.just(
          ChallengeEmailFactorResponse.builder()
              .challengeToken(sendOtpOutcome.getToken())
              .retryAfter(sendOtpOutcome.getRetryAfter())
              .build());
      case BlockedEarlyRequest -> Mono.error(
          AuthException.TOO_EARLY_OTP_REQUEST(sendOtpOutcome.getRetryAfter()));
      case BlockedTooManyRequests -> Mono.error(
          AuthException.TOO_MANY_OTP_REQUEST(sendOtpOutcome.getRetryAfter()));
    };
  }

  @Override
  public Mono<ChallengeEmailFactorResponse> challengeEmailFactor(Claims claims, Device device) {
    var clientConfig = clientConfiguration.getCurrentClient(device.getClient());
    var factorConfig = clientConfig.getFactors();
    var emailFactor = factorConfig.getEmail();
    var accountSubject = claims.getAccountSubject();
    final var ctx = new Context();
    ctx.setVariable("expiry", factorConfig.getExpiry().toMinutes());
    final var htmlContent = this.htmlTemplateEngine.process(emailFactor.getTemplate(), ctx);
    return queryGateway
        .query(
            FactorQuery.GetFactorByAccountIdAndFactorTypeQuery.builder()
                .factorType(FactorType.email)
                .accountId(accountSubject.getId())
                .build(),
            Factor.class)
        .flatMap(
            factor -> {
              if (!factor.getEnabled()) {
                return Mono.error(AuthException.FACTOR_NOT_FOUND.getEx());
              }
              return queryGateway.query(
                  AccountQuery.GetAccountByIdQuery.builder().id(accountSubject.getId()).build(),
                  Account.class);
            })
        .flatMap(
            account ->
                commandGateway.send(
                    createSendEmailOtpCommand(
                        clientConfig, factorConfig, emailFactor, htmlContent, account)))
        .flatMap(
            sendOtpOutcomeObj -> {
              var sendOtpOutcome = (SendOtpOutcome) sendOtpOutcomeObj;
              return createChallengeEmailFactorResponse(sendOtpOutcome);
            });
  }

  @Override
  public Mono<ResponseEntity<Void>> deleteEmailFactor(
      Claims claims, Device device, FactorType factorType) {
    var accountSubject = claims.getAccountSubject();
    return commandGateway
        .send(
            FactorCommand.DeactivateFactorCommand.builder()
                .factorType(factorType)
                .accountId(accountSubject.getId())
                .build())
        .doOnError(
            throwable -> {
              if (throwable instanceof ApplicationException e
                  && CoreExceptions.AGGREGATE_NOT_FOUND.getEx().getCode().equals(e.getCode())) {
                throw AuthException.FACTOR_NOT_FOUND.getEx();
              }
            })
        .map(o -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @Override
  public Mono<ResponseEntity<Void>> verifyEmailFactor(
      VerifyEmailFactorRequest request, Claims claims, Device device) {
    return commandGateway
        .send(
            OtpCommand.VerifyOtpCommand.builder()
                .token(request.getChallengeToken())
                .otp(request.getOtp())
                .build())
        .flatMap(
            o -> {
              var verifyOtpOutcome = (VerifyOtpOutcome) o;
              return switch (verifyOtpOutcome.getResult()) {
                case Valid -> commandGateway.send(
                    FactorCommand.ActivateFactorCommand.builder()
                        .factorType(FactorType.email)
                        .accountId(
                            OtpState.get(objectMapper, verifyOtpOutcome.getState()).getAccountId())
                        .build());
                case Invalid -> Mono.error(AuthException.INVALID_OTP.getEx());
                case Expired -> Mono.error(AuthException.EXPIRED_OTP.getEx());
                case Blocked -> Mono.error(AuthException.BLOCKED_OTP_VERIFICATION.getEx());
              };
            })
        .doOnError(
            throwable -> {
              if (throwable instanceof ApplicationException e
                  && CoreExceptions.AGGREGATE_NOT_FOUND.getEx().getCode().equals(e.getCode())) {
                throw AuthException.FACTOR_NOT_FOUND.getEx();
              }
            })
        .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @Override
  public Mono<TotpFactorResponse> createTotpFactor(Claims claims, Device device) {
    var clientConfig = clientConfiguration.getCurrentClient(device.getClient());
    var factorConfig = clientConfig.getFactors();
    var configTotp = factorConfig.getTotp();
    var accountSubject = claims.getAccountSubject();
    return queryGateway
        .query(
            AccountQuery.GetAccountByIdQuery.builder().id(accountSubject.getId()).build(),
            Account.class)
        .flatMap(
            account -> {
              var secret = secretGenerator.generate();
              var iv = AESUtil.generateIv();
              var totpSecret =
                  AESUtil.encrypt(secret, configTotp.getPassword(), configTotp.getSalt(), iv);
              var totpIv = iv.getIV();
              var data =
                  qrDataFactory
                      .newBuilder()
                      .label(account.getEmail())
                      .secret(secret)
                      .issuer(device.getClient())
                      .build();
              String qrCodeImage = qrCodeImage(data);
              String[] codes = recoveryCodeGenerator.generateCodes(16);
              return commandGateway
                  .sendAll(
                      Flux.just(
                          FactorCommand.CreateFactorCommand.builder()
                              .factorType(FactorType.totp)
                              .accountId(account.getId())
                              .build(),
                          TotpCommand.CreateTotpCommand.builder()
                              .recoveryCode(
                                  AESUtil.encrypt(
                                      String.join(",", codes),
                                      configTotp.getPassword(),
                                      configTotp.getSalt(),
                                      iv))
                              .accountId(account.getId())
                              .iv(totpIv)
                              .secret(totpSecret)
                              .build()))
                  .collectList()
                  .thenReturn(
                      TotpFactorResponse.builder()
                          .qrCode(qrCodeImage)
                          .recoveryCode(codes)
                          .data(
                              QrDataResponse.builder()
                                  .period(data.getPeriod())
                                  .digits(data.getDigits())
                                  .algorithm(data.getAlgorithm())
                                  .issuer(data.getIssuer())
                                  .label(data.getLabel())
                                  .secret(data.getSecret())
                                  .type(data.getType())
                                  .uri(data.getUri())
                                  .build())
                          .build());
            });
  }

  @Override
  public Mono<TotpFactorResponse> recoverTotpFactor(
      TotpFactorRecoveryRequest request, Claims claims, Device device) {
    var accountSubject = claims.getAccountSubject();
    return queryGateway
        .query(
            TotpQuery.GetTotpByAccountIdQuery.builder().accountId(accountSubject.getId()).build(),
            Totp.class)
        .map(
            totp -> {
              var clientConfig = clientConfiguration.getCurrentClient(device.getClient());
              var factorConfig = clientConfig.getFactors();
              var configTotp = factorConfig.getTotp();
              var iv = AESUtil.generateIv(totp.getIv());
              var recoveryCode =
                  AESUtil.decrypt(
                      totp.getRecoveryCode(), configTotp.getPassword(), configTotp.getSalt(), iv);
              var codes = recoveryCode.split(",");
              if (!Arrays.equals(request.getRecoveryCode(), codes)) {
                throw AuthException.INVALID_RECOVERY_CODE.getEx();
              }
              var secret =
                  AESUtil.decrypt(
                      totp.getSecret(), configTotp.getPassword(), configTotp.getSalt(), iv);
              var data =
                  qrDataFactory
                      .newBuilder()
                      .label(accountSubject.getEmail())
                      .secret(secret)
                      .issuer(device.getClient())
                      .build();
              String qrCodeImage = qrCodeImage(data);
              return TotpFactorResponse.builder()
                  .qrCode(qrCodeImage)
                  .recoveryCode(codes)
                  .data(
                      QrDataResponse.builder()
                          .period(data.getPeriod())
                          .digits(data.getDigits())
                          .algorithm(data.getAlgorithm())
                          .issuer(data.getIssuer())
                          .label(data.getLabel())
                          .secret(data.getSecret())
                          .type(data.getType())
                          .uri(data.getUri())
                          .build())
                  .build();
            });
  }

  @Override
  public Mono<ResponseEntity<Void>> verifyTotpFactor(
      VerifyTotpFactorRequest request, Claims claims, Device device) {
    var accountSubject = claims.getAccountSubject();
    return queryGateway
        .query(
            TotpQuery.GetTotpByAccountIdQuery.builder().accountId(accountSubject.getId()).build(),
            Totp.class)
        .flatMap(
            totp -> {
              var clientConfig = clientConfiguration.getCurrentClient(device.getClient());
              var factorConfig = clientConfig.getFactors();
              var configTotp = factorConfig.getTotp();
              var iv = AESUtil.generateIv(totp.getIv());
              var secret =
                  AESUtil.decrypt(
                      totp.getSecret(), configTotp.getPassword(), configTotp.getSalt(), iv);
              if (!verifier.isValidCode(secret, request.getOtp())) {
                return Mono.error(AuthException.INVALID_OTP.getEx());
              }
              return commandGateway.send(
                  FactorCommand.ActivateFactorCommand.builder()
                      .factorType(FactorType.totp)
                      .accountId(accountSubject.getId())
                      .build());
            })
        .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @SneakyThrows
  private String qrCodeImage(QrData data) {
    return getDataUriForImage(qrGenerator.generate(data), qrGenerator.getImageMimeType());
  }

  @Builder
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class OtpState {
    private FactorType factorType;
    private String accountId;

    public JsonNode get(ObjectMapper objectMapper) {
      return objectMapper.convertValue(this, JsonNode.class);
    }

    public static OtpState get(ObjectMapper objectMapper, JsonNode jsonNode) {
      try {
        return objectMapper.treeToValue(jsonNode, OtpState.class);
      } catch (JsonProcessingException e) {
        throw CoreExceptions.INTERNAL_SERVER_ERROR.getEx();
      }
    }
  }
}
