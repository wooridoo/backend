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
public class ExpenseRequest {
    private String id;
    private String meetingId;
    private String createdBy;
    private String title;
    private Long amount;
    private String description;
    private String receiptUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
}
