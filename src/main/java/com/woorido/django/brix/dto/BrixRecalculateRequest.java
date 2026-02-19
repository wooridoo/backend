package com.woorido.django.brix.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BrixRecalculateRequest {
  // ISO-8601 datetime string. If null, service uses current KST time.
  private String cutoffAt;
}
