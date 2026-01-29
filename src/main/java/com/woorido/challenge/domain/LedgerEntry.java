package com.woorido.challenge.domain;

import java.time.LocalDateTime;

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
public class LedgerEntry {
    private String id; // UUID
    private String challengeId;
    private String type; // SUPPORT, ENTRY_FEE, EXPENSE, REFUND
    private Long amount;
    private Long balanceBefore;
    private Long balanceAfter;
    private String description;
    private String relatedUserId;
    private String relatedMeetingId;
    private String relatedExpenseRequestId;
    private String relatedBarcodeId;
    private String merchantName;
    private String merchantCategory;
    private String pgProvider;
    private String pgApprovalNumber;
    private String memo;
    private LocalDateTime memoUpdatedAt;
    private String memoUpdatedBy;
    private LocalDateTime createdAt;
}
