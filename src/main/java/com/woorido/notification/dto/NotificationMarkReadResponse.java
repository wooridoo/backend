package com.woorido.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationMarkReadResponse {
  private String notificationId;
  private boolean isRead;
  private String readAt;
}
