package com.woorido.challenge.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChallengeListRequest {

  // 상태 필터 (RECRUITING, ACTIVE, CLOSED)
  private String status;

  // 카테고리 필터
  private String category;

  // 정렬 기준 (기본값: createdAt.desc)
  private String sort = "createdAt.desc";

  // 페이지 번호 (0부터 시작)
  private Integer page = 0;

  // 페이지 크기 (기본값: 20)
  private Integer size = 20;

  // 정렬 필드 추출 (예: createdAt.desc -> created_at)
  public String getSortField() {
    if (sort == null || sort.isEmpty()) {
      return "created_at";
    }
    String field = sort.split("\\.")[0];
    // camelCase to snake_case
    return switch (field) {
      case "createdAt" -> "created_at";
      case "name" -> "name";
      case "memberCount" -> "current_members";
      default -> "created_at";
    };
  }

  // 정렬 방향 추출 (예: createdAt.desc -> DESC)
  public String getSortDirection() {
    if (sort == null || !sort.contains(".")) {
      return "DESC";
    }
    String direction = sort.split("\\.")[1];
    return "asc".equalsIgnoreCase(direction) ? "ASC" : "DESC";
  }

  // 오프셋 계산
  public int getOffset() {
    return page * size;
  }
}
