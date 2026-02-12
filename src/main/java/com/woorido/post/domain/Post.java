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
  private String title;
  private String content;
  private String category;
  private String isNotice; // 'Y' or 'N'
  private String isPinned; // 'Y' or 'N'
  private Long likeCount;
  private Long commentCount;
  private Long viewCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  @Builder
  public Post(String id, String challengeId, String createdBy, String title, String content, String category,
      String isNotice, String isPinned) {
    this.id = id;
    this.challengeId = challengeId;
    this.createdBy = createdBy;
    this.title = title;
    this.content = content;
    this.category = category;
    this.isNotice = isNotice;
    this.isPinned = isPinned != null ? isPinned : "N";
    this.likeCount = 0L;
    this.commentCount = 0L;
    this.viewCount = 0L;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public void accept(PostVisitor visitor) {
    visitor.visit(this);
  }

  public void modify(String title, String content, String category, String isNotice, String isPinned) {
    this.title = title;
    this.content = content;
    this.category = category;
    this.isNotice = isNotice;
    this.isPinned = isPinned != null ? isPinned : "N";
    this.updatedAt = LocalDateTime.now();
  }

  public void markAsDeleted() {
    this.deletedAt = LocalDateTime.now();
  }
}
