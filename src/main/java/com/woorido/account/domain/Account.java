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
public class Account {
    private String id; // UUID
    private String userId; // 사용자 ID (UUID)
    private Long balance; // 가용 잔액
    private Long lockedBalance; // 보증금 락
    private String bankCode; // 출금 은행 코드
    private String accountNumber; // 출금 계좌번호
    private String accountHolder; // 예금주
    private Integer version; // 동시성 제어
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
