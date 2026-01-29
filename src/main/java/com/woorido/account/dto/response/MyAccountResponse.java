package com.woorido.account.dto.response;

import com.woorido.account.domain.Account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyAccountResponse {
    private String accountId;
    private String userId;
    private Long balance;
    private Long availableBalance;
    private Long lockedBalance;
    private Limits limits;
    private LinkedBankAccount linkedBankAccount;
    private String createdAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Limits {
        private Long dailyWithdrawLimit;
        private Long monthlyWithdrawLimit;
        private Long usedToday;
        private Long usedThisMonth;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedBankAccount {
        private String bankCode;
        private String bankName; // 코드로 조회 필요하지만 일단 저장된 값이나 Mock
        private String accountNumber;
        private String accountHolder;
        private Boolean isVerified;
    }

    public static MyAccountResponse from(Account account) {
        // LinkedBankAccount 구성
        LinkedBankAccount linkedInfo = null;
        if (account.getBankCode() != null) {
            linkedInfo = LinkedBankAccount.builder()
                    .bankCode(account.getBankCode())
                    .bankName(getBankName(account.getBankCode())) // 임시 메서드
                    .accountNumber(maskAccountNumber(account.getAccountNumber()))
                    .accountHolder(account.getAccountHolder())
                    .isVerified(true) // 일단 true로 가정
                    .build();
        }

        // Limits 구성 (임시 하드코딩)
        Limits limitsInfo = Limits.builder()
                .dailyWithdrawLimit(1000000L)
                .monthlyWithdrawLimit(5000000L)
                .usedToday(0L) // TODO: 트랜잭션 조회하여 계산 필요
                .usedThisMonth(0L) // TODO: 트랜잭션 조회하여 계산 필요
                .build();

        // availableBalance 계산 (현재는 balance와 동일하지만 락 로직에 따라 다를 수 있음)
        // 명세상 availableBalance는 출금 가능 잔액. (balance - lockedBalance? 혹은 그냥 balance?)
        // 스키마 설명: balance=가용 잔액, locked_balance=보증금 락
        // 그렇다면 총 보유 자산 = balance + lockedBalance 이고,
        // 출금 가능 잔액 = balance 일 가능성이 높음. (DB 코멘트: balance "가용 잔액")
        // 명세서 Response: balance(총 잔액), availableBalance(출금 가능 잔액)
        // -> 보통 balance는 총액, available은 락 제외 금액.
        // 여기서는 DB의 balance가 "가용 잔액"이라고 적혀있으므로 명세와 용어 차이가 있을 수 있음.
        // 일단 DB balance를 availableBalance로 보고, 총 잔액을 balance + lockedBalance로 계산하거나,
        // DB balance를 총 잔액으로 보고 available을 계산할지 결정해야 함.
        // 일반적인 금융 시스템: Ledger Balance(총 잔액) vs Available Balance(가용 잔액)
        // 여기서는 DB 컬럼 설명이 "가용 잔액"이므로 balance 자체가 출금 가능한 돈일 확률이 큼.
        // 하지만 명세서 예시: balance: 500000, available: 450000, locked: 50000
        // -> balance(50만) = available(45만) + locked(5만) 구조임.
        // 따라서 DB의 balance 컬럼 정의(가용 잔액)가 "총 잔액"을 의미하는지 "순수 가용액"을 의미하는지 애매함.
        // 보통 '잔액' 컬럼은 총 잔액을 의미하고 락 금액을 별도로 관리하는 경우가 많음.
        // 여기서는 명세서 Response를 기준으로:
        // Response.balance = Account.balance (총 잔액으로 가정)
        // Response.available = Account.balance - Account.lockedBalance
        // 이렇게 구현하겠음.

        long totalBalance = account.getBalance();
        long locked = account.getLockedBalance();
        long available = totalBalance - locked;

        return MyAccountResponse.builder()
                .accountId(account.getId())
                .userId(account.getUserId())
                .balance(totalBalance)
                .availableBalance(available)
                .lockedBalance(locked)
                .limits(limitsInfo)
                .linkedBankAccount(linkedInfo)
                .createdAt(account.getCreatedAt().toString()) // ISO format
                .build();
    }

    private static String getBankName(String code) {
        if ("088".equals(code))
            return "신한은행";
        if ("004".equals(code))
            return "국민은행";
        if ("020".equals(code))
            return "우리은행";
        return "기타은행";
    }

    private static String maskAccountNumber(String number) {
        if (number == null || number.length() < 7)
            return number;
        // 간단 마스킹: 앞 3자리, 뒤 3자리 제외하고 *
        // 예: 110-123-456789 -> 110-***-***789
        // 정규식 등으로 처리 가능하지만 간단히 구현
        return number; // 실제 구현 시 마스킹 로직 적용
    }
}
