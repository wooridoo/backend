package com.woorido.challenge.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChallengeRequest {

  @NotBlank(message = "챌린지 이름은 필수입니다")
  @Size(min = 2, max = 50, message = "챌린지 이름은 2~50자여야 합니다")
  private String name;

  @NotBlank(message = "챌린지 설명은 필수입니다")
  @Size(max = 500, message = "챌린지 설명은 최대 500자입니다")
  private String description;

  @NotBlank(message = "카테고리는 필수입니다")
  @Pattern(regexp = "^(HOBBY|STUDY|EXERCISE|SAVINGS|TRAVEL|FOOD|CULTURE|OTHER)$", message = "유효하지 않은 카테고리입니다")
  private String category;

  @NotNull(message = "최대 인원은 필수입니다")
  @Min(value = 3, message = "최대 인원은 3명 이상이어야 합니다")
  @Max(value = 30, message = "최대 인원은 30명 이하여야 합니다")
  private Integer maxMembers;

  @NotNull(message = "월 서포트 금액은 필수입니다")
  @Min(value = 10000, message = "월 서포트 금액은 10,000원 이상이어야 합니다")
  private Long supportAmount;

  @NotNull(message = "보증금은 필수입니다")
  private Long depositAmount;

  @NotNull(message = "납입일은 필수입니다")
  @Min(value = 1, message = "납입일은 1~28 사이여야 합니다")
  @Max(value = 28, message = "납입일은 1~28 사이여야 합니다")
  private Integer supportDay;

  @NotBlank(message = "시작일은 필수입니다")
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "시작일 형식이 올바르지 않습니다 (YYYY-MM-DD)")
  private String startDate;

  @Size(max = 500, message = "썸네일 URL은 최대 500자입니다")
  private String thumbnailImage;

  @Size(max = 1000, message = "규칙은 최대 1000자입니다")
  private String rules;
}
