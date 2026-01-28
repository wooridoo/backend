package com.woorido.auth.service;

import org.springframework.stereotype.Service;

import com.woorido.auth.dto.response.LogoutResponse;
import com.woorido.common.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final JwtUtil jwtUtil;

    /**
     * 로그아웃 처리
     * - 리프레시 토큰 유효성 검증
     * - 실제 운영환경에서는 토큰을 블랙리스트에 추가하거나 DB에서 삭제하는 로직 필요
     */
    public LogoutResponse logout(String refreshToken) {
        // 1. 리프레시 토큰 유효성 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        // 2. 토큰에서 사용자 ID 추출 (로그 목적)
        String userId = jwtUtil.getUserIdFromToken(refreshToken);
        System.out.println("========== LOGOUT 처리 ==========");
        System.out.println("userId: " + userId);
        System.out.println("==================================");

        // 3. 로그아웃 성공 응답
        // Note: 실제 운영환경에서는 여기서 리프레시 토큰을
        // 블랙리스트에 추가하거나 DB에서 삭제해야 합니다.
        return LogoutResponse.success();
    }
}
