package com.woorido.expense.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseListResponse {
  private List<ExpenseResponse> content;
  private long totalAmount;
  private long totalElements;
  private int totalPages;
  private int number;
  private int size;
}
