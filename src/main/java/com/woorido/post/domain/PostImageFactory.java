package com.woorido.post.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PostImageFactory {
    public PostImage create(String postId, String imageUrl, int displayOrder) {
        return PostImage.builder()
                .id(UUID.randomUUID().toString())
                .postId(postId)
                .imageUrl(imageUrl)
                .displayOrder(displayOrder)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
