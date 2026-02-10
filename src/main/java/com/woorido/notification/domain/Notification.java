package com.woorido.notification.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class Notification {
    private String id;
    private String userId;
    private NotificationType type;
    private String title;
    private String content; // Maps to MESSAGE in Frontend
    private String linkUrl;
    private String relatedEntityType;
    private String relatedEntityId;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
