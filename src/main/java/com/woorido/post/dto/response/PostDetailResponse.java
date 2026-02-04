package com.woorido.post.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostDetailResponse {
  private String postId;
  private String title;
  private String content;
  private String category;
  private CreatePostResponse.AuthorInfo author; // Reusing AuthorInfo class
  private List<AttachmentInfo> attachments;
  private Long likeCount;
  private Long commentCount;
  private Long viewCount;
  private Boolean isLiked;
  private Boolean isPinned;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Getter
  @Builder
  public static class AttachmentInfo {
    private String fileId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
  }
}
