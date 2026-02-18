package com.woorido.expense.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
  private String expenseId;
  private String challengeId;
  private String meetingId;
  private String voteId;
  private String title;
  private String description;
  private Long amount;
  private String category;
  private String status;
  private ExpenseUserResponse requestedBy;
  private String receiptUrl;
  private String paymentBarcodeNumber;
  private LocalDateTime approvedAt;
  private LocalDateTime paidAt;
  private LocalDateTime createdAt;
}
