package com.example.auth.device.trust;

import com.example.api.messaging.MessagingCommand;
import com.example.api.messaging.Priority;
import com.example.auth.account.AccountRepository;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.Device;
import com.example.auth.device.DeviceRepository;
import com.example.spring.core.exceptions.CoreExceptions;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
@ProcessingGroup("user_device")
public class AlertNotifier {

  private final CommandGateway commandGateway;
  private final SpringTemplateEngine htmlTemplateEngine;
  private final ClientConfiguration clientConfig;
  private final DeviceRepository deviceRepository;
  private final AccountRepository accountRepository;
  private final UserDeviceRepository userDeviceRepository;

  @EventHandler
  public void on(UserDeviceEvent.UserDeviceRegisteredEvent event) {
    var deviceId = event.getDeviceId();
    var device =
        deviceRepository
            .findById(deviceId)
            .orElseThrow(CoreExceptions.INTERNAL_SERVER_ERROR::getEx);
    var account =
        accountRepository
            .findById(event.getAccountId())
            .orElseThrow(CoreExceptions.INTERNAL_SERVER_ERROR::getEx);
    sendNewDeviceDetectedAlert(
        DeviceInfo.builder()
            .device(device)
            .userEmail(account.getEmail())
            .actionTime(event.getLastLoginAt())
            .build(),
        device.getClient());
  }

  @EventHandler
  public void on(UserDeviceEvent.UserDeviceTrustedEvent event) {
    var userDevice =
        userDeviceRepository
            .findById(event.getId())
            .orElseThrow(CoreExceptions.INTERNAL_SERVER_ERROR::getEx);
    var device =
        deviceRepository
            .findById(userDevice.getDeviceId())
            .orElseThrow(CoreExceptions.INTERNAL_SERVER_ERROR::getEx);
    var account =
        accountRepository
            .findById(userDevice.getAccountId())
            .orElseThrow(CoreExceptions.INTERNAL_SERVER_ERROR::getEx);
    sendTrustedDeviceAddedAlert(
        DeviceInfo.builder()
            .device(device)
            .userEmail(account.getEmail())
            .actionTime(Instant.now())
            .build(),
        device.getClient());
  }

  public void sendTrustedDeviceAddedAlert(DeviceInfo deviceInfo, String client) {
    var currentClient = clientConfig.getCurrentClient(client);
    if (!deviceInfo.getUserEmail().isEmpty())
      sendTrustedDeviceAddedEmail(deviceInfo, currentClient);
    // if (!deviceInfo.getPhoneNumber().isEmpty()) sendTrustedDeviceAddedSms(deviceInfo,
    // currentClient);
  }

  public void sendNewDeviceDetectedAlert(DeviceInfo deviceInfo, String client) {
    var currentClient = clientConfig.getCurrentClient(client);
    if (!deviceInfo.getUserEmail().isEmpty()) sendNewDeviceDetectedEmail(deviceInfo, currentClient);
    // if (!deviceInfo.getPhoneNumber().isEmpty()) sendNewDeviceDetectedSms(deviceInfo,
    // currentClient);
  }

  private void sendNewDeviceDetectedEmail(
      DeviceInfo deviceInfo, ClientConfiguration.Client client) {
    var email = client.getAlerts().getNewDeviceDetected().getEmail();
    final var ctx = new Context();
    ctx.setVariable("name", deviceInfo.getUserFirstName());
    ctx.setVariable("lastLogin", Date.from(deviceInfo.getActionTime()));
    var device = deviceInfo.getDevice();
    ctx.setVariable("deviceName", device.getName());
    final var htmlContent = this.htmlTemplateEngine.process(email.getTemplate(), ctx);
    var command =
        MessagingCommand.SendEmailCommand.builder()
            .profile(client.getMessageProfile())
            .body(htmlContent)
            .from(email.getFrom())
            .id(UUID.randomUUID().toString())
            .subject(email.getSubject())
            .to(List.of(deviceInfo.getUserEmail()))
            .priority(Priority.HIGH)
            .build();
    commandGateway.sendAndWait(command);
    log.info("sending NewDeviceDetected email {} for {}", command.getId(), deviceInfo);
  }

