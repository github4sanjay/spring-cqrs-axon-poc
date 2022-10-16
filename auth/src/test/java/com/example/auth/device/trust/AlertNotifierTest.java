package com.example.auth.device.trust;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.api.messaging.MessagingCommand;
import com.example.auth.account.Account;
import com.example.auth.account.AccountRepository;
import com.example.auth.account.AccountStatus;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.Device;
import com.example.auth.device.DeviceRegistrationConfig;
import com.example.auth.device.DeviceRepository;
import java.time.Instant;
import java.util.UUID;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thymeleaf.spring5.SpringTemplateEngine;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@Import(value = {AlertNotifier.class, SpringTemplateEngine.class, ClientConfiguration.class})
class AlertNotifierTest {

  @Autowired private DeviceRepository deviceRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private UserDeviceRepository userDeviceRepository;
  @Autowired private AlertNotifier alertNotifier;
  @Autowired private ClientConfiguration clientConfiguration;

  @MockBean private CommandGateway commandGateway;

  @Test
  @DisplayName("test given UserDeviceRegisteredEvent should send email")
  public void testGivenUserDeviceRegisteredEventShouldSendEmail() {
    var deviceId = UUID.randomUUID().toString();
    var accountId = UUID.randomUUID().toString();

    deviceRepository.save(
        Device.builder()
            .publicKey("publicKey")
            .id(deviceId)
            .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
            .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
            .hash("some-hash")
            .client("web-app")
            .manufacturer("Windows")
            .os("windows")
            .model("DFFG-123")
            .name("Some Name")
            .build());

    accountRepository.save(
        Account.builder()
            .id(accountId)
            .hashLength(16)
            .email("github4sanjay@gmail.com")
            .saltLength(32)
            .password("password")
            .status(AccountStatus.ACTIVE)
            .build());

    when(commandGateway.sendAndWait(any())).thenReturn(UUID.randomUUID().toString());

    alertNotifier.on(
        UserDeviceEvent.UserDeviceRegisteredEvent.builder()
            .id(UUID.randomUUID().toString())
            .deviceId(deviceId)
            .accountId(accountId)
            .lastLoginAt(Instant.now())
            .isTrusted(false)
            .build());

    ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
    verify(commandGateway, times(1)).sendAndWait(argument.capture());
    var value = argument.getValue();
    assertTrue(value instanceof MessagingCommand.SendEmailCommand);
    var sendEmailCommand = (MessagingCommand.SendEmailCommand) value;
    assertEquals("github4sanjay@gmail.com", sendEmailCommand.getTo().get(0));

    var clientConfig = clientConfiguration.getCurrentClient("web-app");
    var alerts = clientConfig.getAlerts();
    var email = alerts.getNewDeviceDetected();

    assertEquals(email.getEmail().getSubject(), sendEmailCommand.getSubject());
  }

  @Test
  @DisplayName("test given UserDeviceTrustedEvent should send email")
  public void testGivenUserDeviceTrustedEventShouldSendEmail() {
    var deviceId = UUID.randomUUID().toString();
    var accountId = UUID.randomUUID().toString();
    var id = UUID.randomUUID().toString();

    deviceRepository.save(
        Device.builder()
            .publicKey("publicKey")
            .id(deviceId)
            .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
            .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
            .hash("some-hash")
            .client("web-app")
            .manufacturer("Windows")
            .os("windows")
            .model("DFFG-123")
            .name("Some Name")
            .build());

    accountRepository.save(
        Account.builder()
            .id(accountId)
            .hashLength(16)
            .email("github4sanjay@gmail.com")
            .saltLength(32)
            .password("password")
            .status(AccountStatus.ACTIVE)
            .build());

    userDeviceRepository.save(
        UserDevice.builder()
            .id(id)
            .deviceId(deviceId)
            .accountId(accountId)
            .lastLoginAt(Instant.now())
            .trusted(true)
            .build());

    when(commandGateway.sendAndWait(any())).thenReturn(UUID.randomUUID().toString());

    alertNotifier.on(UserDeviceEvent.UserDeviceTrustedEvent.builder().id(id).build());

    ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
    verify(commandGateway, times(1)).sendAndWait(argument.capture());
    var value = argument.getValue();
    assertTrue(value instanceof MessagingCommand.SendEmailCommand);
    var sendEmailCommand = (MessagingCommand.SendEmailCommand) value;
    assertEquals("github4sanjay@gmail.com", sendEmailCommand.getTo().get(0));

    var clientConfig = clientConfiguration.getCurrentClient("web-app");
    var alerts = clientConfig.getAlerts();
    var email = alerts.getTrustedDeviceAdded();

    assertEquals(email.getEmail().getSubject(), sendEmailCommand.getSubject());
  }
}
