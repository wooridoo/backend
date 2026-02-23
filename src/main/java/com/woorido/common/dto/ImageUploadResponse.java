package com.woorido.common.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageUploadResponse {
  private String imageUrl;
}