  private void sendTrustedDeviceAddedEmail(
      DeviceInfo deviceInfo, ClientConfiguration.Client client) {
    var email = client.getAlerts().getTrustedDeviceAdded().getEmail();
    final var ctx = new Context();
    ctx.setVariable("name", deviceInfo.getUserFirstName());
    ctx.setVariable("lastLogin", Date.from(deviceInfo.getActionTime()));
    var device = deviceInfo.getDevice();
    ctx.setVariable("deviceName", device.getName());
    final var htmlContent = this.htmlTemplateEngine.process(email.getTemplate(), ctx);
    var command =
        MessagingCommand.SendEmailCommand.builder()
            .profile(client.getMessageProfile())
            .body(htmlContent)
            .from(email.getFrom())
            .id(UUID.randomUUID().toString())
            .subject(email.getSubject())
            .to(List.of(deviceInfo.getUserEmail()))
            .priority(Priority.HIGH)
            .build();
    commandGateway.sendAndWait(command);
    log.info("sending TrustedDeviceAdded email {} for {}", command.getId(), deviceInfo);
  }

  /*private void sendNewDeviceDetectedSms(DeviceInfo deviceInfo, ClientConfiguration.Client client) {
    final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.of(client.getZoneId()));
    var device = deviceInfo.getDevice();
    var message =
        MessageFormat.format(
            client.getAlerts().getNewDeviceDetected().getSms(),
            device.getName(),
            dateTimeFormatter.format(deviceInfo.getActionTime()));
    var command =
        MessagingCommand.SendSmsCommand.builder()
            .profile(client.getMessageProfile())
            .priority(Priority.HIGH)
            .message(message)
            .id(UUID.randomUUID().toString())
            .phoneNumber(PhoneNumber.builder().value(deviceInfo.getPhoneNumber()).build())
            .build();
    commandGateway.sendAndWait(command);
    log.info("sending NewDeviceDetected sms {} for {}", command.getId(), deviceInfo);
  }

  private void sendTrustedDeviceAddedSms(DeviceInfo deviceInfo, ClientConfiguration.Client client) {
    final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.of(client.getZoneId()));
    var device = deviceInfo.getDevice();
    var message =
        MessageFormat.format(
            client.getAlerts().getTrustedDeviceAdded().getSms(),
            device.getName(),
            dateTimeFormatter.format(deviceInfo.getActionTime()));
    var command =
        MessagingCommand.SendSmsCommand.builder()
            .profile(client.getMessageProfile())
            .priority(Priority.HIGH)
            .message(message)
            .id(UUID.randomUUID().toString())
            .phoneNumber(PhoneNumber.builder().value(deviceInfo.getPhoneNumber()).build())
            .build();
    commandGateway.sendAndWait(command);
    log.info("sending TrustedDeviceAdded sms {} for {}", command.getId(), deviceInfo);
  }

  private String formatNumber(String phoneNumber) {
    return phoneNumber.substring(phoneNumber.length() - 4);
  }*/

  @Data
  @Builder
  public static class DeviceInfo {
    private final String userFirstName;
    private final String userEmail;
    private final Device device;
    private final Instant actionTime;
    private final String phoneNumber;

    public DeviceInfo(
        String userFirstName,
        String userEmail,
        Device device,
        Instant actionTime,
        String phoneNumber) {
      this.userFirstName = userFirstName;
      this.userEmail = StringUtils.isBlank(userEmail) ? "" : userEmail;
      this.device = device;
      this.actionTime = actionTime;
      this.phoneNumber = StringUtils.isBlank(phoneNumber) ? "" : phoneNumber;
    }
  }

  @Data
  @Builder
  public static class MobileUpdateInfo {
    private final String userFirstName;
    private final String userEmail;
    private final String phoneNumber;
    private final Instant updateTime;

    public MobileUpdateInfo(
        String userFirstName, String userEmail, String phoneNumber, Instant updateTime) {
      this.userFirstName = userFirstName;
      this.userEmail = StringUtils.isBlank(userEmail) ? "" : userEmail;
      this.phoneNumber = StringUtils.isBlank(phoneNumber) ? "" : phoneNumber;
      this.updateTime = updateTime;
    }
  }
}
