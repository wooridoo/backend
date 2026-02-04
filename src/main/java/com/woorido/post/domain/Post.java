package com.woorido.post.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {
  private String id;
  private String challengeId;
  private String createdBy;
  private String content;
  private String isNotice; // 'Y' or 'N'
  private String isPinned; // 'Y' or 'N'
  private Long likeCount;
  private Long commentCount;
  private Long viewCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  @Builder
  public Post(String id, String challengeId, String createdBy, String content, String isNotice, String isPinned) {
    this.id = id;
    this.challengeId = challengeId;
    this.createdBy = createdBy;
    this.content = content;
    this.isNotice = isNotice;
    this.isPinned = isPinned != null ? isPinned : "N";
    this.likeCount = 0L;
    this.commentCount = 0L;
    this.viewCount = 0L;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }
}
