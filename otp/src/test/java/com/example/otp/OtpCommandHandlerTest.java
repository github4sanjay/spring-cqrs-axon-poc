package com.example.otp;

import static com.example.otp.OtpService.OTP_TOKEN_VERIFY_COUNT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.api.messaging.MessagingCommand;
import com.example.api.otp.*;
import java.time.temporal.ChronoUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@WebFluxTest
@ImportAutoConfiguration(value = {RedissonAutoConfiguration.class})
@ContextConfiguration(
    classes = {MockRedis.class, OtpCommandHandler.class, OtpService.class, TestConfiguration.class})
public class OtpCommandHandlerTest {

  @Autowired private OtpCommandHandler commandHandler;
  @MockBean private CommandGateway commandGateway;

  @Autowired private RedissonClient redissonClient;
  @Autowired private TestClock clock;

  @BeforeEach
  public void beforeEach() {
    redissonClient.getKeys().flushall();
  }

  @Test
  @DisplayName(
      "test SendSmsOtpCommand given rate limit ok should send an SendSmsCommand with SendOtpResult Ok")
  public void testGivenSendSmsOtpCommandShouldSendAnSendSmsCommandWithOTPResultOk() {
    var sendSmsOtpCommand =
        OtpCommand.SendSmsOtpCommand.builder()
            .message("Use {code} to login to example.com and manage your account")
            .phoneNumber(PhoneNumber.builder().value("+6587304661").build())
            .options(OtpOptions.builder().resendAfter(30).build())
            .reference("login/+6587304661")
            .build();
    var sendOtpOutcome = commandHandler.on(sendSmsOtpCommand);

    var requestArgumentCaptor = ArgumentCaptor.forClass(MessagingCommand.SendSmsCommand.class);
    verify(commandGateway, times(1)).sendAndWait(requestArgumentCaptor.capture());
    var smsCommand = requestArgumentCaptor.getValue();
    assertEquals(SendOtpResult.Ok, sendOtpOutcome.getResult());
    assertNotNull(sendOtpOutcome.getToken());
    assertTrue(sendOtpOutcome.getRetryAfter() <= 30);

    var otp = TestUtils.extractOtp(smsCommand.getMessage());

    var verifyOtpCommand =
        OtpCommand.VerifyOtpCommand.builder().otp(otp).token(sendOtpOutcome.getToken()).build();

    var verifyOtpOutcome = commandHandler.on(verifyOtpCommand);
    assertEquals(VerifyOtpResult.Valid, verifyOtpOutcome.getResult());
  }

  @Test
  @DisplayName("test VerifyOtpCommand given correct otp should return VerifyOtpResult Valid")
  public void testVerifyOtpCommandGivenCorrectOtpShouldReturnVerifyOtpResultValid() {
    var sendSmsOtpCommand =
        OtpCommand.SendSmsOtpCommand.builder()
            .message("Use {code} to login to example.com and manage your account")
            .phoneNumber(PhoneNumber.builder().value("+6587304661").build())
            .options(OtpOptions.builder().resendAfter(30).build())
            .reference("login/+6587304661")
            .build();
    var sendOtpOutcome = commandHandler.on(sendSmsOtpCommand);

    var requestArgumentCaptor = ArgumentCaptor.forClass(MessagingCommand.SendSmsCommand.class);
    verify(commandGateway, times(1)).sendAndWait(requestArgumentCaptor.capture());
    var smsCommand = requestArgumentCaptor.getValue();

    var otp = TestUtils.extractOtp(smsCommand.getMessage());

    var verifyOtpCommand =
        OtpCommand.VerifyOtpCommand.builder().otp(otp).token(sendOtpOutcome.getToken()).build();

    var verifyOtpOutcome = commandHandler.on(verifyOtpCommand);
    assertEquals(VerifyOtpResult.Valid, verifyOtpOutcome.getResult());
  }

  @Test
  @DisplayName("test VerifyOtpCommand given incorrect otp should return VerifyOtpResult Invalid")
  public void testVerifyOtpCommandGivenInCorrectOtpShouldReturnVerifyOtpResultValid() {
    var sendSmsOtpCommand =
        OtpCommand.SendSmsOtpCommand.builder()
            .message("Use {code} to login to example.com and manage your account")
            .phoneNumber(PhoneNumber.builder().value("+6587304661").build())
            .options(OtpOptions.builder().resendAfter(30).build())
            .reference("login/+6587304661")
            .build();
    var sendOtpOutcome = commandHandler.on(sendSmsOtpCommand);
    var verifyOtpCommand =
        OtpCommand.VerifyOtpCommand.builder()
            .otp("131312")
            .token(sendOtpOutcome.getToken())
            .build();

    var verifyOtpOutcome = commandHandler.on(verifyOtpCommand);
    assertEquals(VerifyOtpResult.Invalid, verifyOtpOutcome.getResult());
  }

