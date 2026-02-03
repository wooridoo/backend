package com.woorido.vote.dto.request;

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
public class CreateVoteRequest {
  private VoteType type;
  private String title;
  private String description;
  private String targetId; // API 043 spec says Long, but we use String internally. Input will be parsed.
  private LocalDateTime deadline;
}
