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
        post.modify(
                request.getTitle(),
                request.getContent(),
                request.getCategory(),
                noticeVal,
                post.getIsPinned());
    }
}
