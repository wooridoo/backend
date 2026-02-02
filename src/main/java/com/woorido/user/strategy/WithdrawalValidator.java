package com.woorido.user.strategy;

import org.springframework.stereotype.Component;

import com.woorido.account.domain.Account;
import com.woorido.account.repository.AccountMapper;
import com.woorido.common.mapper.ChallengeMemberMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WithdrawalValidator {

    private final ChallengeMemberMapper challengeMemberMapper;
    private final AccountMapper accountMapper;

    public void validateCanWithdraw(String userId) {
        // 1. 활성 챌린지 참여 여부 체크
        // challenge_members 테이블에서 left_at이 NULL인 레코드 확인
        int activeChallengeCount = challengeMemberMapper.countActiveMembershipsByUserId(userId);
        if (activeChallengeCount > 0) {
            throw new RuntimeException(String.format("USER_008:참여 중인 챌린지가 %d개 있습니다. 먼저 탈퇴해주세요", activeChallengeCount));
        }

        // 2. 미정산 크레딧(잔액) 체크
        // accounts 테이블에서 balance 확인
        Account account = accountMapper.findByUserId(userId);
        if (account != null && account.getBalance() > 0) {
            throw new RuntimeException(
                    String.format("USER_009:미정산 크레딧이 %d원 남아있습니다. 정산 후 탈퇴해주세요", account.getBalance()));
        }
    }
}
