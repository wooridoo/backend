package com.woorido.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserProfileResponse {

  private Long userId;
  private String email;
  private String nickname;
  private String phone;
  private String birthDate;
  private String profileImage;
  private String status;
  private Double brix;
  private AccountInfo account;
  private StatsInfo stats;
  private String createdAt;
  private String updatedAt;

  @Getter
  @Builder
  @AllArgsConstructor
  public static class AccountInfo {
    private Long accountId;
    private Long balance;
    private Long availableBalance;
    private Long lockedBalance;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class StatsInfo {
    private Integer challengeCount;
    private Integer completedChallenges;
    private Long totalSupportAmount;
  }
}
