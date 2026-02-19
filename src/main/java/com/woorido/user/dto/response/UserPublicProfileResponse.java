package com.woorido.user.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserPublicProfileResponse {
  private String userId;
  private String nickname;
  private String profileImage;
  private double brix;
  private Stats stats;
  private List<CommonChallenge> commonChallenges;
  private boolean isVerified;
  private String createdAt;

  @Getter
  @Builder
  public static class Stats {
    private int completedChallenges;
    private int totalMeetings;
  }

  @Getter
  @Builder
  public static class CommonChallenge {
    private String challengeId;
    private String name;
  }
}
