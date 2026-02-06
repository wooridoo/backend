package com.woorido.expense.domain;

public enum ExpenseRequestStatus {
    VOTING, // 투표중
    APPROVED, // 승인
    REJECTED, // 거절
    USED, // 사용됨
    EXPIRED, // 만료
    CANCELLED // 취소
}
