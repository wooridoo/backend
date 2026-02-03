package com.woorido.meeting.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompleteMeetingResponse {
  private String meetingId;
  private String status;
  private AttendanceStats attendance;
  private BenefitInfo benefit;
  private String completedAt;
  private String message;

  @Getter
  @Builder
  public static class AttendanceStats {
    private int actual;
    private int total;
    private double rate;
  }

  @Getter
  @Builder
  public static class BenefitInfo {
    private BeneficiaryInfo beneficiary;
    private long amount;
    private String transferredAt;
  }

  @Getter
  @Builder
  public static class BeneficiaryInfo {
    private String userId;
    private String nickname;
  }
}
