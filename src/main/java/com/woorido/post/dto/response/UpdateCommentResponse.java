package com.woorido.post.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateCommentResponse {
    private String commentId;
    private String content;
    private LocalDateTime updatedAt;
}
