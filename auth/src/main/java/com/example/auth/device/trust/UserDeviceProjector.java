package com.example.auth.device.trust;

import com.example.spring.core.exceptions.CoreExceptions;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Order(1)
@Component
@AllArgsConstructor
@ProcessingGroup("user_device")
public class UserDeviceProjector {

  private final UserDeviceRepository userDeviceRepository;

  @EventHandler
  public void on(UserDeviceEvent.UserDeviceRegisteredEvent event) {
    var userDevice =
        UserDevice.builder()
            .id(event.getId())
            .deviceId(event.getDeviceId())
            .accountId(event.getAccountId())
            .lastLoginAt(event.getLastLoginAt())
            .trusted(event.getIsTrusted())
            .build();
    userDeviceRepository.save(userDevice);
  }

  @EventHandler
  public void on(UserDeviceEvent.UserDeviceLastLoginUpdatedEvent event) {
    var userDevice =
        userDeviceRepository
            .findById(event.getId())
            .orElseThrow(CoreExceptions.INTERNAL_SERVER_ERROR::getEx);
    userDevice.setLastLoginAt(event.getLastLoginAt());
    userDeviceRepository.save(userDevice);
  }

  @EventHandler
  public void on(UserDeviceEvent.UserDeviceTrustedEvent event) {
    var userDevice =
        userDeviceRepository
            .findById(event.getId())
            .orElseThrow(CoreExceptions.INTERNAL_SERVER_ERROR::getEx);
    userDevice.setTrusted(true);
    userDeviceRepository.save(userDevice);
  }

  @EventHandler
  public void on(UserDeviceEvent.UserDeviceTrustRemovedEvent event) {
    var userDevice =
        userDeviceRepository
            .findById(event.getId())
            .orElseThrow(CoreExceptions.INTERNAL_SERVER_ERROR::getEx);
    userDevice.setTrusted(false);
    userDeviceRepository.save(userDevice);
  }

  @QueryHandler
  public List<UserDevice> on(UserDeviceQuery.GetUserDeviceByAccountIdQuery query) {
    return userDeviceRepository.findAllByAccountId(query.getAccountId());
  }
}
