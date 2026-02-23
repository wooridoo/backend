package com.woorido.notification.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class NotificationSettings {
    private String id;
    private String userId;
    private Boolean pushEnabled;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean voteNotification;
    private Boolean meetingNotification;
    private Boolean expenseNotification;
    private Boolean snsNotification;
    private Boolean systemNotification;
    private Boolean quietHoursEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
