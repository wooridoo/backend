package com.woorido.challenge.domain;

import java.time.LocalDateTime;

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
public class Challenge {
    private String id; // UUID
    private String name;
    private String description;
    private String category;
    private String creatorId; // 리더 ID
    private LocalDateTime leaderLastActiveAt;
    private Double leaderBenefitRate; // NUMBER(5,4)
    private Integer currentMembers;
    private Integer minMembers;
    private Integer maxMembers;
    private Long balance; // 챌린지 금고 잔액
    private Long monthlyFee; // 월 서포트 금액
    private Long depositAmount; // 보증금
    private String status;
    private LocalDateTime activatedAt;
    private String isVerified;
    private LocalDateTime verifiedAt;
    private String isPublic;
    private String thumbnailUrl;
    private String bannerUrl;
    private LocalDateTime deletedAt;
    private String dissolutionReason;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
