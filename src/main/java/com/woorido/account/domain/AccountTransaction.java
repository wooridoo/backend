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
public class AccountTransaction {
    private String id; // UUID
    private String accountId; // 계좌 ID
    private TransactionType type; // 거래 유형 (CHARGE, WITHDRAW, SUPPORT 등)
    private Long amount; // 금액
    private Long balanceBefore; // 거래 전 잔액
    private Long balanceAfter; // 거래 후 잔액
    private Long lockedBefore; // 거래 전 락 잔액
    private Long lockedAfter; // 거래 후 락 잔액
    private String idempotencyKey; // 중복 방지 키
    private String relatedChallengeId; // 관련 챌린지 ID
    private String relatedUserId; // 관련 사용자 ID
    private String description; // 설명
    private String pgProvider; // PG사
    private String pgTxId; // PG 거래 ID
    private LocalDateTime createdAt; // 생성일
}
