package com.woorido.vote.dto;

import com.woorido.vote.domain.Vote.VoteStatus;
import com.woorido.vote.domain.Vote.VoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteDto {
  private String voteId;
  private VoteType type;
  private String title;
  private VoteStatus status;
  private CreatorDto createdBy;
  private VoteCountDto voteCount;
  private LocalDateTime deadline;
  private LocalDateTime createdAt;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CreatorDto {
    private String userId;
    private String nickname;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class VoteCountDto {
    private int agree;
    private int disagree;
    private int total;
  }
}
