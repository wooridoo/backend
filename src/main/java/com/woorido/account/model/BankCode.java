package com.woorido.account.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BankCode {
    KB("004", "국민은행"),
    SHINHAN("088", "신한은행"),
    WOORI("020", "우리은행"),
    HANA("081", "하나은행"),
    KAKAO("090", "카카오뱅크"),
    TOSS("092", "토스뱅크"),
    NH("011", "농협은행");

    private final String code;
    private final String name;

    public static String getNameByCode(String code) {
        for (BankCode bank : values()) {
            if (bank.code.equals(code)) {
                return bank.name;
            }
        }
        return "Unknown Bank";
    }
}
