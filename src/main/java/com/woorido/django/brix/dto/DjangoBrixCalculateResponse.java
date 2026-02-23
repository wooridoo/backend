package com.woorido.django.brix.dto;

import java.util.List;

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
public class DjangoBrixCalculateResponse {
  private String calculatedAt;
  private List<UserScore> results;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserScore {
    private String userId;
    private Double paymentScore;
    private Double activityScore;
    private Double totalScore;
  }
}
