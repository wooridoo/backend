package com.woorido.post.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostImagesUploadResponse {
  private List<String> imageUrls;
  private Integer uploadedCount;
}
