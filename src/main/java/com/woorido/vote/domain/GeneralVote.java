package com.woorido.vote.domain;

import com.woorido.vote.domain.Vote.VoteStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralVote {
  private String id;
  private String challengeId;
  private String createdBy; // userId
  private GeneralVoteType type; // KICK, LEADER_KICK, DISSOLVE
  private String title;
  private String description;
  private String targetUserId; // 퇴출 대상 ID
  private Integer eligibleCount;
  private Integer requiredCount;
  private Integer approveCount;
  private Integer rejectCount;
  private VoteStatus status; // PENDING, APPROVED, REJECTED, EXPIRED
  private Integer version;
  private LocalDateTime createdAt;
  private LocalDateTime expiresAt;
  private LocalDateTime closedAt;
}
