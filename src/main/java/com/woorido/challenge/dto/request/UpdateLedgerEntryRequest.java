package com.woorido.challenge.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLedgerEntryRequest {
  private String description;
  private String memo;
}
