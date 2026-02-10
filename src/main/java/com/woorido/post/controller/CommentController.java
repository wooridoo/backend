package com.woorido.post.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.util.JwtUtil;
import com.woorido.post.dto.request.CreateCommentRequest;
import com.woorido.post.dto.response.CommentResponse;
import com.woorido.post.service.CommentService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/challenges/{challengeId}/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final JwtUtil jwtUtil;

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private String extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 댓글 작성 API
     * POST /challenges/{challengeId}/posts/{postId}/comments
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createComment(
            @PathVariable("challengeId") String challengeId,
            @PathVariable("postId") String postId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody CreateCommentRequest request) {

        try {
            String userId = extractUserId(authHeader);

            String commentId = commentService.createComment(postId, userId, request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(Map.of("commentId", commentId), "댓글이 작성되었습니다"));

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("MEMBER_001")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
            }
            if (message != null && (message.startsWith("POST_001") || message.startsWith("COMMENT_001"))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "Create Comment Error");
        }
    }

    /**
     * 댓글 목록 조회 API
     * GET /challenges/{challengeId}/posts/{postId}/comments
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable("challengeId") String challengeId,
            @PathVariable("postId") String postId) {

        try {
            List<CommentResponse> comments = commentService.getComments(postId);
            return ResponseEntity.ok(ApiResponse.success(comments));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "Get Comments Error");
        }
    }

    /**
     * 댓글 좋아요 토글 API
     * POST /challenges/{challengeId}/posts/{postId}/comments/{commentId}/like
     */
    @PostMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleLike(
            @PathVariable("challengeId") String challengeId,
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            String userId = extractUserId(authHeader);

            boolean isLiked = commentService.toggleLike(commentId, userId);

            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("isLiked", isLiked),
                    isLiked ? "좋아요를 눌렀습니다" : "좋아요를 취소했습니다"));

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("COMMENT_001")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "Toggle Comment Like Error");
        }
    }

    /**
     * 댓글 삭제 API
     * DELETE /challenges/{challengeId}/posts/{postId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable("challengeId") String challengeId,
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            String userId = extractUserId(authHeader);

            commentService.deleteComment(commentId, userId);

            return ResponseEntity.ok(ApiResponse.success(null, "댓글이 삭제되었습니다"));

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("COMMENT_001")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
            }
            if (message != null && message.startsWith("COMMENT_002")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "Delete Comment Error");
        }
    }

    /**
     * RuntimeException 공통 처리
     */
    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<ApiResponse<T>> handleRuntimeException(RuntimeException e, String logPrefix) {
        String message = e.getMessage();
        if (message != null && message.startsWith("AUTH_")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
        }
        log.error(logPrefix, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
    }
}
