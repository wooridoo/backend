package com.woorido.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationReadAllResponse {
  private int readCount;
}
