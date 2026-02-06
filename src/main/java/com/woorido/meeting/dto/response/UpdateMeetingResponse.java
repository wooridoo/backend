package com.woorido.meeting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMeetingResponse {
  private String meetingId;
  private String title;
  private String meetingDate; // scheduledAt -> meetingDate
  private String location;
  private String locationDetail;
  private String updatedAt;
  private String message;
}
