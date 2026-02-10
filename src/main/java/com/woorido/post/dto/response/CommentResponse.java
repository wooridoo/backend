package com.woorido.post.dto.response;

import com.woorido.common.dto.AuthorInfo;
import com.woorido.post.domain.Comment;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {
    private String id;
    private String content;
    private AuthorInfo author;
    private int likeCount;
    private boolean isLiked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String parentId;
    private List<CommentResponse> replies;

    public static CommentResponse from(Comment comment, AuthorInfo author) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(author)
                .likeCount(comment.getLikeCount())
                // isLiked is typically set separately or requires user context
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .parentId(comment.getParentId())
                .build();
    }
}
