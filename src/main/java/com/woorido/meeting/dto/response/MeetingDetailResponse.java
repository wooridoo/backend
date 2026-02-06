package com.woorido.meeting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDetailResponse {

  private String meetingId;
  private String challengeId;
  private String title;
  private String description;
  private String status;
  private String meetingDate; // scheduledAt -> meetingDate
  private String location;
  private String locationDetail;
  // private String agenda; // Removed

  private AttendanceSummary attendance;
  private MyAttendance myAttendance;
  // private BeneficiaryInfo beneficiary; // Removed

  // private Long benefitAmount; // Removed
  private CreatorInfo createdBy;
  private String createdAt;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AttendanceSummary {
    private int confirmed;
    private int declind;
    private int pending;
    private int total;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MyAttendance {
    private String status; // "CONFIRMED", "PENDING", "DECLINED"
    private String respondedAt;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreatorInfo {
    private String userId;
    private String nickname;
  }
}
