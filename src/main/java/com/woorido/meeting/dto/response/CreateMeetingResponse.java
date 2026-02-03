package com.woorido.meeting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeetingResponse {
  private String meetingId;
  private String title;
  private String status;
  private String scheduledAt;

  private MeetingDetailResponse.BeneficiaryInfo beneficiary;

  private String createdAt;
  private String message;
}
