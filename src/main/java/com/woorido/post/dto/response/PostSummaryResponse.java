package com.woorido.post.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

import com.woorido.common.dto.AuthorInfo;

@Getter
@Builder
public class PostSummaryResponse {
  private String postId;
  private String title;
  private String content;
  private String category;
  private AuthorInfo author;
  private Long likeCount;
  private Long commentCount;
  private Long viewCount;
  private Boolean isPinned;
  private Boolean isLiked;
  private List<String> images;
  private LocalDateTime createdAt;
}
