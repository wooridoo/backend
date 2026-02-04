package com.woorido.post.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSummaryResponse {
  private String postId;
  private String title;
  private String content; // Preview? Or full content as per spec it says "content"
  private String category;
  private CreatePostResponse.AuthorInfo author;
  private Long likeCount;
  private Long commentCount;
  private Long viewCount;
  private Boolean isPinned;
  private LocalDateTime createdAt;
}
