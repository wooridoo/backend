package com.woorido.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LogoutResponse {

    private boolean isLoggedOut;

    public static LogoutResponse success() {
        return LogoutResponse.builder()
                .isLoggedOut(true)
                .build();
    }
}
