package com.example.auth.device;

import static org.junit.jupiter.api.Assertions.*;

import com.example.auth.AuthException;
import com.example.spring.axon.reactor.ApplicationQueryExecutionException;
import com.example.spring.core.exceptions.IException;
import java.util.List;
import java.util.UUID;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@Import(value = {DeviceProjector.class})
class DeviceProjectorTest {

  @MockBean private QueryUpdateEmitter queryUpdateEmitter;
  @Autowired private DeviceProjector deviceProjector;
  @Autowired private DeviceRepository deviceRepository;

  @BeforeEach
  public void beforeEach() {
    deviceRepository.deleteAll();
  }

  @Test
  @DisplayName("test device registered event handler should save data in db")
  public void testDeviceRegisteredEventHandlerShouldSaveDataInDb() {
    var event =
        DeviceEvent.DeviceRegisteredEvent.builder()
            .publicKey("publicKey")
            .id(UUID.randomUUID().toString())
            .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
            .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
            .hash("some-hash")
            .client("web-app")
            .manufacturer("Windows")
            .os("windows")
            .model("DFFG-123")
            .name("Some Name")
            .build();
    deviceProjector.on(event);
    var mayBeDevice = deviceRepository.findById(event.getId().toString());
    Assertions.assertTrue(mayBeDevice.isPresent());
    var device = mayBeDevice.get();
    Assertions.assertNotNull(device.getModel());
    Assertions.assertNotNull(device.getOs());
    Assertions.assertNotNull(device.getManufacturer());
    Assertions.assertNotNull(device.getName());
    Assertions.assertNotNull(device.getClient());
    Assertions.assertNotNull(device.getHash());
    Assertions.assertNotNull(device.getHashLength());
    Assertions.assertNotNull(device.getPublicKey());
    Assertions.assertNotNull(device.getSaltLength());
  }

  @Test
  @DisplayName(
      "test GetDeviceByIdQuery when device not present should return device not found exception")
  public void testGetDeviceByIdQueryWhenDeviceNotPresentShouldReturnDeviceNotFoundException() {
    var exception =
        Assertions.assertThrows(
            ApplicationQueryExecutionException.class,
            () ->
                deviceProjector.on(
                    DeviceQuery.GetDeviceByIdQuery.builder()
                        .id(UUID.randomUUID().toString())
                        .build()));
    Assertions.assertTrue(exception.getDetails().isPresent());
    var iException = (IException) exception.getDetails().get();
    Assertions.assertEquals(
        AuthException.DEVICE_NOT_FOUND.getEx().getCode(), iException.getEx().getCode());
  }

  @Test
  @DisplayName("test GetDeviceByIdQuery when device present should return device")
  public void testGetDeviceByIdQueryWhenDevicePresentShouldReturnDevice() {
    var id = UUID.randomUUID().toString();
    deviceRepository.save(
        Device.builder()
            .publicKey("publicKey")
            .id(id.toString())
            .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
            .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
            .hash("some-hash")
            .client("web-app")
            .manufacturer("Windows")
            .os("windows")
            .model("DFFG-123")
            .name("Some Name")
            .build());
    var device = deviceProjector.on(DeviceQuery.GetDeviceByIdQuery.builder().id(id).build());
    Assertions.assertEquals(id, device.getId());
  }

  @Test
  @DisplayName("test when DeviceNameUpdatedEvent should change the name of the device in DB")
  public void testWhenDeviceNameUpdatedEventShouldChangeTheNameOfTheDeviceInDB() {
    var id = UUID.randomUUID().toString();
    deviceRepository.save(
        Device.builder()
            .publicKey("publicKey")
            .id(id.toString())
            .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
            .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
            .hash("some-hash")
            .client("web-app")
            .manufacturer("Windows")
            .os("windows")
            .model("DFFG-123")
            .name("Some Name")
            .build());
    deviceProjector.on(
        DeviceEvent.DeviceNameUpdatedEvent.builder().id(id).name("Some Name Changed").build());
    var mayBeUpdatedDevice = deviceRepository.findById(id);
    Assertions.assertTrue(mayBeUpdatedDevice.isPresent());
    var updatedDevice = mayBeUpdatedDevice.get();
    Assertions.assertEquals("Some Name Changed", updatedDevice.getName());
  }

  @Test
  @DisplayName("test GetDevicesByIdsQuery expect devices list to be returned")
  public void testGetDevicesByIdsQueryExpectDeviceListToBeReturned() {
    var id = UUID.randomUUID().toString();
    deviceRepository.saveAll(
        List.of(
            Device.builder()
                .publicKey("publicKey")
                .id(id)
                .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
                .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
                .hash("some-hash")
                .client("web-app")
                .manufacturer("Windows")
                .os("windows")
                .model("DFFG-123")
                .name("Some Name")
                .build(),
            Device.builder()
                .publicKey("publicKey")
                .id(UUID.randomUUID().toString())
                .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
                .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
                .hash("some-hash")
                .client("web-app")
                .manufacturer("Windows")
                .os("windows")
                .model("DFFG-123")
                .name("Some Name")
                .build()));
    var devices =
        deviceProjector.on(DeviceQuery.GetDevicesByIdsQuery.builder().ids(List.of(id)).build());
    assertEquals(1, devices.size());
  }
}
