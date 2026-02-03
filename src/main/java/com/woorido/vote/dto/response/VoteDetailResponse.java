package com.woorido.vote.dto.response;

import com.woorido.vote.domain.Vote.VoteStatus;
import com.woorido.vote.domain.Vote.VoteType;
import com.woorido.vote.dto.VoteDto.CreatorDto;
import com.woorido.vote.dto.VoteDto.VoteCountDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteDetailResponse {
  private String voteId;
  private String challengeId;
  private VoteType type;
  private String title;
  private String description;
  private VoteStatus status;
  private CreatorDto createdBy;
  private Map<String, Object> targetInfo; // 지출/멤버 등 대상 정보
  private VoteCountDto voteCount;
  private String myVote; // AGREE, DISAGREE, ABSTAIN, null
  private int eligibleVoters;
  private int requiredApproval;
  private LocalDateTime deadline;
  private LocalDateTime createdAt;
}
