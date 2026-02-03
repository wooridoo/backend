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
public class ExpenseVote {
  private String id;
  private String challengeId;
  private String createdBy;
  private String title;
  private String description;
  private String targetId; // 지출 ID
  private Integer eligibleCount;
  private Integer requiredCount;
  private Integer approveCount;
  private Integer rejectCount;
  private String status; // PENDING, APPROVED, REJECTED, EXPIRED
  private Integer version;
  private LocalDateTime createdAt;
  private LocalDateTime expiresAt;
  private LocalDateTime closedAt;
}
