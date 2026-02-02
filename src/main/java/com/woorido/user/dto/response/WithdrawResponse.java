package com.woorido.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WithdrawResponse {
    private Long userId;
    private String status; // WITHDRAWN
    private String withdrawnAt; // 탈퇴 처리 일시
    private String dataDeletedAt; // 데이터 완전 삭제 예정일 (30일 후)
}
