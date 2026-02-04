package com.woorido.post.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostListResponse {
  private List<PostSummaryResponse> content;
  private int totalElements;
  private int totalPages;
  private int number;
  private int size;
}
