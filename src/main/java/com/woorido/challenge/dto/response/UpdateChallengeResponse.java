package com.woorido.challenge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChallengeResponse {

  private String challengeId;
  private String name;
  private String description;
  private Integer maxMembers;
  private String updatedAt;
  private String message;
}
