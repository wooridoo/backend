package com.woorido.account.dto.response;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        private String bankName;
        private String accountNumber;
        private String accountHolder;
        private Boolean isVerified;
    }

    public static MyAccountResponse from(
            Account account,
            long dailyWithdrawLimit,
            long monthlyWithdrawLimit,
            long usedToday,
            long usedThisMonth) {

        LinkedBankAccount linkedInfo = null;
        if (account.getBankCode() != null) {
            linkedInfo = LinkedBankAccount.builder()
                    .bankCode(account.getBankCode())
                    .bankName(getBankName(account.getBankCode()))
                    .accountNumber(maskAccountNumber(account.getAccountNumber()))
                    .accountHolder(account.getAccountHolder())
                    .isVerified(Boolean.TRUE)
                    .build();
        }

        Limits limitsInfo = Limits.builder()
                .dailyWithdrawLimit(dailyWithdrawLimit)
                .monthlyWithdrawLimit(monthlyWithdrawLimit)
                .usedToday(usedToday)
                .usedThisMonth(usedThisMonth)
                .build();

        long totalBalance = account.getBalance() != null ? account.getBalance() : 0L;
        long locked = account.getLockedBalance() != null ? account.getLockedBalance() : 0L;
        long available = Math.max(0L, totalBalance - locked);

        return MyAccountResponse.builder()
                .accountId(account.getId())
                .userId(account.getUserId())
                .balance(totalBalance)
                .availableBalance(available)
                .lockedBalance(locked)
                .limits(limitsInfo)
                .linkedBankAccount(linkedInfo)
                .createdAt(account.getCreatedAt() != null ? account.getCreatedAt().toString() : null)
                .build();
    }

    private static String getBankName(String code) {
        if ("088".equals(code)) {
            return "신한은행";
        }
        if ("004".equals(code)) {
            return "국민은행";
        }
        if ("020".equals(code)) {
            return "우리은행";
        }
        return "기타은행";
    }

    private static String maskAccountNumber(String number) {
        if (number == null || number.isBlank()) {
            return number;
        }

        String trimmed = number.trim();
        Matcher dashedPattern = Pattern.compile("^(\\d{3})-(\\d{3})-(\\d{3,})$").matcher(trimmed);
        if (dashedPattern.matches()) {
            String tail = dashedPattern.group(3);
            String tailVisible = tail.substring(Math.max(0, tail.length() - 3));
            return dashedPattern.group(1) + "-***-***" + tailVisible;
        }

        String digitsOnly = trimmed.replaceAll("\\D", "");
        if (digitsOnly.length() < 7) {
            return trimmed;
        }

        String prefix = digitsOnly.substring(0, 3);
        String suffix = digitsOnly.substring(digitsOnly.length() - 3);
        return prefix + "***" + suffix;
    }
}
