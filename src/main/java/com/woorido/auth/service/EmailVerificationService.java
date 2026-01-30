package com.woorido.auth.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.woorido.auth.domain.VerificationCode;
import com.woorido.auth.domain.VerifiedEmail;
import com.woorido.auth.dto.request.EmailConfirmRequest;
import com.woorido.auth.dto.request.EmailVerificationRequest;
import com.woorido.auth.dto.response.EmailConfirmResponse;
import com.woorido.auth.dto.response.EmailVerificationResponse;
import com.woorido.auth.factory.VerificationCodeFactory;
import com.woorido.auth.repository.VerificationCodeStore;
import com.woorido.auth.repository.VerifiedEmailStore;
import com.woorido.auth.strategy.EmailSenderStrategy;
import com.woorido.common.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int RETRY_AFTER_SECONDS = 60; // 1분

    private final VerificationCodeStore codeStore;
    private final VerifiedEmailStore verifiedEmailStore;
    private final VerificationCodeFactory codeFactory;
    private final EmailSenderStrategy emailSender;
    private final UserMapper userMapper;

    public EmailVerificationResponse sendVerificationCode(EmailVerificationRequest request) {
        String email = request.getEmail();
        String type = request.getType();

        log.info("EMAIL VERIFY 요청 - email: {}, type: {}", email, type);

        // 1. type 검증
        if (!"SIGNUP".equals(type) && !"PASSWORD_RESET".equals(type)) {
            throw new IllegalArgumentException("type은 SIGNUP 또는 PASSWORD_RESET이어야 합니다");
        }

        // 2. SIGNUP인 경우, 이미 가입된 이메일인지 확인
        if ("SIGNUP".equals(type)) {
            int existingCount = userMapper.countByEmail(email);
            if (existingCount > 0) {
                throw new RuntimeException("USER_002:이미 존재하는 이메일입니다");
            }
        }

        // 3. 재발송 제한 체크 (1분 이내 재요청 방지)
        Optional<VerificationCode> existingCode = codeStore.findByEmail(email);
        if (existingCode.isPresent() && !existingCode.get().canRetry(RETRY_AFTER_SECONDS)) {
            throw new RuntimeException("AUTH_007:잠시 후 다시 시도해주세요");
        }

        // 4. 인증 코드 생성 (Factory 패턴)
        VerificationCode newCode = codeFactory.create(email, type);

        // 5. 저장
        codeStore.save(newCode);

        // 6. 이메일 발송 (Strategy 패턴)
        emailSender.sendVerificationCode(email, newCode.getCode(), type);

        // 7. 응답 생성
        return EmailVerificationResponse.builder()
                .email(email)
                .expiresIn(codeFactory.getExpiresInSeconds())
                .retryAfter(RETRY_AFTER_SECONDS)
                .build();
    }

    public EmailConfirmResponse confirmVerificationCode(EmailConfirmRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        log.info("EMAIL CONFIRM 요청 - email: {}, code: {}", email, code);

        // 1. 저장된 코드 조회
        VerificationCode savedCode = codeStore.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("AUTH_006:인증 코드가 일치하지 않습니다")); // 없으면 불일치로 간주

        // 2. 코드 및 만료 검증
        if (savedCode.isExpired()) {
            throw new RuntimeException("AUTH_008:인증 코드가 만료되었습니다");
        }
        if (!savedCode.getCode().equals(code)) {
            throw new RuntimeException("AUTH_006:인증 코드가 일치하지 않습니다");
        }

        // 3. 검증 성공 -> 임시 토큰 발급
        String verificationToken = java.util.UUID.randomUUID().toString();
        VerifiedEmail verifiedEmail = VerifiedEmail.builder()
                .email(email)
                .verificationToken(verificationToken)
                .type(savedCode.getType())
                // 토큰 유효기간: 5분 (회원가입/재설정 완료까지 시간)
                .expiresAt(java.time.LocalDateTime.now().plusMinutes(5))
                .build();

        // 4. 저장 (VerifiedEmailStore) 및 기존 코드 삭제
        verifiedEmailStore.save(verifiedEmail);
        codeStore.remove(email); // 인증 완료된 코드는 삭제 (재사용 방지)

        log.info("검증 성공! Token: {}", verificationToken);

        // 5. 응답 생성
        return EmailConfirmResponse.builder()
                .email(email)
                .verified(true)
                .verificationToken(verificationToken)
                .build();
    }
}
