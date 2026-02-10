package com.woorido.post.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentLike {
    private String id;
    private String commentId;
    private String userId;
    private LocalDateTime createdAt;
}
