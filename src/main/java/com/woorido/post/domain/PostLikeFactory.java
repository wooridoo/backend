package com.woorido.post.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PostLikeFactory {
    public PostLike create(String postId, String userId) {
        return PostLike.builder()
                .id(UUID.randomUUID().toString())
                .postId(postId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
