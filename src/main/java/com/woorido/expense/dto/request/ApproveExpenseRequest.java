package com.woorido.expense.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveExpenseRequest {
  private Boolean approved;
  private String reason;
}
