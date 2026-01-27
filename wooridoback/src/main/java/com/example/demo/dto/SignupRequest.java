package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SignupRequest {
    private String email;          // 이메일 주소
    private String password;       // 비밀번호 (8-20자)
    private String nickname;       // 닉네임 (2-20자)
    private String phone;          // 휴대폰 번호
    private String birthDate;      // 생년월일 (YYYY-MM-DD)
    private String verificationToken; // 이메일 인증 토큰
    private boolean termsAgreed;   // 서비스 이용약관 동의
    private boolean privacyAgreed; // 개인정보 처리방침 동의
    private boolean marketingAgreed; // 마케팅 수신 동의 (선택)
}