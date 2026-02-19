package com.woorido.challenge.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLedgerEntryRequest {
  private String type;
  private Long amount;
  private String description;
  private String memo;
}
