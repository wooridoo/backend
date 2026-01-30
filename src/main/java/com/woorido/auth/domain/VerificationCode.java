package com.woorido.auth.domain;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerificationCode {
    private String email;
    private String code;
    private String type; // SIGNUP | PASSWORD_RESET
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canRetry(int retryAfterSeconds) {
        return LocalDateTime.now().isAfter(createdAt.plusSeconds(retryAfterSeconds));
    }
}
