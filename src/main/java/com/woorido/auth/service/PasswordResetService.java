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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
  // Learning note:
  // - Read flow as: validate auth/role -> execute domain logic -> persist via Mapper.

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // 비밀번호 재설정 토큰 만료 시간 (30분 = 1800초)
    private static final int RESET_TOKEN_EXPIRES_IN = 1800;

    /**
     * 비밀번호 재설정 요청을 처리한다.
     * - 이메일로 사용자 존재 여부를 확인한다.
     * - 재설정 토큰과 만료 시각을 생성한다.
     * - 토큰 정보를 DB에 저장한다.
     */
    // [학습] 비밀번호 재설정 토큰을 발급한다.
    public PasswordResetResponse requestPasswordReset(String email) {
        // 1. 이메일로 사용자 조회
        User user = userMapper.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("USER_001:사용자를 찾을 수 없습니다");
        }

        log.info("Password reset requested - userId: {}, email: {}", user.getId(), email);

        // 2. 재설정 토큰 생성
        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(RESET_TOKEN_EXPIRES_IN);

        // 3. 토큰 정보를 DB에 저장
        userMapper.updatePasswordResetToken(user.getId(), resetToken, expiresAt);

        // 4. TODO: 실제 메일 발송 연동 시 이메일 전송
        // emailService.sendPasswordResetEmail(email, resetToken);
        log.info("Password reset token issued - userId: {}, expiresAt: {}", user.getId(), expiresAt);

        // 5. 응답 생성
        return PasswordResetResponse.of(email, RESET_TOKEN_EXPIRES_IN);
    }

    /**
     * 비밀번호를 재설정한다.
     * - 토큰 유효성 검증
     * - 새 비밀번호 일치 여부 확인
     * - 비밀번호 업데이트 및 토큰 제거
     */
    @Transactional
    // [학습] 재설정 토큰 검증 후 비밀번호를 변경한다.
    public PasswordResetExecuteResponse resetPassword(PasswordResetExecuteRequest request) {
        // 1. 토큰으로 사용자 조회 및 유효성 검증
        User user = userMapper.findByPasswordResetToken(request.getToken());

        if (user == null) {
            throw new RuntimeException("AUTH_009:Invalid reset token");
        }

        if (user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("AUTH_009:Reset token has expired");
        }

        // 2. 새 비밀번호 확인
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new RuntimeException("VALIDATION_001:비밀번호가 일치하지 않습니다");
        }

        // 3. 새 비밀번호 인코딩
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        // 4. 비밀번호 업데이트 후 재설정 토큰 제거
        userMapper.updatePassword(user.getId(), encodedPassword);
        userMapper.clearPasswordResetToken(user.getId());

        log.info("Password reset completed - userId: {}", user.getId());

        return PasswordResetExecuteResponse.success();
    }
}
