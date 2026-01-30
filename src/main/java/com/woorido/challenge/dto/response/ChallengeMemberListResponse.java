package com.woorido.challenge.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeMemberListResponse {

  private List<MemberInfo> members;
  private Summary summary;

  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MemberInfo {
    private String memberId; // ChallengeMember ID (UUID)
    private UserInfo user;
    private String role; // LEADER, FOLLOWER
    private String status; // ACTIVE, LEFT, ETC
    private SupportStatus supportStatus;
    private Double attendanceRate; // 0.0 ~ 100.0
    private String joinedAt;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class UserInfo {
    private String userId; // User ID
    private String nickname;
    private String profileImage;
    private Double brix;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SupportStatus {
    private String thisMonth; // PAID, UNPAID
    private Integer consecutivePaid;
    private Integer overdueCount;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Summary {
    private Integer total;
    private Integer active;
    private Integer overdue;
    private Integer gracePeriod;
  }
}
