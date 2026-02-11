package com.woorido.post.domain;

public interface PostDeleteStrategy {
    void validate(Post post, String userId, String userRole);
}
