package com.woorido.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.woorido.dto.response.PasswordResetResponse;
import com.woorido.entity.User;
import com.woorido.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private final UserMapper userMapper;

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
}
