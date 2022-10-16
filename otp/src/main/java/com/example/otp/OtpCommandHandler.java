package com.example.otp;

import com.example.api.messaging.MessagingCommand;
import com.example.api.messaging.PhoneNumber;
import com.example.api.messaging.Priority;
import com.example.api.otp.OtpCommand;
import com.example.api.otp.SendOtpOutcome;
import com.example.api.otp.SendOtpResult;
import com.example.api.otp.VerifyOtpOutcome;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OtpCommandHandler {
  private final CommandGateway commandGateway;
  private final OtpService otpService;

  @CommandHandler
  public SendOtpOutcome on(OtpCommand.SendSmsOtpCommand command) {
    var options = command.getOptions();
    var mayBeRetryAfterForEarlyRequest =
        otpService.getRetryAfterForEarlyRequest(command.getId(), options.getResendAfter());
    if (mayBeRetryAfterForEarlyRequest.isPresent()) {
      return SendOtpOutcome.builder()
          .retryAfter(mayBeRetryAfterForEarlyRequest.get())
          .result(SendOtpResult.BlockedEarlyRequest)
          .build();
    }

    var mayBeRetryAfterForRateLimit =
        otpService.getRetryAfterForRateLimit(
            command.getId(), options.getRateLimitCount(), options.getRateLimitExpiry());
    if (mayBeRetryAfterForRateLimit.isPresent()) {
      return SendOtpOutcome.builder()
          .retryAfter(mayBeRetryAfterForRateLimit.get())
          .result(SendOtpResult.BlockedTooManyRequests)
          .build();
    }

    var token = UUID.randomUUID().toString();
    var otp = RandomStringUtils.randomNumeric(6);

    otpService.saveToken(
        token, otp, options.getState(), options.getExpiration(), options.getVerifyLimitCount());

    commandGateway.sendAndWait(
        MessagingCommand.SendSmsCommand.builder()
            .message(command.getMessage().replace("{code}", otp))
            .id(token)
            .phoneNumber(PhoneNumber.builder().value(command.getPhoneNumber().getValue()).build())
            .profile(options.getProfile())
            .priority(Priority.HIGH)
            .build());

    return SendOtpOutcome.builder()
        .token(token)
        .retryAfter(options.getResendAfter().longValue())
        .result(SendOtpResult.Ok)
        .build();
  }

  @CommandHandler
  public SendOtpOutcome on(OtpCommand.SendEmailOtpCommand command) {
    var options = command.getOptions();
    var mayBeRetryAfterForEarlyRequest =
        otpService.getRetryAfterForEarlyRequest(command.getId(), options.getResendAfter());
    if (mayBeRetryAfterForEarlyRequest.isPresent()) {
      return SendOtpOutcome.builder()
          .retryAfter(mayBeRetryAfterForEarlyRequest.get())
          .result(SendOtpResult.BlockedEarlyRequest)
          .build();
    }

    var mayBeRetryAfterForRateLimit =
        otpService.getRetryAfterForRateLimit(
            command.getId(), options.getRateLimitCount(), options.getRateLimitExpiry());
    if (mayBeRetryAfterForRateLimit.isPresent()) {
      return SendOtpOutcome.builder()
          .retryAfter(mayBeRetryAfterForRateLimit.get())
          .result(SendOtpResult.BlockedTooManyRequests)
          .build();
    }

    var token = UUID.randomUUID().toString();
    var otp = RandomStringUtils.randomNumeric(6);

    otpService.saveToken(
        token, otp, options.getState(), options.getExpiration(), options.getVerifyLimitCount());

    commandGateway.sendAndWait(
        MessagingCommand.SendEmailCommand.builder()
            .subject(command.getSubject().replace("{code}", otp))
            .id(token)
            .body(command.getBody().replace("{code}", otp))
            .from(command.getFromEmail())
            .to(List.of(command.getToEmail()))
            .profile(options.getProfile())
            .priority(Priority.HIGH)
            .build());

    return SendOtpOutcome.builder()
        .token(token)
        .retryAfter(options.getResendAfter().longValue())
        .result(SendOtpResult.Ok)
        .build();
  }

  @CommandHandler
  public VerifyOtpOutcome on(OtpCommand.VerifyOtpCommand command) {
    return otpService.verifyOtp(command.getToken(), command.getOtp());
  }
}
