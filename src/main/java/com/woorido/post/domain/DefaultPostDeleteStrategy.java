package com.woorido.post.domain;

import org.springframework.stereotype.Component;

@Component
public class DefaultPostDeleteStrategy implements PostDeleteStrategy {

    @Override
    public void validate(Post post, String userId, String userRole) {
        // Allow if user is author OR user is LEADER
        boolean isAuthor = post.getCreatedBy().equals(userId);
        boolean isLeader = "LEADER".equals(userRole);

        if (!isAuthor && !isLeader) {
            throw new IllegalArgumentException("POST_004: 삭제 권한이 없습니다");
        }
    }
}
