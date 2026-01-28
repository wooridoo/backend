package com.woorido.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NicknameCheckResponse {

    private String nickname;
    private Boolean isAvailable;

    public static NicknameCheckResponse available(String nickname) {
        return NicknameCheckResponse.builder()
                .nickname(nickname)
                .isAvailable(true)
                .build();
    }

    public static NicknameCheckResponse unavailable(String nickname) {
        return NicknameCheckResponse.builder()
                .nickname(nickname)
                .isAvailable(false)
                .build();
    }
}
