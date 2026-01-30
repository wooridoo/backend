package com.woorido.auth.strategy;

/**
 * 이메일 발송 전략 인터페이스
 * Strategy Pattern 적용
 */
public interface EmailSenderStrategy {

    /**
     * 인증 코드를 이메일로 발송
     * 
     * @param email 수신자 이메일
     * @param code  인증 코드
     * @param type  발송 목적 (SIGNUP, PASSWORD_RESET)
     */
    void sendVerificationCode(String email, String code, String type);
}
