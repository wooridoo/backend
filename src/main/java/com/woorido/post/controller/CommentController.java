package com.woorido.post.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.exception.RateLimitExceededException;
import com.woorido.common.util.AuthHeaderResolver;
import com.woorido.post.dto.request.CreateCommentRequest;
import com.woorido.post.dto.request.UpdateCommentRequest;
import com.woorido.post.dto.response.CommentResponse;
import com.woorido.post.dto.response.UpdateCommentResponse;
import com.woorido.post.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/challenges/{challengeId}/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {
  // Learning note:
  // - Controller parses request/header and delegates business rules to Service.
  // - Keep API response mapping here, keep domain rules in Service.

  private final CommentService commentService;
  private final AuthHeaderResolver authHeaderResolver;

  @PostMapping
  public ResponseEntity<ApiResponse<Map<String, String>>> createComment(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader("Authorization") String authHeader,
      HttpServletRequest httpServletRequest,
      @RequestBody CreateCommentRequest request) {

    try {
      String userId = extractUserId(authHeader);
      String clientIp = extractClientIp(httpServletRequest);
      String commentId = commentService.createComment(challengeId, postId, userId, clientIp, request);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(Map.of("commentId", commentId)));
    } catch (RateLimitExceededException e) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
          .body(ApiResponse.error(e.getMessage()));
    } catch (IllegalArgumentException e) {
      return handleIllegalArgument(e);
    } catch (RuntimeException e) {
      return handleRuntime(e);
    }
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "50") int size,
      @RequestHeader("Authorization") String authHeader) {

    try {
      String userId = extractUserId(authHeader);
      List<CommentResponse> comments = commentService.getComments(challengeId, postId, userId, page, size);
      return ResponseEntity.ok(ApiResponse.success(comments));
    } catch (IllegalArgumentException e) {
      return handleIllegalArgument(e);
    } catch (RuntimeException e) {
      return handleRuntime(e);
    }
  }

  @PostMapping("/{commentId}/like")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleLike(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @PathVariable("commentId") String commentId,
      @RequestHeader("Authorization") String authHeader,
      HttpServletRequest httpServletRequest) {

    try {
      String userId = extractUserId(authHeader);
      String clientIp = extractClientIp(httpServletRequest);
      boolean isLiked = commentService.toggleLike(challengeId, postId, commentId, userId, clientIp);
      return ResponseEntity.ok(ApiResponse.success(
          Map.of("isLiked", isLiked),
          isLiked ? "좋아요를 눌렀습니다" : "좋아요를 취소했습니다"));
    } catch (RateLimitExceededException e) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
          .body(ApiResponse.error(e.getMessage()));
    } catch (IllegalArgumentException e) {
      return handleIllegalArgument(e);
    } catch (RuntimeException e) {
      return handleRuntime(e);
    }
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<ApiResponse<Void>> deleteComment(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @PathVariable("commentId") String commentId,
      @RequestHeader("Authorization") String authHeader) {

    try {
      String userId = extractUserId(authHeader);
      commentService.deleteComment(challengeId, postId, commentId, userId);
      return ResponseEntity.ok(ApiResponse.success(null, "댓글이 삭제되었습니다"));
    } catch (IllegalArgumentException e) {
      return handleIllegalArgument(e);
    } catch (RuntimeException e) {
      return handleRuntime(e);
    }
  }

  @PutMapping("/{commentId}")
  public ResponseEntity<ApiResponse<UpdateCommentResponse>> updateComment(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @PathVariable("commentId") String commentId,
      @RequestHeader("Authorization") String authHeader,
      @RequestBody UpdateCommentRequest request) {

    try {
      String userId = extractUserId(authHeader);
      UpdateCommentResponse response = commentService.updateComment(challengeId, postId, commentId, userId, request);
      return ResponseEntity.ok(ApiResponse.success(response, "댓글이 수정되었습니다"));
    } catch (IllegalArgumentException e) {
      return handleIllegalArgument(e);
    } catch (RuntimeException e) {
      return handleRuntime(e);
    }
  }

  private String extractUserId(String authHeader) {
    return authHeaderResolver.resolveUserId(authHeader);
  }

  private String extractClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      String[] parts = forwarded.split(",");
      if (parts.length > 0 && !parts[0].isBlank()) {
        return parts[0].trim();
      }
    }
    String remoteAddr = request.getRemoteAddr();
    return remoteAddr == null ? "unknown" : remoteAddr;
  }

  private <T> ResponseEntity<ApiResponse<T>> handleRuntime(RuntimeException e) {
    String message = e.getMessage();
    if (message != null && message.startsWith("AUTH_")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("서버 오류가 발생했습니다"));
  }

  private <T> ResponseEntity<ApiResponse<T>> handleIllegalArgument(IllegalArgumentException e) {
    String message = e.getMessage();
    if (message != null && (message.startsWith("MEMBER_001") || message.startsWith("COMMENT_003"))) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
    }
    if (message != null && (message.startsWith("POST_001") || message.startsWith("COMMENT_002"))) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
  }
}
