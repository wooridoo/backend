package com.woorido.post.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UpdatePostRequest {
    private String title;
    private String content;
    private String category;
    private List<String> imageUrls; // Optional, for future use
}
