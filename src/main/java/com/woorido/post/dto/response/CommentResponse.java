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
    private String commentId;
    private String content;
    private AuthorInfo author;
    private int likeCount;
    private boolean isDeleted;
    private boolean isLiked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String parentId;
    private List<CommentResponse> replies; // For nested comments

    public static CommentResponse from(Comment comment, AuthorInfo author, boolean isLiked) {
        return CommentResponse.builder()
                .id(comment.getId())
                .commentId(comment.getId())
                .content(comment.getContent())
                .author(author)
                .likeCount(comment.getLikeCount())
                .isDeleted(comment.getDeletedAt() != null)
                .isLiked(isLiked)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .parentId(comment.getParentId())
                .build();
    }
}
