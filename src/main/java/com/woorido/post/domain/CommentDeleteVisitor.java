package com.woorido.post.domain;

public class CommentDeleteVisitor implements CommentVisitor {
    @Override
    public void visit(Comment comment) {
        comment.modify("삭제된 댓글입니다"); // Reusing modify to set content and updatedAt
        // We also need to set deletedAt, but Comment.modify only sets
        // content/updatedAt.
        // We might need a specific markAsDeleted method in Comment or use
        // reflection/setter?
        // Let's add markAsDeleted to Comment entity first or use what's available.
        // The prompt says "isDeleted: true" logic.
        // In our entity, we have deletedAt.
        comment.markAsDeleted();
    }
}
