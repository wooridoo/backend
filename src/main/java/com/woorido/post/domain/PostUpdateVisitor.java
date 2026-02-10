package com.woorido.post.domain;

import com.woorido.post.dto.request.UpdatePostRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PostUpdateVisitor implements PostVisitor {
    private final UpdatePostRequest request;
    private final String isNotice; // 'Y' or 'N'

    @Override
    public void visit(Post post) {
        String noticeVal = (isNotice != null) ? isNotice : post.getIsNotice();
        // Default pin behavior: if not provided, keep existing? Or default to N?
        // Usually update keeps existing if null.
        // However, for simplicity and ensuring non-null, we'll pass explicitly handled
        // values or existing.
        // Spec says isPinned is not in Update Request (it was not in API 057 spec
        // provided in prompt).
        // Wait, API 057 spec in prompt does NOT have isPinned in parameters.
        // API 56 (Create) doesn't have it either (default N).
        // API 55 (Detail) returns it.
        // So Update likely doesn't change pinned status unless there's a specific API
        // or it's admin/leader only.
        // For now, keep existing pinned status.

        post.modify(
                request.getTitle(),
                request.getContent(),
                request.getCategory(),
                noticeVal,
                post.getIsPinned());
    }
}
