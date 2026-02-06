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
public class VoteRecord {
  private String id;
  private String voteId;
  private String userId;
  private VoteChoice choice;
  private LocalDateTime votedAt;

  public enum VoteChoice {
    AGREE, DISAGREE, ABSTAIN,
    APPROVE, REJECT // Expense/General Vote용
  }
}
