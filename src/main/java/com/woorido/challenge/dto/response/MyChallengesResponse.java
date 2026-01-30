package com.woorido.challenge.dto.response;

import java.util.List;

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
public class MyChallengesResponse {

  private List<MyChallengeItem> challenges;
  private Summary summary;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MyChallengeItem {
    private String challengeId;
    private String name;
    private String status;
    private String myRole;
    private String myStatus;
    private MemberCount memberCount;
    private Long supportAmount;
    private String nextSupportDate;
    private String thumbnailImage;
    private UpcomingMeeting upcomingMeeting;
  }

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
  public static class UpcomingMeeting {
    private String meetingId;
    private String title;
    private String scheduledAt;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Summary {
    private Integer totalChallenges;
    private Integer asLeader;
    private Integer asFollower;
    private Long monthlySupport;
  }
}
