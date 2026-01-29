package com.woorido.account.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    private String id; // UUID, used as orderId
    private String userId; // 사용자 ID
    private String sessionType; // 세션 타입 ("CHARGE" 등)
    private String returnUrl; // 콜백 URL (금액 정보 포함 예정)
    private String isUsed; // 사용 여부 ("N"/"Y")
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
