package com.woorido.challenge.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LedgerEntryResponse {
  private String entryId;
  private String challengeId;
  private String type;
  private long amount;
  private String description;
  private String memo;
  private String createdAt;
  private String updatedAt;
}
