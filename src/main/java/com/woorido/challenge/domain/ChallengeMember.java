package com.woorido.challenge.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeMember {
    private String id; // 멤버십 ID (UUID)
    private String challengeId;
    private String userId;
    private ChallengeRole role; // LEADER, FOLLOWER
    private DepositStatus depositStatus; // NONE, LOCKED, USED, UNLOCKED
    private LocalDateTime depositLockedAt;
    private LocalDateTime depositUnlockedAt;
    private Long entryFeeAmount;
    private LocalDateTime entryFeePaidAt;
    private PrivilegeStatus privilegeStatus; // ACTIVE, REVOKED
    private LocalDateTime privilegeRevokedAt;
    private LocalDateTime lastSupportPaidAt;
    private Long totalSupportPaid;
    private String autoPayEnabled;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private LeaveReason leaveReason;
}
