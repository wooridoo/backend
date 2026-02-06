package com.woorido.account.domain;

public enum TransactionType {
    CHARGE, // 충전
    WITHDRAW, // 출금
    LOCK, // 보증금 락
    UNLOCK, // 보증금 해제
    SUPPORT, // 서포트 납입
    ENTRY_FEE, // 입회비
    REFUND // 환불
}
