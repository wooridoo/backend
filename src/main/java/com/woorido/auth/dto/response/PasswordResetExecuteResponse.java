package com.woorido.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetExecuteResponse {
    private boolean passwordReset;

    public static PasswordResetExecuteResponse success() {
        return PasswordResetExecuteResponse.builder()
                .passwordReset(true)
                .build();
    }
}
