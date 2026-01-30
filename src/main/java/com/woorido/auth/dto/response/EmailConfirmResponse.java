package com.woorido.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailConfirmResponse {
    private String email;
    private Boolean verified;
    private String verificationToken; // 회원가입/재설정용 임시 토큰
}
