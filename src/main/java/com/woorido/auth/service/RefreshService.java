package com.woorido.auth.service;

import org.springframework.stereotype.Service;

import com.woorido.auth.dto.response.RefreshResponse;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.common.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshService {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    /**
     * 토큰 갱신 처리
     * - 리프레시 토큰 유효성 검증
     * - 새로운 액세스 토큰 및 리프레시 토큰 발급
     */
    public RefreshResponse refresh(String refreshToken) {
        // 1. 리프레시 토큰 유효성 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("AUTH_004:리프레시 토큰이 만료되었습니다");
        }

        // 2. 토큰에서 사용자 ID 추출
        String userId = jwtUtil.getUserIdFromToken(refreshToken);

        System.out.println("========== TOKEN REFRESH 처리 ==========");
        System.out.println("userId: " + userId);
        System.out.println("========================================");

        // 3. 사용자 정보 조회 (이메일 가져오기 위함)
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("AUTH_004:리프레시 토큰이 만료되었습니다");
        }

        // 4. 새로운 토큰 발급
        String newAccessToken = jwtUtil.generateAccessToken(userId, user.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        // 5. 응답 생성
        return RefreshResponse.of(
                newAccessToken,
                newRefreshToken,
                (int) jwtUtil.getAccessTokenExpiration());
    }
}
