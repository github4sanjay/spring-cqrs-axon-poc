package com.example.auth.device.trust;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@Import(value = {UserDeviceProjector.class})
class UserDeviceProjectorTest {

  @Autowired private UserDeviceProjector userDeviceProjector;
  @Autowired private UserDeviceRepository userDeviceRepository;

  @Test
  @DisplayName("test when UserDeviceRegisteredEvent received should save UserDevice in DB")
  public void testWhenUserDeviceRegisteredEventReceivedShouldSaveUserDeviceInDB() {
    var event =
        UserDeviceEvent.UserDeviceRegisteredEvent.builder()
            .id(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .accountId(UUID.randomUUID().toString())
            .lastLoginAt(Instant.now())
            .isTrusted(false)
            .build();
    userDeviceProjector.on(event);

    var mayBeUserDevice = userDeviceRepository.findById(event.getId());
    assertTrue(mayBeUserDevice.isPresent());
    var userDevice = mayBeUserDevice.get();
    assertEquals(event.getDeviceId(), userDevice.getDeviceId());
    assertEquals(event.getId(), userDevice.getId());
    assertEquals(event.getAccountId(), userDevice.getAccountId());
    assertEquals(event.getIsTrusted(), userDevice.getTrusted());
    assertEquals(event.getLastLoginAt(), userDevice.getLastLoginAt());
  }

  @Test
  @DisplayName(
      "test when UserDeviceLastLoginUpdatedEvent received should update UserDevice lastLoginAt in DB")
  public void testWhenUserDeviceLastLoginUpdatedEventReceivedShouldUpdateLastLoginAtInDB() {

    var userDevice =
        UserDevice.builder()
            .id(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .accountId(UUID.randomUUID().toString())
            .lastLoginAt(Instant.now())
            .trusted(false)
            .build();

    userDeviceRepository.save(userDevice);

    var event =
        UserDeviceEvent.UserDeviceLastLoginUpdatedEvent.builder()
            .id(userDevice.getId())
            .lastLoginAt(Instant.now())
            .build();
    userDeviceProjector.on(event);

    var mayBeUserDevice = userDeviceRepository.findById(event.getId());
    assertTrue(mayBeUserDevice.isPresent());
    var updatedUserDevice = mayBeUserDevice.get();
    assertEquals(event.getId(), updatedUserDevice.getId());
    assertEquals(event.getLastLoginAt(), updatedUserDevice.getLastLoginAt());
  }

  @Test
  @DisplayName(
      "test when UserDeviceTrustedEvent received should update UserDevice trusted to true in DB")
  public void testWhenUserDeviceTrustedEventReceivedShouldUpdateTrustedToTrueInDB() {

    var userDevice =
        UserDevice.builder()
            .id(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .accountId(UUID.randomUUID().toString())
            .lastLoginAt(Instant.now())
            .trusted(false)
            .build();

    userDeviceRepository.save(userDevice);

    var event = UserDeviceEvent.UserDeviceTrustedEvent.builder().id(userDevice.getId()).build();
    userDeviceProjector.on(event);

    var mayBeUserDevice = userDeviceRepository.findById(event.getId());
    assertTrue(mayBeUserDevice.isPresent());
    var updatedUserDevice = mayBeUserDevice.get();
    assertEquals(event.getId(), updatedUserDevice.getId());
    assertTrue(updatedUserDevice.getTrusted());
  }

  @Test
  @DisplayName(
      "test when UserDeviceTrustRemovedEvent received should update UserDevice trusted to false in DB")
  public void testWheUserDeviceTrustRemovedEventReceivedShouldUpdateTrustedToFalseInDB() {

    var userDevice =
        UserDevice.builder()
            .id(UUID.randomUUID().toString())
            .deviceId(UUID.randomUUID().toString())
            .accountId(UUID.randomUUID().toString())
            .lastLoginAt(Instant.now())
            .trusted(true)
            .build();

    userDeviceRepository.save(userDevice);

    var event =
        UserDeviceEvent.UserDeviceTrustRemovedEvent.builder().id(userDevice.getId()).build();
    userDeviceProjector.on(event);

    var mayBeUserDevice = userDeviceRepository.findById(event.getId());
    assertTrue(mayBeUserDevice.isPresent());
    var updatedUserDevice = mayBeUserDevice.get();
    assertEquals(event.getId(), updatedUserDevice.getId());
    assertFalse(updatedUserDevice.getTrusted());
  }

  @Test
  @DisplayName("test GetUserDeviceByAccountIdQuery expect user devices list to be returned")
  public void testGetDevicesByIdsQueryExpectDeviceListToBeReturned() {
    var accountId = UUID.randomUUID().toString();
    userDeviceRepository.saveAll(
        List.of(
            UserDevice.builder()
                .id(UUID.randomUUID().toString())
                .deviceId(UUID.randomUUID().toString())
                .accountId(accountId)
                .lastLoginAt(Instant.now())
                .trusted(true)
                .build(),
            UserDevice.builder()
                .id(UUID.randomUUID().toString())
                .deviceId(UUID.randomUUID().toString())
                .accountId(UUID.randomUUID().toString())
                .lastLoginAt(Instant.now())
                .trusted(true)
                .build()));
    var devices =
        userDeviceProjector.on(
            UserDeviceQuery.GetUserDeviceByAccountIdQuery.builder().accountId(accountId).build());
    assertEquals(1, devices.size());
  }
}
