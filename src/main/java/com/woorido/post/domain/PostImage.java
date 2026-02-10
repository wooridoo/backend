package com.woorido.post.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostImage {
    private String id;
    private String postId;
    private String imageUrl;
    private int displayOrder;
    private LocalDateTime createdAt;
}
