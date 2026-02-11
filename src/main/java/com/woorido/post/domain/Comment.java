package com.woorido.post.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {
    private String id;
    private String postId;
    private String parentId;
    private String createdBy;
    private String content;
    private int likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Builder
    public Comment(String id, String postId, String parentId, String createdBy, String content, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.postId = postId;
        this.parentId = parentId;
        this.createdBy = createdBy;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.likeCount = 0;
    }

    public void modify(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void accept(CommentVisitor visitor) {
        visitor.visit(this);
    }

    public void markAsDeleted() {
        this.content = "삭제된 댓글입니다";
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
