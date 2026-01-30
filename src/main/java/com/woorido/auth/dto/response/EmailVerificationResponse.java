package com.woorido.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailVerificationResponse {
    private String email;
    private Integer expiresIn; // 인증 코드 유효 시간 (초)
    private Integer retryAfter; // 재발송 가능 시간 (초)
}
