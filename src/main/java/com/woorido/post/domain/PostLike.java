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
public class PostLike {
    private String id;
    private String postId;
    private String userId;
    private LocalDateTime createdAt;
}
