package com.example.demo.dto;

import lombok.Builder;
import lombok.Getter;
import com.example.demo.entity.User;

@Getter @Builder
public class SignupResponse {
	private String userId; // UUID String
    private String email;
    private String nickname;
    private String status;
    private String createdAt;
    
    public static SignupResponse from(User user) {
        return SignupResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt().toString())
                .build();
    }
}
