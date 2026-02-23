package com.woorido.notification.dto.response;

import com.woorido.notification.domain.NotificationSettings;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationSettingsResponse {
  private boolean pushEnabled;
  private boolean emailEnabled;
  private boolean smsEnabled;
  private boolean voteNotification;
  private boolean meetingNotification;
  private boolean expenseNotification;
  private boolean snsNotification;
  private boolean systemNotification;
  private boolean quietHoursEnabled;
  private String quietHoursStart;
  private String quietHoursEnd;

  public static NotificationSettingsResponse from(NotificationSettings settings) {
    return NotificationSettingsResponse.builder()
        .pushEnabled(Boolean.TRUE.equals(settings.getPushEnabled()))
        .emailEnabled(Boolean.TRUE.equals(settings.getEmailEnabled()))
        .smsEnabled(Boolean.TRUE.equals(settings.getSmsEnabled()))
        .voteNotification(Boolean.TRUE.equals(settings.getVoteNotification()))
        .meetingNotification(Boolean.TRUE.equals(settings.getMeetingNotification()))
        .expenseNotification(Boolean.TRUE.equals(settings.getExpenseNotification()))
        .snsNotification(Boolean.TRUE.equals(settings.getSnsNotification()))
        .systemNotification(Boolean.TRUE.equals(settings.getSystemNotification()))
        .quietHoursEnabled(Boolean.TRUE.equals(settings.getQuietHoursEnabled()))
        .quietHoursStart(settings.getQuietHoursStart())
        .quietHoursEnd(settings.getQuietHoursEnd())
        .build();
  }
}
