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
public class GeneralVoteRecord {
  private String id;
  private String generalVoteId;
  private String userId;
  private String choice; // APPROVE, REJECT
  private String comment;
  private LocalDateTime createdAt;
}
