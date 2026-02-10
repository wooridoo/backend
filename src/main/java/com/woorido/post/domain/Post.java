package com.woorido.post.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Post {
  private String id;
  private String challengeId;
  private String createdBy;
  private String content;
  private String isNotice; // 'Y' or 'N'
  @Builder.Default
  private String isPinned = "N"; // 'Y' or 'N'
  @Builder.Default
  private Long likeCount = 0L;
  @Builder.Default
  private Long commentCount = 0L;
  @Builder.Default
  private Long viewCount = 0L;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;
}
