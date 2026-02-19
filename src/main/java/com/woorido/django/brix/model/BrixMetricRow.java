package com.woorido.django.brix.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrixMetricRow {
  private String userId;
  private Integer attendanceCount;
  private Integer paymentMonths;
  private Integer overdueCount;
  private Integer consecutiveOverdueCount;
  private Integer feedCount;
  private Integer commentCount;
  private Integer likeCount;
  private Integer leaderMonths;
  private Integer voteAbsenceCount;
  private Integer reportReceivedCount;
  private Integer kickCount;
}
