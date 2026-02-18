package com.woorido.expense.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateExpenseRequest {
  private String title;
  private String description;
  private Long amount;
  private String category;
  private String receiptUrl;
}
