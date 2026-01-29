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
    private String role; // LEADER, FOLLOWER
    private String depositStatus; // NONE, LOCKED, RELEASED, SEIZED
    private LocalDateTime depositLockedAt;
    private LocalDateTime depositUnlockedAt;
    private Long entryFeeAmount;
    private LocalDateTime entryFeePaidAt;
    private String privilegeStatus; // ACTIVE, REVOKED
    private LocalDateTime privilegeRevokedAt;
    private LocalDateTime lastSupportPaidAt;
    private Long totalSupportPaid;
    private String autoPayEnabled;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private String leaveReason;
}
