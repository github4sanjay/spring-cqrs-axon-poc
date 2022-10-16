package com.example.auth.token.refresh;

import com.example.auth.AuthException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;

  public String on(RefreshTokenCommand.GenerateRefreshTokenCommand command) {
    var refreshToken =
        RefreshToken.builder()
            .amr(command.getAmr())
            .token(command.getToken())
            .createdAt(command.getCreatedAt())
            .deviceId(command.getDeviceId())
            .expireAt(command.getExpireAt())
            .id(command.getId())
            .subject(command.getSubject())
            .refreshChainExpiry(command.getRefreshChainExpiry())
            .build();
    refreshTokenRepository.save(refreshToken);
    return command.getId();
  }

  public String on(RefreshTokenCommand.RefreshRefreshTokenCommand command) {
    var refreshToken =
        refreshTokenRepository
            .findById(command.getId())
            .orElseThrow(AuthException.INVALID_TOKEN::getEx);
    if (refreshToken.getExpireAt().isBefore(Instant.now())
        || Instant.now()
            .minus(refreshToken.getRefreshChainExpiry())
            .isAfter(refreshToken.getCreatedAt())) {
      throw AuthException.EXPIRED_TOKEN.getEx();
    }

    refreshToken.setToken(command.getToken());
    refreshToken.setExpireAt(command.getExpireAt());
    refreshTokenRepository.save(refreshToken);
    return command.getId();
  }

  public String on(RefreshTokenCommand.DisableRefreshTokenCommand command) {
    var refreshToken =
        refreshTokenRepository
            .findById(command.getId())
            .orElseThrow(AuthException.INVALID_TOKEN::getEx);
    refreshTokenRepository.delete(refreshToken);
    return command.getId();
  }

  public RefreshToken on(RefreshTokenQuery.GetRefreshTokenByDeviceIdAndTokenQuery query) {
    return refreshTokenRepository
        .findByDeviceIdAndToken(query.getDeviceId(), query.getToken())
        .orElseThrow(AuthException.INVALID_TOKEN::getEx);
  }
}
