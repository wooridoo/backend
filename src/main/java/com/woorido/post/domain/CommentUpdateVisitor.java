package com.woorido.post.domain;

import com.woorido.post.dto.request.UpdateCommentRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommentUpdateVisitor implements CommentVisitor {
    private final UpdateCommentRequest request;

    @Override
    public void visit(Comment comment) {
        comment.modify(request.getContent());
    }
}
