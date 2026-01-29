package com.woorido.account.strategy;

import org.springframework.stereotype.Component;

import com.woorido.account.domain.Account;

@Component
public class DefaultWithdrawalPolicy implements WithdrawalPolicyStrategy {

    private static final long DAILY_LIMIT = 5_000_000L;
    private static final long MONTHLY_LIMIT = 20_000_000L;

    @Override
    public void validate(Account account, long amount, long dailyTotal, long monthlyTotal) {
        // 1. 잔액 체크 (잔액 < 출금액)
        if (account.getBalance() < amount) {
            throw new RuntimeException("ACCOUNT_003:출금 가능 금액을 초과했습니다");
        }

        // 2. 일일 한도 체크
        if (dailyTotal + amount > DAILY_LIMIT) {
            throw new RuntimeException("ACCOUNT_005:일일 출금 한도를 초과했습니다");
        }

        // 3. 월간 한도 체크
        if (monthlyTotal + amount > MONTHLY_LIMIT) {
            throw new RuntimeException("ACCOUNT_006:월간 출금 한도를 초과했습니다");
        }
    }

    @Override
    public long calculateFee(long amount) {
        return 0L; // 수수료 무료 정책
    }
}
