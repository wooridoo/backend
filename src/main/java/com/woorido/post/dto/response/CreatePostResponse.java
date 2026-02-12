package com.woorido.post.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

import com.woorido.common.dto.AuthorInfo;

@Getter
@Builder
public class CreatePostResponse {
  private String postId;
  private String title;
  private String category;
  private AuthorInfo author;
  private String content; // Added for Update Response
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt; // Added for Update Response
}
