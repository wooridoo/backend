package com.woorido.post.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreatePostRequest {
  private String title;
  private String content;
  private String category; // NOTICE, GENERAL, QUESTION
  private List<Long> attachmentIds;
}
