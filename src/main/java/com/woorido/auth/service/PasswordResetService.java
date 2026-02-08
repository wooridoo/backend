package com.woorido.auth.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woorido.auth.dto.request.PasswordResetExecuteRequest;
import com.woorido.auth.dto.response.PasswordResetExecuteResponse;
import com.woorido.auth.dto.response.PasswordResetResponse;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // 비밀번호 재설정 링크 유효 시간 (30분 = 1800초)
    private static final int RESET_TOKEN_EXPIRES_IN = 1800;

    /**
     * 비밀번호 재설정 요청 처리
     * - 이메일로 사용자 존재 확인
     * - 재설정 토큰 생성 및 저장
     * - 실제 운영환경에서는 이메일 발송 로직 필요
     */
    public PasswordResetResponse requestPasswordReset(String email) {
        // 1. 이메일로 사용자 조회
        User user = userMapper.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("USER_001:사용자를 찾을 수 없습니다");
        }

        System.out.println("========== PASSWORD RESET 요청 ==========");
        System.out.println("userId: " + user.getId());
        System.out.println("email: " + email);
        System.out.println("==========================================");

        // 2. 재설정 토큰 생성
        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(RESET_TOKEN_EXPIRES_IN);

        // 3. 토큰을 DB에 저장
        userMapper.updatePasswordResetToken(user.getId(), resetToken, expiresAt);

        // 4. TODO: 실제 운영환경에서는 이메일 발송
        // emailService.sendPasswordResetEmail(email, resetToken);
        System.out.println("재설정 토큰: " + resetToken);
        System.out.println("만료 시간: " + expiresAt);

        // 5. 응답 생성
        return PasswordResetResponse.of(email, RESET_TOKEN_EXPIRES_IN);
    }

    /**
     * 비밀번호 재설정 실행
     * - 토큰 유효성 검증
     * - 새 비밀번호 암호화 및 업데이트
     * - 토큰 만료 처리
     */
    @Transactional
    public PasswordResetExecuteResponse resetPassword(PasswordResetExecuteRequest request) {
        // 1. 토큰으로 사용자 조회 및 유효성 검증
        User user = userMapper.findByPasswordResetToken(request.getToken());

        if (user == null) {
            throw new RuntimeException("AUTH_009:유효하지 않은 토큰입니다");
        }

        if (user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("AUTH_009:재설정 토큰이 만료되었습니다");
        }

        // 2. 비밀번호 확인
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new RuntimeException("VALIDATION_001:비밀번호가 일치하지 않습니다");
        }

        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        // 4. 비밀번호 업데이트 및 토큰 초기화
        userMapper.updatePassword(user.getId(), encodedPassword);
        userMapper.clearPasswordResetToken(user.getId());

        System.out.println("========== PASSWORD RESET SUCCESS ==========");
        System.out.println("userId: " + user.getId());

        return PasswordResetExecuteResponse.success();
    }
}
