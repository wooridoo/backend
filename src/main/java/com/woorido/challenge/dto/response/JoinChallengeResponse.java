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
public class JoinChallengeResponse {

  private String memberId;
  private String challengeId;
  private String challengeName;
  private String role;
  private String status;
  private Breakdown breakdown;
  private Long newBalance;
  private String joinedAt;
  private String message;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Breakdown {
    private Long entryFee;
    private Long deposit;
    private Long firstSupport;
    private Long total;
  }
}
