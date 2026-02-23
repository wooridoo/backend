package com.woorido.django.ledger.dto;

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
public class DjangoLedgerGraphResponse {
  private String calculatedAt;
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
