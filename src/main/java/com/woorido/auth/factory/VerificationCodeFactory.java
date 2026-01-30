package com.woorido.auth.factory;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.woorido.auth.domain.VerificationCode;

/**
 * 인증 코드 생성 Factory
 * Factory Pattern 적용
 */
@Component
public class VerificationCodeFactory {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRES_IN_SECONDS = 300; // 5분

    /**
     * 6자리 숫자 인증 코드 생성
     */
    public VerificationCode create(String email, String type) {
        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();

        return VerificationCode.builder()
                .email(email)
                .code(code)
                .type(type)
                .createdAt(now)
                .expiresAt(now.plusSeconds(EXPIRES_IN_SECONDS))
                .build();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public int getExpiresInSeconds() {
        return EXPIRES_IN_SECONDS;
    }
}
