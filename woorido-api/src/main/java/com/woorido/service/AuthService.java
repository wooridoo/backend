package com.woorido.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.woorido.dto.response.LoginResponse;
import com.woorido.dto.response.UserInfo;
import com.woorido.entity.User;
import com.woorido.mapper.UserMapper;
import com.woorido.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(String email, String password) {
        // 1. 이메일로 사용자 조회
        User user = userMapper.findByEmail(email);
        
        // === 디버그 로그 추가 ===
        System.out.println("========== DEBUG ==========");
        if (user == null) {
            System.out.println("user is NULL!");
        } else {
            System.out.println("id: " + user.getId());
            System.out.println("email: " + user.getEmail());
            System.out.println("passwordHash: " + user.getPasswordHash());
            System.out.println("accountStatus: " + user.getAccountStatus());
            System.out.println("createdAt: " + user.getCreatedAt());
            System.out.println("failedLoginAttempts: " + user.getFailedLoginAttempts());
        }
        System.out.println("===========================");
        // === 디버그 로그 끝 ===
        
        if (user == null) {
            throw new RuntimeException("AUTH_001:이메일 또는 비밀번호가 일치하지 않습니다");
        }

        // 2. 계정 잠금 확인
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("AUTH_002:계정이 잠겨있습니다");
        }

        // 3. 탈퇴 대기 상태 확인
        if ("WITHDRAWN".equals(user.getAccountStatus())) {
            throw new RuntimeException("USER_005:탈퇴 대기 상태입니다");
        }

        // 4. 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            userMapper.incrementFailedLoginAttempts(user.getId());
            
            // 5회 실패 시 계정 잠금 (30분)
            if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() >= 4) {
                userMapper.lockAccount(user.getId(), LocalDateTime.now().plusMinutes(30));
            }
            
            throw new RuntimeException("AUTH_001:이메일 또는 비밀번호가 일치하지 않습니다");
        }

        // 5. 로그인 성공 처리
        userMapper.resetFailedLoginAttempts(user.getId());
        userMapper.updateLastLoginAt(user.getId());

        // 6. JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 7. 신규 가입자 여부 (7일 이내)
        boolean isNewUser = user.getCreatedAt() != null && 
                user.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7));

        // 8. 응답 생성
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn((int) jwtUtil.getAccessTokenExpiration())
                .user(UserInfo.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .profileImage(user.getProfileImageUrl())
                        .status(user.getAccountStatus())
                        .isNewUser(isNewUser)
                        .build())
                .build();
    }
}
