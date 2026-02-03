package com.woorido.meeting.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeetingListResponse {

  private List<MeetingItem> content;
  private PageInfo page;

  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MeetingItem {
    private String meetingId;
    private String title;
    private String description;
    private String status;
    private String scheduledAt;
    private String location;
    private AttendanceInfo attendance;
    private BeneficiaryInfo beneficiary; // 베네핏 수령자
    private String createdAt;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class AttendanceInfo {
    private Integer confirmed;
    private Integer total; // 전체 멤버 수 or 대상자 수
  }

  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class BeneficiaryInfo {
    private String userId;
    private String nickname;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class PageInfo {
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
  }
}
