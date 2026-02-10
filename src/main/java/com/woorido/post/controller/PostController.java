package com.woorido.post.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.util.JwtUtil;
import com.woorido.post.dto.request.CreatePostRequest;
import com.woorido.post.dto.request.UpdatePostRequest;
import com.woorido.post.dto.response.CreatePostResponse;
import com.woorido.post.dto.response.PostDetailResponse;
import com.woorido.post.dto.response.PostListResponse;
import com.woorido.post.service.PostService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/challenges/{challengeId}/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostService postService;
  private final JwtUtil jwtUtil;

  /**
   * JWT 토큰에서 사용자 ID 추출
   */
  private String extractUserId(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001:인증이 필요합니다");
    }
    String accessToken = authHeader.substring(7);
    if (!jwtUtil.validateToken(accessToken)) {
      throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
    }
    return jwtUtil.getUserIdFromToken(accessToken);
  }

  /**
   * 게시글 작성 API
   * POST /challenges/{challengeId}/posts
   */
  @PostMapping
  public ResponseEntity<ApiResponse<CreatePostResponse>> createPost(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody CreatePostRequest request) {

    try {
      String userId = extractUserId(authHeader);

      CreatePostResponse response = postService.createPost(challengeId, userId, request);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(response, "게시글이 작성되었습니다"));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && (message.startsWith("MEMBER_001") || message.startsWith("POST_002"))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    } catch (RuntimeException e) {
      return handleRuntimeException(e, "Create Post Error");
    }
  }

  /**
   * 게시글 상세 조회 API
   * GET /challenges/{challengeId}/posts/{postId}
   */
  @GetMapping("/{postId}")
  public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    try {
      String userId = extractUserId(authHeader);

      PostDetailResponse response = postService.getPostDetail(challengeId, postId, userId);

      return ResponseEntity.ok(ApiResponse.success(response));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("POST_001")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
      }
      if (message != null && message.startsWith("MEMBER_001")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    } catch (RuntimeException e) {
      return handleRuntimeException(e, "Get Post Detail Error");
    }
  }

  /**
   * 게시글 목록 조회 API
   * GET /challenges/{challengeId}/posts
   */
  @GetMapping
  public ResponseEntity<ApiResponse<PostListResponse>> getPostList(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String order) {

    try {
      String userId = extractUserId(authHeader);

      PostListResponse response = postService.getPostList(challengeId, userId, page, size, category, sort, order);

      return ResponseEntity.ok(ApiResponse.success(response));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("MEMBER_001")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    } catch (RuntimeException e) {
      return handleRuntimeException(e, "Get Post List Error");
    }
  }

  /**
   * 게시글 수정 API
   * PUT /challenges/{challengeId}/posts/{postId}
   */
  @PutMapping("/{postId}")
  public ResponseEntity<ApiResponse<String>> updatePost(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody UpdatePostRequest request) {

    try {
      String userId = extractUserId(authHeader);

      postService.updatePost(challengeId, userId, postId, request);

      return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다"));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("POST_001")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
      }
      if (message != null && (message.startsWith("POST_004") || message.startsWith("POST_002"))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    } catch (RuntimeException e) {
      return handleRuntimeException(e, "Update Post Error");
    }
  }

  /**
   * 게시글 삭제 API
   * DELETE /challenges/{challengeId}/posts/{postId}
   */
  @DeleteMapping("/{postId}")
  public ResponseEntity<ApiResponse<String>> deletePost(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    try {
      String userId = extractUserId(authHeader);

      postService.deletePost(challengeId, postId, userId);

      return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다"));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("POST_001")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
      }
      if (message != null && message.startsWith("POST_004")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    } catch (RuntimeException e) {
      return handleRuntimeException(e, "Delete Post Error");
    }
  }

  /**
   * 좋아요 토글 API
   * POST /challenges/{challengeId}/posts/{postId}/like
   */
  @PostMapping("/{postId}/like")
  public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    try {
      String userId = extractUserId(authHeader);

      Map<String, Object> result = postService.toggleLike(postId, userId);
      boolean liked = (boolean) result.get("liked");

      return ResponseEntity.ok(ApiResponse.success(result,
          liked ? "좋아요를 눌렀습니다" : "좋아요를 취소했습니다"));

    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    } catch (RuntimeException e) {
      return handleRuntimeException(e, "Toggle Like Error");
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
