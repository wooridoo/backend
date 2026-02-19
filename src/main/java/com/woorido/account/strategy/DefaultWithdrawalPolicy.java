package com.woorido.account.strategy;

import org.springframework.stereotype.Component;

import com.woorido.account.domain.Account;

@Component
public class DefaultWithdrawalPolicy implements WithdrawalPolicyStrategy {

    private static final long DAILY_LIMIT = 5_000_000L;
    private static final long MONTHLY_LIMIT = 20_000_000L;

    @Override
    public void validate(Account account, long amount, long dailyTotal, long monthlyTotal) {
        if (account.getBalance() < amount) {
            throw new RuntimeException("ACCOUNT_003:Insufficient balance for withdrawal");
        }

        if (dailyTotal + amount > DAILY_LIMIT) {
            throw new RuntimeException("ACCOUNT_005:Daily withdrawal limit exceeded");
        }

        if (monthlyTotal + amount > MONTHLY_LIMIT) {
            throw new RuntimeException("ACCOUNT_006:Monthly withdrawal limit exceeded");
        }
    }

    @Override
    public long calculateFee(long amount) {
        return 0L;
    }

    @Override
    public long getDailyLimit() {
        return DAILY_LIMIT;
    }

    @Override
    public long getMonthlyLimit() {
        return MONTHLY_LIMIT;
    }
}