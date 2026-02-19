package com.woorido.challenge.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LedgerListResponse {
  private List<LedgerEntryResponse> content;
  private long totalElements;
  private int totalPages;
  private int number;
  private int size;
}
