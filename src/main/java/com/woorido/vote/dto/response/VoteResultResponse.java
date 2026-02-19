package com.woorido.vote.dto.response;

import com.woorido.vote.domain.Vote.VoteStatus;
import com.woorido.vote.domain.Vote.VoteType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoteResultResponse {
  private String voteId;
  private VoteType type;
  private VoteStatus status;
  private VoteCount voteCount;
  private int eligibleVoters;
  private int requiredApproval;
  private boolean passed;
  private double approvalRate;

  @Getter
  @Builder
  public static class VoteCount {
    private int agree;
    private int disagree;
    private int total;
  }
}
