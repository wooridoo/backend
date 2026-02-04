package com.woorido.post.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePostResponse {
  private String postId; // Assuming UUID based on Schema, but API spec example shows Long(24). Waiting
                         // on user decision or following Schema (UUID/String).
                         // Wait, schema says id is VARCHAR2(36). API example says Long.
                         // I should follow Schema usually. But let's check what I used in Domain.
                         // String.
                         // Let's stick to String for ID to match Schema.
  private String title;
  private String category;
  private AuthorInfo author;
  private LocalDateTime createdAt;

  @Getter
  @Builder
  public static class AuthorInfo {
    private String userId; // UUID
    private String nickname;
  }
}
