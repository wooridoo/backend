package com.woorido.expense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseUserResponse {
  private String userId;
  private String nickname;
  private String profileImage;
}
