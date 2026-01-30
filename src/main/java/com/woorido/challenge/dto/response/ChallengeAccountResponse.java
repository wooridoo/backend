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
public class ChallengeAccountResponse {

  private String challengeId;
  private Long balance;
  private Long lockedDeposits;
  private Long availableBalance;
  private Stats stats;
  private List<Transaction> recentTransactions;
  private SupportStatus supportStatus;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Stats {
    private Long totalSupport;
    private Long totalExpense;
    private Long totalFee;
    private Long monthlyAverage;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Transaction {
    private String transactionId;
    private Long amount;
    private String type;
    private String description;
    private String createdAt;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SupportStatus {
    private Integer paid;
    private Integer unpaid;
    private Integer total;
  }
}
