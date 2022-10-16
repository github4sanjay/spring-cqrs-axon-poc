package com.example.auth.device;

import com.example.auth.AuthException;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.device.trust.UserDevice;
import com.example.auth.device.trust.UserDeviceCommand;
import com.example.auth.device.trust.UserDeviceQuery;
import com.example.auth.token.Claims;
import com.example.auth.token.refresh.RefreshTokenCommand;
import com.example.auth.token.refresh.RefreshTokenService;
import com.example.security.core.RSAUtil;
import com.example.spring.core.exceptions.ApplicationException;
import com.example.spring.core.exceptions.CoreExceptions;
import java.util.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DeviceController implements DeviceAPI {
  private final ReactorCommandGateway commandGateway;
  private final ReactorQueryGateway queryGateway;
  private final DeviceRegistrationConfig deviceRegistrationConfig;
  private final ClientConfiguration clientConfiguration;
  private final RefreshTokenService refreshTokenService;

  @Override
  public Mono<DeviceRegistrationResponse> registerDevice(DeviceRequest deviceRequest) {
    if (!clientConfiguration.isValidClient(deviceRequest.getClient())) {
      throw AuthException.INVALID_CLIENT.getEx();
    }
    var password = UUID.randomUUID().toString();
    var keyPairGenerator = RSAUtil.generateKeyPair();
    var privateKey = Base64.getEncoder().encodeToString(keyPairGenerator.getPrivate().getEncoded());
    var publicKey = Base64.getEncoder().encodeToString(keyPairGenerator.getPublic().getEncoded());
    var command =
        DeviceCommand.RegisterDeviceCommand.builder()
            .publicKey(publicKey)
            .id(UUID.randomUUID().toString())
            .hashLength(DeviceRegistrationConfig.HASH_LENGTH)
            .saltLength(DeviceRegistrationConfig.SALT_LENGTH)
            .hash(deviceRegistrationConfig.getHash(password))
            .os(deviceRequest.getOs())
            .client(deviceRequest.getClient())
            .name(deviceRequest.getName())
            .model(deviceRequest.getModel())
            .manufacturer(deviceRequest.getManufacturer())
            .build();
    return commandGateway
        .send(command)
        .publishOn(Schedulers.boundedElastic())
        .mapNotNull(
            o ->
                queryGateway
                    .subscriptionQuery(
                        DeviceQuery.GetDeviceByIdQuery.builder().id(command.getId()).build(),
                        Device.class)
                    .blockFirst())
        .map(
            device ->
                DeviceRegistrationResponse.builder()
                    .id(device.getId() + "." + password)
                    .key(privateKey)
                    .build());
  }

  @Override
  public Mono<ResponseEntity<Void>> updateDevice(DeviceUpdateRequest request, Device device) {
    return commandGateway
        .send(
            DeviceCommand.UpdateDeviceNameCommand.builder()
                .id(device.getId())
                .name(request.getName())
                .build())
        .map(o -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @Override
  public Mono<ResponseEntity<Void>> trust(Claims claims, Device device) {
    var subject = claims.getAccountSubject();
    return commandGateway
        .send(
            UserDeviceCommand.TrustUserDeviceCommand.builder()
                .accountId(subject.getId())
                .deviceId(device.getId())
                .build())
        .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @Override
  public Mono<ResponseEntity<Void>> deleteTrust(Device device, String deviceId, Claims claims) {
    var subject = claims.getAccountSubject();
    return commandGateway
        .send(
            UserDeviceCommand.RemoveTrustUserDeviceCommand.builder()
                .accountId(subject.getId())
                .deviceId(deviceId)
                .build())
        .doOnError(
            throwable -> {
              if (throwable instanceof ApplicationException e
                  && CoreExceptions.AGGREGATE_NOT_FOUND.getEx().getCode().equals(e.getCode())) {
                throw AuthException.DEVICE_NOT_FOUND.getEx();
              }
            })
        .map(
            o ->
                refreshTokenService.on(
                    RefreshTokenCommand.DisableRefreshTokenCommand.builder()
                        .identifier(subject.getId())
                        .deviceId(deviceId)
                        .build()))
        .map(o -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @Override
  public Mono<List<DeviceResponse>> getDevices(Boolean isTrusted, Claims claims) {
    log.info("getDevices");
    var accountSubject = claims.getAccountSubject();
    return queryGateway
        .query(
            UserDeviceQuery.GetUserDeviceByAccountIdQuery.builder()
                .accountId(accountSubject.getId())
                .build(),
            ResponseTypes.multipleInstancesOf(UserDevice.class))
        .flatMap(
            userDevices -> {
              var deviceIds = new ArrayList<String>();
              var deviceIdVsUserDevice = new HashMap<String, UserDevice>();
              for (var userDevice : userDevices) {
                if (isTrusted == null
                    || (isTrusted && userDevice.getTrusted())
                    || (!isTrusted && !userDevice.getTrusted())) {
                  deviceIdVsUserDevice.put(userDevice.getDeviceId(), userDevice);
                  deviceIds.add(userDevice.getDeviceId());
                }
              }
              return Mono.zip(
                  queryGateway.query(
                      DeviceQuery.GetDevicesByIdsQuery.builder().ids(deviceIds).build(),
                      ResponseTypes.multipleInstancesOf(Device.class)),
                  Mono.just(deviceIdVsUserDevice));
            })
        .map(
            objects -> {
              var devices = objects.getT1();
              var deviceIdVsUserDevice = objects.getT2();
              var deviceResponses = new ArrayList<DeviceResponse>();
              devices.forEach(
                  device -> {
                    var userDevice = deviceIdVsUserDevice.get(device.getId());
                    var deviceResponse =
                        DeviceResponse.builder()
                            .client(device.getClient())
                            .model(device.getModel())
                            .name(device.getName())
                            .manufacturer(device.getManufacturer())
                            .os(device.getOs())
                            .trusted(userDevice.getTrusted())
                            .id(device.getId())
                            .lastLoginAt(userDevice.getLastLoginAt())
                            .build();
                    deviceResponses.add(deviceResponse);
                  });
              return deviceResponses;
            });
  }
}
