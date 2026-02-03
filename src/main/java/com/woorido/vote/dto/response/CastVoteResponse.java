package com.woorido.vote.dto.response;

import com.woorido.vote.dto.VoteDto;
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
public class CastVoteResponse {
  private String voteId;
  private String myVote;
  private VoteDto.VoteCountDto voteCount;
  private LocalDateTime votedAt;
  private String message;
}
