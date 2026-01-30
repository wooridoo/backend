package com.woorido.challenge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDetailResponse {

  private String challengeId;
  private String name;
  private String description;
  private String category;
  private String status;
  private MemberCount memberCount;
  private Long supportAmount;
  private Long depositAmount;
  private Integer supportDay;
  private String thumbnailImage;
  private String rules;
  private Boolean isVerified;
  private Leader leader;
  private Account account;
  private Stats stats;
  private Boolean isMember;
  private MyMembership myMembership;
  private String startedAt;
  private String createdAt;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MemberCount {
    private Integer current;
    private Integer max;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Leader {
    private String id;
    private String nickname;
    private String profileImage;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Account {
    private Long balance;
    private Long totalSupport;
    private Long totalExpense;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Stats {
    private Integer totalMeetings;
    private Integer completedMeetings;
    private Double averageAttendance;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MyMembership {
    private String memberId;
    private String role;
    private String joinedAt;
    private String status;
  }
}
