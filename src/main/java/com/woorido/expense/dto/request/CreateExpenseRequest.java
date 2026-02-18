package com.woorido.expense.dto.request;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateExpenseRequest {
  private String meetingId;
  private String title;
  private String description;
  private Long amount;
  private String category;
  private String receiptUrl;
  private LocalDateTime deadline;
}
