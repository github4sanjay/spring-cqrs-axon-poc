package com.example.auth.device;

import static com.example.auth.AuthException.DEVICE_NOT_FOUND;

import com.example.spring.axon.reactor.ApplicationQueryExecutionException;
import com.example.spring.core.exceptions.CoreExceptions;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup("device")
public class DeviceProjector {

  private final DeviceRepository deviceRepository;
  private final QueryUpdateEmitter queryUpdateEmitter;

  @EventHandler
  public void on(DeviceEvent.DeviceRegisteredEvent event) {
    var device =
        Device.builder()
            .publicKey(event.getPublicKey())
            .id(event.getId().toString())
            .hashLength(event.getHashLength())
            .saltLength(event.getSaltLength())
            .hash(event.getHash())
            .os(event.getOs())
            .client(event.getClient())
            .name(event.getName())
            .model(event.getModel())
            .manufacturer(event.getManufacturer())
            .build();
    deviceRepository.save(device);
    queryUpdateEmitter.emit(
        DeviceQuery.GetDeviceByIdQuery.class,
        getDeviceByIdQuery -> getDeviceByIdQuery.getId().toString().equals(device.getId()),
        device);
  }

  @EventHandler
  public void on(DeviceEvent.DeviceNameUpdatedEvent event) {
    var device =
        deviceRepository
            .findById(event.getId())
            .orElseThrow(CoreExceptions.INTERNAL_SERVER_ERROR::getEx);
    device.setName(event.getName());
    deviceRepository.save(device);
  }

  @QueryHandler
  public Device on(DeviceQuery.GetDeviceByIdQuery query) {
    return deviceRepository
        .findById(query.getId())
        .orElseThrow(() -> new ApplicationQueryExecutionException(DEVICE_NOT_FOUND));
  }

  @QueryHandler
  public List<Device> on(DeviceQuery.GetDevicesByIdsQuery query) {
    return deviceRepository.findAllById(query.getIds());
  }
}
