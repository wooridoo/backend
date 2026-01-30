package com.woorido.challenge.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateChallengeRequest {

  // 챌린지 이름 (2-50자)
  private String name;

  // 챌린지 설명 (최대 500자)
  private String description;

  // 챌린지 이미지 URL
  private String thumbnailImage;

  // 챌린지 규칙 (최대 1000자)
  private String rules;

  // 최대 인원 (현재 인원 이상, 증가만 가능)
  private Integer maxMembers;
}
