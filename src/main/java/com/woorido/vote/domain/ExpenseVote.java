package com.woorido.vote.domain;

import com.woorido.vote.domain.Vote.VoteStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * expense_votes 테이블 매핑 (DB 스키마 4.2)
 * expense_requests와 1:1 관계 (expense_request_id FK)
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseVote {
  private String id;
  private String expenseRequestId; // FK to expense_requests.id
  private Integer eligibleCount; // 투표 자격자 수 (참석자만)
  private Integer requiredCount;
  private Integer approveCount;
  private Integer rejectCount;
  private VoteStatus status; // PENDING, APPROVED, REJECTED, EXPIRED
  private Integer version;
  private LocalDateTime createdAt;
  private LocalDateTime expiresAt;
  private LocalDateTime closedAt;
}
