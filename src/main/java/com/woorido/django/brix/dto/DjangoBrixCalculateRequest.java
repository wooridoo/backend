package com.woorido.django.brix.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DjangoBrixCalculateRequest {
  private String cutoffAt;
  private List<UserMetric> users;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserMetric {
    private String userId;
    private Integer attendance;
    private Integer paymentMonths;
    private Integer overdue;
    private Integer consecutiveOverdue;
    private Integer feed;
    private Integer comment;
    private Integer like;
    private Integer leaderMonths;
    private Integer voteAbsence;
    private Integer reportReceived;
    private Integer kickCount;
  }
}
