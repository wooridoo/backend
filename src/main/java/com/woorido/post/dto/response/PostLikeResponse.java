package com.woorido.post.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostLikeResponse {
    private String postId;
    private boolean liked;
    private long likeCount;
}
