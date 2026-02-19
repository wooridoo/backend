package com.woorido.auth.service;

import com.woorido.auth.dto.response.EmailConfirmResponse;
import com.woorido.auth.dto.response.EmailVerifyResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
  private static final int VERIFY_CODE_EXPIRES_IN = 600;
  private static final int VERIFICATION_TOKEN_EXPIRES_IN = 1800;
  private static final Map<String, VerificationCodeInfo> CODE_STORE = new ConcurrentHashMap<>();

  public EmailVerifyResponse issueVerifyCode(String email) {
    String code = String.format("%06d", (int) (Math.random() * 1_000_000));
    CODE_STORE.put(email, new VerificationCodeInfo(code, LocalDateTime.now().plusSeconds(VERIFY_CODE_EXPIRES_IN)));

    // 운영 환경에서는 실제 이메일 발송으로 대체한다.
    return EmailVerifyResponse.builder()
        .email(email)
        .expiresIn(VERIFY_CODE_EXPIRES_IN)
        .build();
  }

  public EmailConfirmResponse confirm(String email, String code) {
    VerificationCodeInfo info = CODE_STORE.get(email);
    if (info == null || info.expiresAt().isBefore(LocalDateTime.now())) {
      throw new RuntimeException("AUTH_007:인증 코드가 만료되었거나 존재하지 않습니다");
    }
    if (!info.code().equals(code)) {
      throw new RuntimeException("AUTH_007:인증 코드가 올바르지 않습니다");
    }

    CODE_STORE.remove(email);
    return EmailConfirmResponse.builder()
        .verificationToken(UUID.randomUUID().toString())
        .expiresIn(VERIFICATION_TOKEN_EXPIRES_IN)
        .build();
  }

  private record VerificationCodeInfo(String code, LocalDateTime expiresAt) {
  }
}
