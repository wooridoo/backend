package com.woorido.post.service;

import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.common.dto.AuthorInfo;
import com.woorido.post.domain.Comment;
import com.woorido.post.domain.Post;
import com.woorido.post.dto.request.CreateCommentRequest;
import com.woorido.post.dto.response.CommentResponse;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.post.repository.CommentMapper;
import com.woorido.post.repository.CommentLikeMapper;
import com.woorido.post.repository.PostMapper;
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
    private final PostMapper postMapper;
    private final ChallengeMemberMapper challengeMemberMapper;

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
                .likeCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        commentMapper.save(comment);

        return comment.getId();
    }

    @Transactional
    public boolean toggleLike(String commentId, String userId) {
        // 비관적 락으로 동시성 제어
        Comment comment = commentMapper.findByIdForUpdate(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("COMMENT_001: 댓글을 찾을 수 없습니다");
        }

        if (commentLikeMapper.exists(commentId, userId)) {
            commentLikeMapper.delete(commentId, userId);
            commentMapper.decreaseLikeCount(commentId);
            return false;
        } else {
            commentLikeMapper.save(com.woorido.post.domain.CommentLike.builder()
                    .id(UUID.randomUUID().toString())
                    .commentId(commentId)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build());
            commentMapper.increaseLikeCount(commentId);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(String postId) {
        List<Comment> comments = commentMapper.findAllByPostId(postId);
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> userIds = comments.stream()
                .map(Comment::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        Map<String, AuthorInfo> authorMap = new HashMap<>();
        for (String userId : userIds) {
            User user = userMapper.findById(userId);
            if (user != null) {
                authorMap.put(userId, AuthorInfo.builder()
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .profileImage(user.getProfileImageUrl())
                        .build());
            }
        }

        // Build tree structure
        return comments.stream()
                .filter(c -> c.getParentId() == null)
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
                .map(c -> {
                    List<CommentResponse> replies = getReplies(c.getId(), allComments, authorMap);
                    return CommentResponse.builder()
                            .id(c.getId())
                            .content(c.getContent())
                            .author(authorMap.get(c.getCreatedBy()))
                            .likeCount(c.getLikeCount())
                            .createdAt(c.getCreatedAt())
                            .updatedAt(c.getUpdatedAt())
                            .parentId(parentId)
                            .replies(replies)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(String commentId, String userId) {
        Comment comment = commentMapper.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("COMMENT_001: 댓글을 찾을 수 없습니다"));

        // 작성자 또는 LEADER 삭제 허용 (PM-004, spec 069)
        if (!comment.getCreatedBy().equals(userId)) {
            Post post = postMapper.findById(comment.getPostId());
            if (post != null) {
                Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(
                        userId, post.getChallengeId());
                String role = memberInfo != null ? (String) memberInfo.get("ROLE") : null;
                if (!"LEADER".equals(role)) {
                    throw new IllegalArgumentException("COMMENT_002: 삭제 권한이 없습니다");
                }
            } else {
                throw new IllegalArgumentException("COMMENT_002: 삭제 권한이 없습니다");
            }
        }
        commentMapper.deleteById(commentId);
    }
}
