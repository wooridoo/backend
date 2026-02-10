package com.woorido.post.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.util.JwtUtil;
import java.util.Map;
import com.woorido.post.dto.request.CreatePostRequest;
import com.woorido.post.dto.request.UpdatePostRequest;
import com.woorido.post.dto.response.CreatePostResponse;
import com.woorido.post.dto.response.PostDetailResponse;
import com.woorido.post.dto.response.PostListResponse;
import com.woorido.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/challenges/{challengeId}/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostService postService;
  private final JwtUtil jwtUtil;

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
      // Check Authorization
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("AUTH_001:인증이 필요합니다");
      }
      String accessToken = authHeader.substring(7);

      // Validate Token and Get User ID
      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
      }
      String userId = jwtUtil.getUserIdFromToken(accessToken);

      CreatePostResponse response = postService.createPost(challengeId, userId, request);

      // 201 Created
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(response, "게시글이 작성되었습니다"));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("MEMBER_001")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(message));
      } else if (message != null && message.startsWith("POST_002")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(message));

    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("AUTH_")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(message));
      }
      log.error("Create Post Error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
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
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("AUTH_001:인증이 필요합니다");
      }
      String accessToken = authHeader.substring(7);

      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
      }
      String userId = jwtUtil.getUserIdFromToken(accessToken);

      PostDetailResponse response = postService.getPostDetail(challengeId, postId, userId);

      return ResponseEntity.ok(ApiResponse.success(response));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && (message.startsWith("MEMBER_001") || message.startsWith("POST_001"))) {
        // POST_001 can be 404, MEMBER_001 is 403
        if (message.startsWith("POST_001")) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("AUTH_")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
      }
      log.error("Get Post Detail Error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  /**
   * 게시글 목록 조회 API
   * GET /challenges/{challengeId}/posts
   */
  @GetMapping
  public ResponseEntity<ApiResponse<PostListResponse>> getPostList(
      @PathVariable("challengeId") String challengeId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "category", required = false) String category,
      @RequestParam(value = "sortBy", defaultValue = "CREATED_AT") String sortBy,
      @RequestParam(value = "order", defaultValue = "DESC") String order,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("AUTH_001:인증이 필요합니다");
      }
      String accessToken = authHeader.substring(7);

      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
      }
      String userId = jwtUtil.getUserIdFromToken(accessToken);

      PostListResponse response = postService.getPostList(challengeId, userId, page, size, category, sortBy, order);

      return ResponseEntity.ok(ApiResponse.success(response));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("MEMBER_001")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("AUTH_")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
      }
      log.error("Get Post List Error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  /**
   * 게시글 수정 API
   * PUT /challenges/{challengeId}/posts/{postId}
   */
  @PutMapping("/{postId}")
  public ResponseEntity<ApiResponse<CreatePostResponse>> updatePost(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody UpdatePostRequest request) {

    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("AUTH_001:인증이 필요합니다");
      }
      String accessToken = authHeader.substring(7);

      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
      }
      String userId = jwtUtil.getUserIdFromToken(accessToken);

      CreatePostResponse response = postService.updatePost(challengeId, postId, userId, request);

      return ResponseEntity.ok(ApiResponse.success(response, "게시글이 수정되었습니다"));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null
          && (message.startsWith("MEMBER_001") || message.startsWith("POST_004") || message.startsWith("POST_002"))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      } else if (message != null && message.startsWith("POST_001")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("AUTH_")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
      }
      log.error("Update Post Error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  /**
   * 게시글 좋아요 토글 API
   * POST /challenges/{challengeId}/posts/{postId}/like
   */
  @PostMapping("/{postId}/like")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleLike(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("AUTH_001:인증이 필요합니다");
      }
      String accessToken = authHeader.substring(7);

      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
      }
      String userId = jwtUtil.getUserIdFromToken(accessToken);

      boolean isLiked = postService.toggleLike(postId, userId);

      return ResponseEntity.ok(ApiResponse.success(
          java.util.Map.of("isLiked", isLiked),
          isLiked ? "좋아요를 눌렀습니다" : "좋아요를 취소했습니다"));

    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
    } catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().startsWith("AUTH_")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
      }
      log.error("Toggle Post Like Error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 게시글 삭제 API
   * DELETE /challenges/{challengeId}/posts/{postId}
   */
  @org.springframework.web.bind.annotation.DeleteMapping("/{postId}")
  public ResponseEntity<ApiResponse<Void>> deletePost(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("AUTH_001:인증이 필요합니다");
      }
      String accessToken = authHeader.substring(7);

      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:유효하지 않은 토큰입니다");
      }
      String userId = jwtUtil.getUserIdFromToken(accessToken);

      postService.deletePost(challengeId, postId, userId);

      return ResponseEntity.ok(ApiResponse.success(null, "게시글이 삭제되었습니다"));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("POST_004")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    } catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().startsWith("AUTH_")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
      }
      log.error("Delete Post Error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }
}
