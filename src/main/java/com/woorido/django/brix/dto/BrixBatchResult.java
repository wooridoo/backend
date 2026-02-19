package com.woorido.django.brix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrixBatchResult {
  private String cutoffAt;
  private int requestedUsers;
  private int updatedUsers;
}
