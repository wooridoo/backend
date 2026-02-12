package com.woorido.post.domain;

import org.springframework.stereotype.Component;

@Component
public class CommentDeleteStrategy {
    public void validate(Comment comment, String userId, String userRole) {
        // 1. Check Author
        if (comment.getCreatedBy().equals(userId)) {
            return;
        }

        // 2. Check Leader
        if ("LEADER".equals(userRole)) {
            return;
        }

        // 3. Fail
        throw new IllegalArgumentException("COMMENT_003: 삭제 권한이 없습니다");
    }
}