  @Test
  @DisplayName(
      "test VerifyOtpCommand more than given verify limit count should return VerifyOtpResult Blocked")
  public void
      testVerifyOtpCommandMoreThanGivenVerifyLimitCountShouldReturnVerifyOtpResultBlocked() {
    var sendSmsOtpCommand =
        OtpCommand.SendSmsOtpCommand.builder()
            .message("Use {code} to login to example.com and manage your account")
            .phoneNumber(PhoneNumber.builder().value("+6587304661").build())
            .options(OtpOptions.builder().verifyLimitCount(3).build())
            .reference("login/+6587304661")
            .build();
    var sendOtpOutcome = commandHandler.on(sendSmsOtpCommand);

    var verifyOtpOutcome1 =
        commandHandler.on(
            OtpCommand.VerifyOtpCommand.builder()
                .otp(RandomStringUtils.randomNumeric(6))
                .token(sendOtpOutcome.getToken())
                .build());
    var verifyOtpOutcome2 =
        commandHandler.on(
            OtpCommand.VerifyOtpCommand.builder()
                .otp(RandomStringUtils.randomNumeric(6))
                .token(sendOtpOutcome.getToken())
                .build());
    var verifyOtpOutcome3 =
        commandHandler.on(
            OtpCommand.VerifyOtpCommand.builder()
                .otp(RandomStringUtils.randomNumeric(6))
                .token(sendOtpOutcome.getToken())
                .build());

    var verifyOtpOutcome4 =
        commandHandler.on(
            OtpCommand.VerifyOtpCommand.builder()
                .otp(RandomStringUtils.randomNumeric(6))
                .token(sendOtpOutcome.getToken())
                .build());
    assertEquals(VerifyOtpResult.Invalid, verifyOtpOutcome1.getResult());
    assertEquals(VerifyOtpResult.Invalid, verifyOtpOutcome2.getResult());
    assertEquals(VerifyOtpResult.Invalid, verifyOtpOutcome3.getResult());
    assertEquals(VerifyOtpResult.Blocked, verifyOtpOutcome4.getResult());
  }

  @Test
  @DisplayName("test VerifyOtpCommand given expired otp should return VerifyOtpResult Expired")
  public void testVerifyOtpCommandGivenExpiredOtpShouldReturnVerifyOtpResultExpired() {
    var sendSmsOtpCommand =
        OtpCommand.SendSmsOtpCommand.builder()
            .message("Use {code} to login to example.com and manage your account")
            .phoneNumber(PhoneNumber.builder().value("+6587304661").build())
            .options(OtpOptions.builder().expiration(5).build())
            .reference("login/+6587304661")
            .build();
    var sendOtpOutcome = commandHandler.on(sendSmsOtpCommand);
    var requestArgumentCaptor = ArgumentCaptor.forClass(MessagingCommand.SendSmsCommand.class);
    verify(commandGateway, times(1)).sendAndWait(requestArgumentCaptor.capture());
    var smsCommand = requestArgumentCaptor.getValue();
    var otp = TestUtils.extractOtp(smsCommand.getMessage());
    var verifyOtpCommand =
        OtpCommand.VerifyOtpCommand.builder().otp(otp).token(sendOtpOutcome.getToken()).build();

    var verifyOtpOutcomeBeforeExpiry = commandHandler.on(verifyOtpCommand);
    assertEquals(VerifyOtpResult.Valid, verifyOtpOutcomeBeforeExpiry.getResult());

    clock.forward(5, ChronoUnit.SECONDS);
    var verifyOtpOutcomeAfterExpiry = commandHandler.on(verifyOtpCommand);
    assertEquals(VerifyOtpResult.Expired, verifyOtpOutcomeAfterExpiry.getResult());
  }

