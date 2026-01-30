package com.woorido.challenge.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyChallengesRequest {

  // 역할 필터: LEADER, FOLLOWER
  private String role;

  // 상태 필터: ACTIVE, CLOSED (RECRUITING, ACTIVE, COMPLETED, DISSOLVED)
  private String status;
}
