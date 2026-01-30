package com.woorido.account.factory;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.woorido.account.domain.AccountTransaction;
import com.woorido.account.domain.TransactionType;
import com.woorido.account.dto.request.WithdrawRequest;

@Component
public class AccountTransactionFactory {

    public AccountTransaction createWithdrawTransaction(String accountId, WithdrawRequest request, long balanceBefore,
            long balanceAfter) {
        return AccountTransaction.builder()
                .id(UUID.randomUUID().toString())
                .accountId(accountId)
                .type(TransactionType.WITHDRAW)
                .amount(-request.getAmount()) // 출금은 음수로 기록 (잔액 계산은 별도지만, 거래내역상 입출금 구분 위해 +,- 사용? 보통 Type으로 구분하고 Amount는
                                              // 절대값 or 부호. DB_REFERENCE를 보면 +/- 라고 되어있음)
                // DB_REFERENCE says: AMOUNT Number(19) (+/-). So Withdraw should be negative.
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .lockedBefore(0L) // 단순 출금은 락 변경 없음 (가정)
                .lockedAfter(0L)
                .description(request.getBankCode() + " " + request.getAccountNumber()) // 은행명+계좌번호
                .createdAt(LocalDateTime.now())
                .build();
    }

    public AccountTransaction createSupportTransaction(String accountId, String challengeId, long amount,
            long balanceBefore, long balanceAfter) {
        return AccountTransaction.builder()
                .id(UUID.randomUUID().toString())
                .accountId(accountId)
                .type(TransactionType.SUPPORT) // SUPPORT type
                .amount(-amount) // 출금이므로 음수
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .lockedBefore(0L)
                .lockedAfter(0L)
                .relatedChallengeId(challengeId)
                .description("서포트 납입")
                .createdAt(LocalDateTime.now())
                .build();
    }

    public AccountTransaction createLockTransaction(String accountId, long amount,
            long balanceBefore, long balanceAfter, long lockedBefore, long lockedAfter,
            String challengeId, String description) {
        return AccountTransaction.builder()
                .id(UUID.randomUUID().toString())
                .accountId(accountId)
                .type(TransactionType.LOCK)
                .amount(-amount) // 잔액에서 차감되므로 음수
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .lockedBefore(lockedBefore)
                .lockedAfter(lockedAfter)
                .relatedChallengeId(challengeId)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
