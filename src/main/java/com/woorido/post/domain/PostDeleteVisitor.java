package com.woorido.post.domain;

public class PostDeleteVisitor implements PostVisitor {
    @Override
    public void visit(Post post) {
        post.markAsDeleted();
    }
}
