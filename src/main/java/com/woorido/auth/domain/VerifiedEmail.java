package com.woorido.auth.domain;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerifiedEmail {
    private String email;
    private String verificationToken;
    private String type; // SIGNUP | PASSWORD_RESET
    private LocalDateTime expiresAt; // 토큰 만료 시간

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
