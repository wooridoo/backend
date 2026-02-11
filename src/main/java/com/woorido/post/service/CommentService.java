package com.woorido.post.service;

import com.woorido.common.dto.AuthorInfo;
import com.woorido.post.domain.Comment;
import com.woorido.post.dto.request.CreateCommentRequest;
import com.woorido.post.dto.response.CommentResponse;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.post.repository.CommentMapper;
import com.woorido.post.repository.CommentLikeMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final com.woorido.post.domain.CommentDeleteStrategy commentDeleteStrategy;
    private final com.woorido.challenge.repository.ChallengeMemberMapper challengeMemberMapper;

    @Transactional
    public String createComment(String postId, String userId, CreateCommentRequest request) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        Comment comment = Comment.builder()
                .id(id)
                .postId(postId)
                .parentId(request.getParentId())
                .createdBy(userId)
                .content(request.getContent())
                .createdAt(now)
                .updatedAt(now)
                .build();

        commentMapper.save(comment);

        return comment.getId();
    }

    public boolean toggleLike(String commentId, String userId) {
        if (commentLikeMapper.exists(commentId, userId)) {
            commentLikeMapper.delete(commentId, userId);
            commentMapper.decreaseLikeCount(commentId);
            return false;
        } else {
            commentLikeMapper.save(com.woorido.post.domain.CommentLike.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .commentId(commentId)
                    .userId(userId)
                    .createdAt(java.time.LocalDateTime.now())
                    .build());
            commentMapper.increaseLikeCount(commentId);
            return true;
        }
    }

    public com.woorido.post.dto.response.UpdateCommentResponse updateComment(String challengeId, String postId,
            String commentId, String userId, com.woorido.post.dto.request.UpdateCommentRequest request) {
        // 1. Find Comment
        Comment comment = commentMapper.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("COMMENT_002: 댓글을 찾을 수 없습니다"));

        // 2. Validate Membership & Post
        if (!comment.getPostId().equals(postId)) {
            throw new IllegalArgumentException("COMMENT_002: 댓글을 찾을 수 없습니다");
        }

        // 3. Check Author
        if (!comment.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("COMMENT_003: 수정 권한이 없습니다");
        }

        // 4. Update using Visitor
        com.woorido.post.domain.CommentUpdateVisitor visitor = new com.woorido.post.domain.CommentUpdateVisitor(
                request);
        comment.accept(visitor);

        // 5. Update DB
        commentMapper.update(comment);

        return com.woorido.post.dto.response.UpdateCommentResponse.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(String postId) {
        List<Comment> comments = commentMapper.findAllByPostId(postId);
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }

        // Fetch authors
        List<String> userIds = comments.stream()
                .map(Comment::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        // Assuming UserMapper has a method to get multiple users or we fetch
        // individually.
        // For optimization, we should have findAllByIds, but relying on findById for
        // now if not available or looping.
        // Actually, let's use a loop for now or fetch author info.
        // Better: UserMapper needs findById.

        Map<String, AuthorInfo> authorMap = new HashMap<>();
        for (String userId : userIds) {
            // Reusing AuthorInfo logic from UserMapper/UserService usually
            // But here we might just query User table.
            // Let's assume userMapper.findById returns User domain.
            // We need to map User to AuthorInfo.
            // Or better, let's fetch AuthorInfo directly if possible or map it manually.

            // We need to use UserMapper to get User entity
            User user = userMapper.findById(userId);
            if (user != null) {
                authorMap.put(userId, AuthorInfo.builder()
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .profileImage(user.getProfileImageUrl())
                        .build());
            }
        }

        // Map to DTO and handle nesting
        List<CommentResponse> roots = new ArrayList<>();
        Map<String, CommentResponse> dtos = new HashMap<>();

        for (Comment comment : comments) {
            CommentResponse dto = CommentResponse.from(comment, authorMap.get(comment.getCreatedBy()));
            dtos.put(comment.getId(), dto);

            if (comment.getParentId() == null) {
                roots.add(dto);
            }
        }

        // Second pass for nesting
        for (Comment comment : comments) {
            if (comment.getParentId() != null) {
                CommentResponse parent = dtos.get(comment.getParentId());
                if (parent != null) {
                    if (parent.getReplies() == null) {
                        // We need to modify the DTO to have a mutable list or add a setter.
                        // Since CommentResponse uses Builder which creates immutable usually,
                        // actually looking at my DTO, it has List<CommentResponse> replies.
                        // I need to handle this.
                        // Let's use a method in DTO or just reflection/re-building.
                        // Wait, creating DTOs first then linking is hard with immutable pattern.

                        // Let's perform a tree build.
                        // Actually, doing it simple:
                        // No setter in DTO.
                        // I'll leave the nesting logic for now or implement a simpler flat list if the
                        // UI handles it,
                        // but the requirement implies structure.
                        // Let's assume flat list for now or simple "replies" field population via a
                        // separate method if I adding one.

                        // Adjusting plan: I will just return the list sorted by creation time for now
                        // as per "getComments" basic requirement.
                        // The UI can thread it or I can add threading logic if I added `replies` field.
                        // I added `replies` field in DTO.
                        // I'll use reflection or add a method to add reply if I can modify DTO.
                        // I will modify DTO to have @Setter or a helper method `addReply`.
                    }
                }
            }
        }

        // Re-reading DTO: It has `private List<CommentResponse> replies;` and @Builder.
        // I cannot add to it easily.
        // Let's just return flat list for MVP Phase 2 and let FE handle threading or
        // just return roots with populated replies.

        // Simpler approach for Phase 2:
        // Group by parentId.

        Map<String, List<CommentResponse>> childrenMap = new HashMap<>();
        for (Comment c : comments) {
            String pId = c.getParentId();
            if (pId != null) {
                childrenMap.computeIfAbsent(pId, k -> new ArrayList<>())
                        .add(CommentResponse.from(c, authorMap.get(c.getCreatedBy())));
            }
        }

        // Build tree structure
        return comments.stream().filter(c -> c.getParentId() == null)
                .map(c -> {
                    List<CommentResponse> replies = getReplies(c.getId(), comments, authorMap);
                    return CommentResponse.builder()
                            .id(c.getId())
                            .content(c.getContent())
                            .author(authorMap.get(c.getCreatedBy()))
                            .likeCount(c.getLikeCount())
                            .createdAt(c.getCreatedAt())
                            .updatedAt(c.getUpdatedAt())
                            .parentId(null)
                            .replies(replies)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<CommentResponse> getReplies(String parentId, List<Comment> allComments,
            Map<String, AuthorInfo> authorMap) {
        return allComments.stream()
                .filter(c -> parentId.equals(c.getParentId()))
                .map(c -> CommentResponse.builder()
                        .id(c.getId())
                        .content(c.getContent())
                        .author(authorMap.get(c.getCreatedBy()))
                        .likeCount(c.getLikeCount())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .parentId(parentId)
                        .replies(getReplies(c.getId(), allComments, authorMap)) // Recursive
                        .build())
                .collect(Collectors.toList());
    }

    public com.woorido.post.dto.response.DeleteCommentResponse deleteComment(String challengeId, String postId,
            String commentId, String userId) {
        // 1. Find Comment
        Comment comment = commentMapper.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("COMMENT_002: 댓글을 찾을 수 없습니다"));

        if (!comment.getPostId().equals(postId)) {
            throw new IllegalArgumentException("COMMENT_002: 댓글을 찾을 수 없습니다");
        }

        // 2. Fetch User Role
        Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
        String role = (memberInfo != null) ? (String) memberInfo.get("ROLE") : null;

        // 3. Validate Permission (Strategy)
        commentDeleteStrategy.validate(comment, userId, role);

        // 4. Check Children
        int childCount = commentMapper.countByParentId(commentId);

        if (childCount > 0) {
            // 5-A. Soft Delete using Visitor
            com.woorido.post.domain.CommentDeleteVisitor visitor = new com.woorido.post.domain.CommentDeleteVisitor();
            comment.accept(visitor);
            commentMapper.update(comment);
        } else {
            // 5-B. Hard Delete (Physical)
            // Need to remove Likes first due to FK if cascading is not set
            // Ideally we should delete likes. Spec says "Complete Delete".
            // Let's safe guard by deleting likes first.
            commentLikeMapper.deleteByCommentId(commentId);
            commentMapper.deletePhysical(commentId);
        }

        return com.woorido.post.dto.response.DeleteCommentResponse.builder()
                .commentId(commentId)
                .deletedAt(LocalDateTime.now())
                .build();
    }
}
