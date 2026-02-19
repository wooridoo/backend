package com.woorido.auth.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    private static final int RESET_TOKEN_EXPIRES_IN = 1800;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.auth.password-reset-path:/reset-password}")
    private String passwordResetPath;

    @Value("${app.mail.from:no-reply@woorido.com}")
    private String fromAddress;

    public PasswordResetResponse requestPasswordReset(String email) {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("USER_001:User not found");
        }

        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(RESET_TOKEN_EXPIRES_IN);

        userMapper.updatePasswordResetToken(user.getId(), resetToken, expiresAt);

        try {
            sendPasswordResetEmail(email, resetToken, expiresAt);
        } catch (RuntimeException e) {
            userMapper.clearPasswordResetToken(user.getId());
            throw e;
        }

        log.info("Password reset token issued - userId: {}, expiresAt: {}", user.getId(), expiresAt);
        return PasswordResetResponse.of(email, RESET_TOKEN_EXPIRES_IN);
    }

    private void sendPasswordResetEmail(String email, String resetToken, LocalDateTime expiresAt) {
        String resetLink = buildResetLink(resetToken);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[Woorido] Password Reset");
        message.setText(
                "We received a request to reset your password.\n\n"
                        + "Reset link: " + resetLink + "\n\n"
                        + "This link will expire at: " + expiresAt + "\n"
                        + "If you did not request this, you can ignore this email.");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send password reset email - email: {}", email, e);
            throw new RuntimeException("AUTH_010:Failed to send reset email");
        }
    }

    private String buildResetLink(String token) {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String normalizedBaseUrl = normalizeBaseUrl(frontendBaseUrl);
        String normalizedPath = passwordResetPath.startsWith("/") ? passwordResetPath : "/" + passwordResetPath;
        return normalizedBaseUrl + normalizedPath + "?token=" + encodedToken;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:5173";
        }
        return baseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public PasswordResetExecuteResponse resetPassword(PasswordResetExecuteRequest request) {
        User user = userMapper.findByPasswordResetToken(request.getToken());
        if (user == null) {
            throw new RuntimeException("AUTH_009:Invalid reset token");
        }

        if (user.getPasswordResetExpires() == null || user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("AUTH_009:Reset token has expired");
        }

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new RuntimeException("VALIDATION_001:Passwords do not match");
        }

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(user.getId(), encodedPassword);
        userMapper.clearPasswordResetToken(user.getId());

        log.info("Password reset completed - userId: {}", user.getId());
        return PasswordResetExecuteResponse.success();
    }
}