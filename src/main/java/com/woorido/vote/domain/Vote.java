package com.woorido.vote.domain;

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
public class Vote {
  private String id;
  private String challengeId;
  private VoteType type;
  private String title;
  private String description;
  private String targetId; // 지출 ID 등
  private String meetingId; // 모임 ID (선택)
  private VoteStatus status;
  private String createdBy; // userId
  private LocalDateTime deadline;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
  private Integer requiredCount;

  public enum VoteType {
    EXPENSE, KICK, LEADER_KICK, DISSOLVE, MEETING_ATTENDANCE, MEETING_EXPENSE
  }

  public enum VoteStatus {
    // DB Schema: PENDING, APPROVED, REJECTED, EXPIRED
    // Legacy/Existing data: OPEN, IN_PROGRESS
    PENDING, OPEN, IN_PROGRESS, APPROVED, REJECTED, EXPIRED, CANCELLED, COMPLETED
  }
}
