package com.woorido.post.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.util.JwtUtil;
import com.woorido.post.dto.request.CreateCommentRequest;
import com.woorido.post.dto.response.CommentResponse;
import com.woorido.post.service.CommentService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/challenges/{challengeId}/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createComment(
            @PathVariable("challengeId") String challengeId,
            @PathVariable("postId") String postId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateCommentRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
        }
        String userId = jwtUtil.getUserIdFromToken(token);
        String commentId = commentService.createComment(postId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(Map.of("commentId", commentId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable("challengeId") String challengeId,
            @PathVariable("postId") String postId) {
        List<CommentResponse> comments = commentService.getComments(postId);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> toggleLike(
            @PathVariable("challengeId") String challengeId,
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId,
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
        }
        String userId = jwtUtil.getUserIdFromToken(token);

        boolean isLiked = commentService.toggleLike(commentId, userId);

        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("isLiked", isLiked),
                isLiked ? "좋아요를 눌렀습니다" : "좋아요를 취소했습니다"));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable("challengeId") String challengeId,
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId,
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
        }
        String userId = jwtUtil.getUserIdFromToken(token);

        commentService.deleteComment(challengeId, postId, commentId, userId);

        return ResponseEntity.ok(ApiResponse.success(null, "댓글이 삭제되었습니다"));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<com.woorido.post.dto.response.UpdateCommentResponse>> updateComment(
            @PathVariable("challengeId") String challengeId,
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody com.woorido.post.dto.request.UpdateCommentRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
        }
        String userId = jwtUtil.getUserIdFromToken(token);

        try {
            com.woorido.post.dto.response.UpdateCommentResponse response = commentService.updateComment(challengeId,
                    postId, commentId, userId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "댓글이 수정되었습니다"));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("COMMENT_003")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
            } else if (message != null && message.startsWith("COMMENT_002")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
