package com.woorido.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import com.woorido.common.entity.User;

@Getter
@Builder
public class SignupResponse {

    private String userId;
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
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }
}
