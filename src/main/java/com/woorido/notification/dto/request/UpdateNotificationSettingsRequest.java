package com.woorido.notification.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateNotificationSettingsRequest {
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
}
