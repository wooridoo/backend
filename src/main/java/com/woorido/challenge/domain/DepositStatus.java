package com.woorido.challenge.domain;

public enum DepositStatus {
    NONE, // 없음
    LOCKED, // 락됨
    USED, // 사용됨 (차감됨)
    UNLOCKED // 해제됨
}
