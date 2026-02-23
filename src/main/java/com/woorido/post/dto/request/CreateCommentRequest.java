package com.woorido.post.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCommentRequest {
  private String content;
  private String parentId;
  private String parentCommentId;

  public String getParentId() {
    if (parentId != null && !parentId.isBlank()) {
      return parentId;
    }
    if (parentCommentId != null && !parentCommentId.isBlank()) {
      return parentCommentId;
    }
    return null;
  }
}
