package com.woorido.post.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PinPostResponse {
  private String postId;
  private Boolean isPinned;
  private LocalDateTime pinnedAt;
}
