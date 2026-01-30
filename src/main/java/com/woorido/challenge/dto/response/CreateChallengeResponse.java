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
public class CreateChallengeResponse {

  private String challengeId;
  private String name;
  private String status;
  private MemberCount memberCount;
  private String myRole;
  private String createdAt;
  private String message;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MemberCount {
    private Integer current;
    private Integer max;
  }
}
