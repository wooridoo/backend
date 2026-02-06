package com.woorido.sns.domain;

public enum PaymentAction {
    REQUEST, // 요청
    SUCCESS, // 성공
    FAIL, // 실패
    RETRY // 재시도
}
