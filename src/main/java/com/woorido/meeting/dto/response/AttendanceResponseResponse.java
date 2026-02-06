package com.woorido.meeting.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttendanceResponseResponse {
  private String meetingId;
  private MyAttendanceInfo myAttendance;
  private AttendanceStats attendance;

  @Getter
  @Builder
  public static class MyAttendanceInfo {
    private String status;
    private String respondedAt;
  }

  @Getter
  @Builder
  public static class AttendanceStats {
    private int confirmed;
    private int declined;
    private int pending;
    private int total;
  }
}
