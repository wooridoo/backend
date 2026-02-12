package com.woorido.post.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeleteCommentResponse {
    private String commentId;
    private LocalDateTime deletedAt;
}
