package com.woorido.user.strategy;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,20}$";
    private static final Pattern PATTERN = Pattern.compile(PASSWORD_PATTERN);

    public void validate(String newPassword, String newPasswordConfirm) {
        // 1. 형식 검증
        if (newPassword == null || !PATTERN.matcher(newPassword).matches()) {
            throw new RuntimeException("VALIDATION_001:비밀번호 형식이 올바르지 않습니다");
        }

        // 2. 일치 여부 검증
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new RuntimeException("VALIDATION_001:새 비밀번호가 일치하지 않습니다");
        }
    }
}
