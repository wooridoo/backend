package com.woorido.post.domain;

import com.woorido.post.dto.request.UpdatePostRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PostUpdateVisitor implements PostVisitor {
    private final UpdatePostRequest request;
    private final String isNotice;

    @Override
    public void visit(Post post) {
        String noticeVal = (isNotice != null) ? isNotice : post.getIsNotice();
        String titleVal = request.getTitle() != null ? request.getTitle() : post.getTitle();
        String contentVal = request.getContent() != null ? request.getContent() : post.getContent();
        String categoryVal = request.getCategory() != null ? request.getCategory() : post.getCategory();

        post.modify(
                titleVal,
                contentVal,
                categoryVal,
                noticeVal,
                post.getIsPinned());
    }
}
