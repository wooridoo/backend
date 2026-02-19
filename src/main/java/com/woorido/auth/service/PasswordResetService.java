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
            throw new RuntimeException("USER_001:사용자를 찾을 수 없습니다");
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

        log.info("비밀번호 재설정 토큰 발급 - userId: {}, expiresAt: {}", user.getId(), expiresAt);
        return PasswordResetResponse.of(email, RESET_TOKEN_EXPIRES_IN);
    }

    private void sendPasswordResetEmail(String email, String resetToken, LocalDateTime expiresAt) {
        String resetLink = buildResetLink(resetToken);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[Woorido] 비밀번호 재설정");
        message.setText(
                "비밀번호 재설정 요청이 접수되었습니다.\n\n"
                        + "재설정 링크: " + resetLink + "\n\n"
                        + "만료 시각: " + expiresAt + "\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("비밀번호 재설정 메일 전송 실패 - email: {}", email, e);
            throw new RuntimeException("AUTH_010:재설정 메일 전송에 실패했습니다");
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
            throw new RuntimeException("AUTH_009:유효하지 않은 재설정 토큰입니다");
        }

        if (user.getPasswordResetExpires() == null || user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("AUTH_009:재설정 토큰이 만료되었습니다");
        }

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new RuntimeException("VALIDATION_001:비밀번호가 일치하지 않습니다");
        }

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(user.getId(), encodedPassword);
        userMapper.clearPasswordResetToken(user.getId());

        log.info("비밀번호 재설정 완료 - userId: {}", user.getId());
        return PasswordResetExecuteResponse.success();
    }
}
