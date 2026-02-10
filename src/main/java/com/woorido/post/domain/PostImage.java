package com.woorido.post.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostImage {
    private String id;
    private String postId;
    private String imageUrl;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
