package com.example.demo.dto;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SignupRequest {
    private String email;
    private String password;
    private String nickname;
    private String phone;
    private String birthDate;
    private String name; // 실명 정책 반영
    private String verificationToken;
    private Boolean termsAgreed;
    private Boolean privacyAgreed;
    private Boolean marketingAgreed;
}