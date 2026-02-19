package com.woorido.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationListResponse {
  private List<NotificationResponse> content;
  private int unreadCount;
  private long totalElements;
  private int totalPages;
  private int number;
  private int size;
}
