package com.woorido.post.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeletePostResponse {
    private String postId;
    private LocalDateTime deletedAt;
}
