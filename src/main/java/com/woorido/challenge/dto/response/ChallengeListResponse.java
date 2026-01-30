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
public class ChallengeListResponse {

  private List<ChallengeItem> content;
  private PageInfo page;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChallengeItem {
    private String challengeId;
    private String name;
    private String description;
    private String category;
    private String status;
    private MemberCount memberCount;
    private Long supportAmount;
    private String thumbnailImage;
    private Boolean isVerified;
    private Leader leader;
    private String createdAt;
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
  public static class Leader {
    private String userId;
    private String nickname;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PageInfo {
    private Integer number; // 현재 페이지 번호
    private Integer size; // 페이지 크기
    private Long totalElements; // 전체 항목 수
    private Integer totalPages; // 전체 페이지 수
  }
}
