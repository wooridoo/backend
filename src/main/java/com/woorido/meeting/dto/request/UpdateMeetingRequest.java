package com.woorido.meeting.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMeetingRequest {
  private String title;
  private String description;
  private String scheduledAt;
  private String location;
  private String locationDetail;
  private String agenda;
}
