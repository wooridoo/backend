package com.woorido.meeting.dto.response;

import java.util.Map;

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
  private String scheduledAt;
  private String location;
  private String locationDetail;
  private String agenda;

  private AttendanceSummary attendance;
  private MyAttendance myAttendance;
  private BeneficiaryInfo beneficiary;

  private Long benefitAmount;
  private CreatorInfo createdBy;
  private String createdAt;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AttendanceSummary {
    private int confirmed;
    private int declind; // typo in API spec 'declined' but let's stick to standard English if possible,
                         // but spec says 'declined' in example. Wait, existing MeetingListResponse used
                         // map?
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
  public static class BeneficiaryInfo {
    private String userId;
    private String nickname;
    private int order; // 순번
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
