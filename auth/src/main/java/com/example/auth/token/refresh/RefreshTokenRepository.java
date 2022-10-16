package com.example.auth.token.refresh;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
  List<RefreshToken> findByDeviceId(String deviceId);

  void deleteByToken(String refreshToken);

  void deleteAllBySubject(String accountId);

  default Optional<RefreshToken> findByDeviceIdAndToken(String deviceId, String token) {
    var refreshTokens = findByDeviceId(deviceId);
    for (var refreshToken : refreshTokens) {
      if (token.equals(refreshToken.getToken())) {
        return Optional.of(refreshToken);
      }
    }
    return Optional.empty();
  }

  default Optional<RefreshToken> findByDeviceIdAndSubject(String deviceId, String subject) {
    var refreshTokens = findByDeviceId(deviceId);
    for (var refreshToken : refreshTokens) {
      if (subject.equals(refreshToken.getSubject())) {
        return Optional.of(refreshToken);
      }
    }
    return Optional.empty();
  }

  default void deleteAllByDeviceIdAndSubject(String id, String subject) {
    var refreshTokens = findByDeviceId(id);
    var listToBeDeleted = new ArrayList<RefreshToken>();
    for (var refreshToken : refreshTokens) {
      if (subject.equals(refreshToken.getSubject())) {
        listToBeDeleted.add(refreshToken);
      }
    }
    deleteAll(listToBeDeleted);
  }
}
