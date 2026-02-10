package com.woorido.post.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

import com.woorido.common.dto.AuthorInfo;

@Getter
@Builder
public class PostDetailResponse {
  private String postId;
  private String title;
  private String content;
  private String category;
  private AuthorInfo author; // Reusing AuthorInfo class
  private List<ImageInfo> images;
  private Long likeCount;
  private Long commentCount;
  private Long viewCount;
  private Boolean isLiked;
  private Boolean isPinned;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Getter
  @Builder
  public static class ImageInfo {
    private String id;
    private String url;
    private Integer displayOrder;
  }
}
