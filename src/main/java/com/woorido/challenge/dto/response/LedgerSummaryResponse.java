package com.woorido.challenge.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LedgerSummaryResponse {
  private String challengeId;
  private long totalIncome;
  private long totalExpense;
  private long balance;
  private long entryCount;
}
