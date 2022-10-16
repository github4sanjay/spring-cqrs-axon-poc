package com.example.auth.device.trust;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, String> {
  List<UserDevice> findByDeviceId(String deviceId);

  List<UserDevice> findAllByAccountId(String accountId);

  void deleteAllByAccountId(String ssoId);

  default UserDevice findByDeviceIdAndAccountId(String deviceId, String accountId) {
    var userDevices = findByDeviceId(deviceId);
    for (var userDevice : userDevices) {
      if (accountId.equals(userDevice.getAccountId())) {
        return userDevice;
      }
    }
    return null;
  }

  default List<UserDevice> findAllByAccountIdAndTrustedTrue(String accountId) {
    var userDevices = findAllByAccountId(accountId);
    var filteredDevices = new ArrayList<UserDevice>();
    for (var userDevice : userDevices) {
      if (userDevice.getTrusted() != null && userDevice.getTrusted()) {
        filteredDevices.add(userDevice);
      }
    }
    return filteredDevices;
  }

  default UserDevice findByDeviceIdAndBiometric(String id, String token) {
    var userDevices = findByDeviceId(id);
    for (var userDevice : userDevices) {
      if (token.equals(userDevice.getBiometric())) {
        return userDevice;
      }
    }
    return null;
  }
}
