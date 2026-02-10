package com.woorido.post.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostLike {
    private String id;
    private String postId;
    private String userId;
    private LocalDateTime createdAt;
}
