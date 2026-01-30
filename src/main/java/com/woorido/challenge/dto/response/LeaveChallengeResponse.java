package com.woorido.challenge.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class LeaveChallengeResponse {
  private String challengeId;
  private String challengeName;
  private Refund refund;
  private Long newBalance;
  private String leftAt;

  @Getter
  @Builder
  @ToString
  public static class Refund {
    private Long deposit;
    private Long deducted;
    private Long netRefund;
  }
}
