package com.woorido.auth.social;

public enum SocialAuthIntent {
    LOGIN,
    SIGNUP;

    public static SocialAuthIntent from(String rawValue) {
        if (rawValue == null) {
            throw new RuntimeException("AUTH_012:유효하지 않은 소셜 인증 요청입니다");
        }
        try {
            return SocialAuthIntent.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("AUTH_012:유효하지 않은 소셜 인증 요청입니다");
        }
    }
}

