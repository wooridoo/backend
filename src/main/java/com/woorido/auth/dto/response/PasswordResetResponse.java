package com.woorido.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PasswordResetResponse {

    private String email;
    private int expiresIn;

    public static PasswordResetResponse of(String email, int expiresIn) {
        return PasswordResetResponse.builder()
                .email(email)
                .expiresIn(expiresIn)
                .build();
    }
}
