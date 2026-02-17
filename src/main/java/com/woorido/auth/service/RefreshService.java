package com.woorido.auth.service;

import com.woorido.auth.dto.response.RefreshResponse;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshService {
  // Learning note:
  // - Read flow as: validate auth/role -> execute domain logic -> persist via Mapper.

  private final JwtUtil jwtUtil;
  private final UserMapper userMapper;

  // [학습] 유효한 리프레시 토큰으로 액세스 토큰을 재발급한다.
  public RefreshResponse refresh(String refreshToken) {
    if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
      throw new RuntimeException("AUTH_004:리프레시 토큰이 유효하지 않습니다");
    }

    String userId = jwtUtil.getUserIdFromToken(refreshToken);
    User user = userMapper.findById(userId);
    if (user == null) {
      throw new RuntimeException("AUTH_004:리프레시 토큰이 유효하지 않습니다");
    }

    String newAccessToken = jwtUtil.generateAccessToken(userId, user.getEmail());
    String newRefreshToken = jwtUtil.generateRefreshToken(userId);
    log.debug("Refresh token reissued for userId={}", userId);

    return RefreshResponse.of(
        newAccessToken,
        newRefreshToken,
        (int) jwtUtil.getAccessTokenExpiration());
  }
}