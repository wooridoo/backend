package com.woorido.vote.dto.response;

import com.woorido.vote.domain.Vote.VoteStatus;
import com.woorido.vote.domain.Vote.VoteType;
import com.woorido.vote.dto.VoteDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteResultResponse {
  private String voteId;
  private VoteType type;
  private VoteStatus status;
  private VoteDto.VoteCountDto voteCount;
  private int eligibleVoters;
  private int requiredApproval;
  private boolean passed;
  private double approvalRate;
}
