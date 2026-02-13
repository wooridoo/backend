package com.woorido.meeting.dto.response;

import java.util.List;
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
  private String meetingDate;
  private String location;
  private String locationDetail;

  private AttendanceSummary attendance;
  private MyAttendance myAttendance;
  private List<MemberInfo> members;

  private CreatorInfo createdBy;
  private String createdAt;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AttendanceSummary {
    private int confirmed;
    private int declined;
    private int pending;
    private int total;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MyAttendance {
    private String status;
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

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MemberInfo {
    private String userId;
    private String nickname;
    private String profileImage;
  }
}
