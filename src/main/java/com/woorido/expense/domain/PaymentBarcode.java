package com.woorido.expense.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentBarcode {
  private String id;
  private String expenseRequestId;
  private String challengeId;
  private String barcodeNumber;
  private Long amount;
  private String status;
  private LocalDateTime usedAt;
  private String usedMerchantName;
  private String usedMerchantCategory;
  private String pgTxId;
  private LocalDateTime expiresAt;
  private LocalDateTime createdAt;
}