  @Test
  @DisplayName(
      "test VerifyOtpCommand given expired otp from expired redis object should return VerifyOtpResult Expired")
  public void
      testVerifyOtpCommandGivenExpiredOtpFromExpiredRedisObjectShouldReturnVerifyOtpResultExpiredfd() {
    var sendSmsOtpCommand =
        OtpCommand.SendSmsOtpCommand.builder()
            .message("Use {code} to login to example.com and manage your account")
            .phoneNumber(PhoneNumber.builder().value("+6587304661").build())
            .options(OtpOptions.builder().expiration(5).build())
            .reference("login/+6587304661")
            .build();
    var sendOtpOutcome = commandHandler.on(sendSmsOtpCommand);
    var requestArgumentCaptor = ArgumentCaptor.forClass(MessagingCommand.SendSmsCommand.class);
    verify(commandGateway, times(1)).sendAndWait(requestArgumentCaptor.capture());
    var smsCommand = requestArgumentCaptor.getValue();
    var otp = TestUtils.extractOtp(smsCommand.getMessage());
    var verifyOtpCommand =
        OtpCommand.VerifyOtpCommand.builder().otp(otp).token(sendOtpOutcome.getToken()).build();

    RAtomicLong atomicLong =
        redissonClient.getAtomicLong(OTP_TOKEN_VERIFY_COUNT + sendOtpOutcome.getToken());
    atomicLong.delete();
    var verifyOtpOutcomeAfterExpiry = commandHandler.on(verifyOtpCommand);
    assertEquals(VerifyOtpResult.Expired, verifyOtpOutcomeAfterExpiry.getResult());
  }

  @Test
  @DisplayName(
      "test SendSmsOtpCommand given two consecutive request before resendAfter allowed time should return BlockedEarlyRequest")
  public void
      testSendSmsOtpCommandGivenTwoConsecutiveRequestBeforeResendAfterAllowedTimeShouldReturnBlockedEarlyRequest() {
    var sendSmsOtpCommand =
        OtpCommand.SendSmsOtpCommand.builder()
            .message("Use {code} to login to example.com and manage your account")
            .phoneNumber(PhoneNumber.builder().value("+6587304661").build())
            .options(OtpOptions.builder().resendAfter(5).build())
            .reference("login/+6587304661")
            .build();
    var sendOtpOutcome1 = commandHandler.on(sendSmsOtpCommand);
    var sendOtpOutcome2 = commandHandler.on(sendSmsOtpCommand);
    assertEquals(SendOtpResult.Ok, sendOtpOutcome1.getResult());
    assertEquals(SendOtpResult.BlockedEarlyRequest, sendOtpOutcome2.getResult());
    clock.forward(5, ChronoUnit.SECONDS);

    var sendOtpOutcome3 = commandHandler.on(sendSmsOtpCommand);
    assertEquals(SendOtpResult.Ok, sendOtpOutcome3.getResult());
  }

  @Test
  @DisplayName(
      "test SendSmsOtpCommand given two consecutive request before resendAfter allowed time should return BlockedEarlyRequest")
  public void
      testSendSmsOtpCommandGivenTwoConsecutiveRequestBeforeResendAfterAllowedTimeShouldReturnBlockedEarlyRequestsf() {
    var sendSmsOtpCommand =
        OtpCommand.SendSmsOtpCommand.builder()
            .message("Use {code} to login to example.com and manage your account")
            .phoneNumber(PhoneNumber.builder().value("+6587304661").build())
            .options(
                OtpOptions.builder().resendAfter(5).rateLimitCount(3).rateLimitExpiry(30).build())
            .reference("login/+6587304661")
            .build();
    var sendOtpOutcome1 = commandHandler.on(sendSmsOtpCommand);
    assertEquals(SendOtpResult.Ok, sendOtpOutcome1.getResult());

    clock.forward(5, ChronoUnit.SECONDS);
    var sendOtpOutcome2 = commandHandler.on(sendSmsOtpCommand);
    assertEquals(SendOtpResult.Ok, sendOtpOutcome2.getResult());

    clock.forward(5, ChronoUnit.SECONDS);
    var sendOtpOutcome3 = commandHandler.on(sendSmsOtpCommand);
    assertEquals(SendOtpResult.Ok, sendOtpOutcome3.getResult());

    clock.forward(5, ChronoUnit.SECONDS);
    var sendOtpOutcome4 = commandHandler.on(sendSmsOtpCommand);
    assertEquals(SendOtpResult.BlockedTooManyRequests, sendOtpOutcome4.getResult());
  }
}
