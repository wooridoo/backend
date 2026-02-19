package com.woorido.django.ledger.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DjangoLedgerGraphRequest {
  private String challengeId;
  private Integer months;
  private Long currentBalance;
  private List<LedgerEntryItem> entries;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LedgerEntryItem {
    private String createdAt;
    private String type;
    private Long amount;
    private Long balanceAfter;
  }
}
