package com.woorido.notification.dto;

import com.woorido.notification.domain.Notification;
import lombok.Builder;
import lombok.Getter;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class NotificationResponse {
    private String notificationId;
    private String type;
    private String title;
    private String message;
    private boolean isRead;
    private Map<String, Object> data;
    private String createdAt;
    private String readAt;

    public static NotificationResponse from(Notification notification) {
        Map<String, Object> dataMap = new HashMap<>();
        if (notification.getRelatedEntityType() != null) {
            dataMap.put("subType", notification.getRelatedEntityType());
        }
        if (notification.getRelatedEntityId() != null) {
            dataMap.put("targetId", notification.getRelatedEntityId());
        }
        if (notification.getLinkUrl() != null) {
            dataMap.put("linkUrl", notification.getLinkUrl());
        }

        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .type(notification.getType().getCategory())
                .title(notification.getTitle())
                .message(notification.getContent())
                .isRead(notification.getIsRead())
                .data(dataMap)
                .createdAt(notification.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME))
                .readAt(notification.getReadAt() != null ? notification.getReadAt().format(DateTimeFormatter.ISO_DATE_TIME) : null)
                .build();
    }
}
