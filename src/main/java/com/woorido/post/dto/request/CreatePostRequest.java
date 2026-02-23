package com.woorido.post.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreatePostRequest {
  private String title;
  private String content;
  private String category; // NOTICE, GENERAL, QUESTION
  private List<String> imageUrls;
  private List<String> images;

  public List<String> getImageUrls() {
    if (imageUrls != null) {
      return imageUrls;
    }
    return images;
  }
}
