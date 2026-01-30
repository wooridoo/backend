package com.woorido.auth.strategy;

import org.springframework.stereotype.Component;

/**
 * Mock 이메일 발송 (콘솔 출력)
 * 개발/테스트용
 */
@Component
public class MockEmailSender implements EmailSenderStrategy {

    @Override
    public void sendVerificationCode(String email, String code, String type) {
        System.out.println("========== 이메일 인증 코드 발송 (Mock) ==========");
        System.out.println("수신자: " + email);
        System.out.println("인증 코드: " + code);
        System.out.println("발송 목적: " + type);
        System.out.println("================================================");
    }
}
