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
import org.springframework.web.bind.annotation.DeleteMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/challenges/{challengeId}/posts")
@RequiredArgsConstructor
public class PostController {
  // Learning note:
  // - Controller parses request/header and delegates business rules to Service.
  // - Keep API response mapping here, keep domain rules in Service.

  private final PostService postService;
  private final JwtUtil jwtUtil;
  private final com.woorido.common.strategy.ImageUploadStrategy imageUploadStrategy;
  private final com.woorido.challenge.repository.ChallengeMemberMapper challengeMemberMapper; // Need to verify
                                                                                              // membership

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
        throw new RuntimeException("AUTH_001:Authorization header is required");
      }
      String accessToken = authHeader.substring(7);

      // Validate Token and Get User ID
      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:Invalid access token");
      }
      String userId = jwtUtil.getUserIdFromToken(accessToken);

      CreatePostResponse response = postService.createPost(challengeId, userId, request);

      // 201 Created
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(response, "Post created successfully"));

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
        throw new RuntimeException("AUTH_001:Authorization header is required");
      }
      String accessToken = authHeader.substring(7);

      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:Invalid access token");
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
        throw new RuntimeException("AUTH_001:Authorization header is required");
      }
      String accessToken = authHeader.substring(7);

      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:Invalid access token");
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
        throw new RuntimeException("AUTH_001:Authorization header is required");
      }
      String accessToken = authHeader.substring(7);

      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:Invalid access token");
      }
      String userId = jwtUtil.getUserIdFromToken(accessToken);

      CreatePostResponse response = postService.updatePost(challengeId, postId, userId, request);

      return ResponseEntity.ok(ApiResponse.success(response, "Post updated successfully"));

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
  public ResponseEntity<ApiResponse<com.woorido.post.dto.response.PostLikeResponse>> toggleLike(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("AUTH_001:Authorization header is required");
      }
      String accessToken = authHeader.substring(7);

      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:Invalid access token");
      }
      String userId = jwtUtil.getUserIdFromToken(accessToken);

      com.woorido.post.dto.response.PostLikeResponse response = postService.toggleLike(challengeId, postId, userId);

      String message = response.isLiked() ? "Post liked" : "Post like removed";
      return ResponseEntity.ok(ApiResponse.success(response, message));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("MEMBER_001")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
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
  @DeleteMapping("/{postId}")
  public ResponseEntity<ApiResponse<com.woorido.post.dto.response.DeletePostResponse>> deletePost(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("AUTH_001:Authorization header is required");
      }
      String accessToken = authHeader.substring(7);

      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:Invalid access token");
      }
      String userId = jwtUtil.getUserIdFromToken(accessToken);

      com.woorido.post.dto.response.DeletePostResponse response = postService.deletePost(challengeId, postId, userId);

      return ResponseEntity.ok(ApiResponse.success(response, "Post deleted successfully"));

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

  /**
   * 파일 업로드 API
   * POST /challenges/{challengeId}/posts/upload
   */
  @PostMapping("/upload")
  public ResponseEntity<ApiResponse<com.woorido.post.dto.response.FileUploadResponse>> uploadFile(
      @PathVariable("challengeId") String challengeId,
      @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("AUTH_001:Authorization header is required");
      }
      String accessToken = authHeader.substring(7);

      if (!jwtUtil.validateToken(accessToken)) {
        throw new RuntimeException("AUTH_002:Invalid access token");
      }
      String userId = jwtUtil.getUserIdFromToken(accessToken);

      // Check Membership
      Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
      if (memberInfo == null) {
        throw new IllegalArgumentException("MEMBER_001:User is not a challenge member");
      }

      // Upload File
      String uploadedPath = imageUploadStrategy.upload(file, "attachments");

      // Build Response
      // Note: We don't save to DB here as per requirement. ID is generated for
      // display.
      Long fileId = Math.abs(java.util.UUID.randomUUID().getMostSignificantBits());
      String fileUrl = "/uploads/" + uploadedPath; // Assuming handled by static resource handler

      com.woorido.post.dto.response.FileUploadResponse response = com.woorido.post.dto.response.FileUploadResponse
          .builder()
          .fileId(fileId)
          .fileName(file.getOriginalFilename())
          .fileUrl(fileUrl)
          .fileSize(file.getSize())
          .contentType(file.getContentType())
          .build();

      return ResponseEntity.ok(ApiResponse.success(response, "파일이 업로드되었습니다"));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("MEMBER_001")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    } catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().startsWith("AUTH_")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
      }
      log.error("File Upload Error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
    }
  }
}

