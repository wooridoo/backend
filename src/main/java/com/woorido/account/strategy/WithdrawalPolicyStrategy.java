package com.woorido.account.strategy;

import com.woorido.account.domain.Account;

public interface WithdrawalPolicyStrategy {
    void validate(Account account, long amount, long dailyTotal, long monthlyTotal);

    long calculateFee(long amount);
}
