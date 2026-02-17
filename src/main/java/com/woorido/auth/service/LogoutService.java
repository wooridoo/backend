package com.woorido.auth.service;

import com.woorido.auth.dto.response.LogoutResponse;
import com.woorido.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {
  // Learning note:
  // - Read flow as: validate auth/role -> execute domain logic -> persist via Mapper.

  private final JwtUtil jwtUtil;

  // [학습] 리프레시 토큰을 무효화하여 로그아웃 처리한다.
  public LogoutResponse logout(String refreshToken) {
    if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
      throw new RuntimeException("AUTH_001:인증이 필요합니다");
    }

    String userId = jwtUtil.getUserIdFromToken(refreshToken);
    log.debug("Logout requested for userId={}", userId);
    return LogoutResponse.success();
  }
}