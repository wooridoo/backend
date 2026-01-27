package com.example.demo.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class SignupResponse {
    private Long userId;     // 사용자 ID
    private String email;    // 이메일
    private String nickname; // 닉네임
    private String status;   // 상태 (ACTIVE)
    private String createdAt; // 가입일시
}