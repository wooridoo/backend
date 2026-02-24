package com.woorido.post.domain;

import com.woorido.post.dto.request.CreatePostRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PostFactory {

    public Post create(
            String challengeId,
            String userId,
            CreatePostRequest request,
            String category,
            String isNotice,
            String isPinned) {
        return Post.builder()
                .id(UUID.randomUUID().toString())
                .challengeId(challengeId)
                .createdBy(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .category(category)
                .isNotice(isNotice)
                .isPinned(isPinned)
                .build();
    }
}
