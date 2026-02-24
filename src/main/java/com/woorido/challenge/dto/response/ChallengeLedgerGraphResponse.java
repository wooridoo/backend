package com.woorido.challenge.dto.response;

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
public class ChallengeLedgerGraphResponse {
  private String challengeId;
  private Integer months;
  private String calculatedAt;
  private String graphSource;
  private String graphStatusCode;
  private List<MonthlyExpense> monthlyExpenses;
  private List<MonthlyBalance> monthlyBalances;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthlyExpense {
    private String month;
    private Long expense;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthlyBalance {
    private String month;
    private Long balance;
  }
}
