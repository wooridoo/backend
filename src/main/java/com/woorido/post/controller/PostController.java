package com.woorido.post.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.exception.RateLimitExceededException;
import com.woorido.common.exception.ImageValidationException;
import com.woorido.common.image.ImagePolicyType;
import com.woorido.common.image.ImageUploadService;
import com.woorido.common.util.AuthHeaderResolver;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import com.woorido.post.dto.request.CreatePostRequest;
import com.woorido.post.dto.request.PinPostRequest;
import com.woorido.post.dto.request.UpdatePostRequest;
import com.woorido.post.dto.response.CreatePostResponse;
import com.woorido.post.dto.response.PostImagesUploadResponse;
import com.woorido.post.dto.response.PinPostResponse;
import com.woorido.post.dto.response.PostDetailResponse;
import com.woorido.post.dto.response.PostListResponse;
import com.woorido.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

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
  private final AuthHeaderResolver authHeaderResolver;
  private final com.woorido.common.strategy.ImageUploadStrategy imageUploadStrategy;
  private final ImageUploadService imageUploadService;
  private final com.woorido.challenge.repository.ChallengeMemberMapper challengeMemberMapper; // Need to verify
                                                                                              // membership

  @Value("${app.backend.base-url:http://localhost:8080}")
  private String backendBaseUrl;

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
      String userId = resolveUserId(authHeader);

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
      if (message != null && message.startsWith("POST_006")) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(message));
      }
      log.error("Create Post Error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
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
      String userId = resolveUserId(authHeader);

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
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
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
      String userId = resolveUserId(authHeader);

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
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
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
      String userId = resolveUserId(authHeader);

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
      if (message != null && message.startsWith("POST_006")) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
      }
      log.error("Update Post Error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 게시글 상단 고정/해제 API
   * PUT /challenges/{challengeId}/posts/{postId}/pin
   */
  @PutMapping("/{postId}/pin")
  public ResponseEntity<ApiResponse<PinPostResponse>> setPostPinned(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody PinPostRequest request) {

    try {
      String userId = resolveUserId(authHeader);

      if (request.getPinned() == null) {
        throw new IllegalArgumentException("VALIDATION_001:pinned 값이 필요합니다");
      }

      PinPostResponse response = postService.setPostPinned(challengeId, postId, userId, request.getPinned());
      String message = request.getPinned() ? "게시글이 상단에 고정되었습니다" : "게시글 고정이 해제되었습니다";

      return ResponseEntity.ok(ApiResponse.success(response, message));

    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message != null &&
          (message.startsWith("MEMBER_001") || message.startsWith("POST_002") || message.startsWith("POST_005"))) {
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
      log.error("Set Post Pin Error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
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
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      HttpServletRequest httpServletRequest) {

    try {
      String userId = resolveUserId(authHeader);
      String clientIp = extractClientIp(httpServletRequest);

      com.woorido.post.dto.response.PostLikeResponse response = postService.toggleLike(
          challengeId,
          postId,
          userId,
          clientIp);

      String message = response.isLiked() ? "Post liked" : "Post like removed";
      return ResponseEntity.ok(ApiResponse.success(response, message));

    } catch (RateLimitExceededException e) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
          .body(ApiResponse.error(e.getMessage()));
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
   * 게시글 좋아요 취소 API
   * DELETE /challenges/{challengeId}/posts/{postId}/like
   */
  @DeleteMapping("/{postId}/like")
  public ResponseEntity<ApiResponse<com.woorido.post.dto.response.PostLikeResponse>> unlikePost(
      @PathVariable("challengeId") String challengeId,
      @PathVariable("postId") String postId,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      HttpServletRequest httpServletRequest) {

    try {
      String userId = resolveUserId(authHeader);
      String clientIp = extractClientIp(httpServletRequest);

      com.woorido.post.dto.response.PostLikeResponse response = postService.unlikePost(
          challengeId,
          postId,
          userId,
          clientIp);
      return ResponseEntity.ok(ApiResponse.success(response, "Post like removed"));
    } catch (RateLimitExceededException e) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
          .body(ApiResponse.error(e.getMessage()));
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
      log.error("Unlike Post Error", e);
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
      String userId = resolveUserId(authHeader);

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
   * 게시글 이미지 다중 업로드 API
   * POST /challenges/{challengeId}/posts/images
   */
  @PostMapping("/images")
  public ResponseEntity<ApiResponse<PostImagesUploadResponse>> uploadPostImages(
      @PathVariable("challengeId") String challengeId,
      @RequestParam(value = "files", required = false) List<MultipartFile> files,
      @RequestParam(value = "files[]", required = false) List<MultipartFile> filesBracket,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    String userId = null;
    try {
      userId = resolveUserId(authHeader);

      Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
      if (memberInfo == null || "LEFT".equals(memberInfo.get("STATUS"))) {
        throw new IllegalArgumentException("MEMBER_001:User is not a challenge member");
      }

      List<MultipartFile> uploadFiles = mergeUploadFiles(files, filesBracket);
      if (uploadFiles.isEmpty()) {
        throw new ImageValidationException("IMAGE_004", "업로드할 이미지가 없습니다");
      }

      List<String> imageUrls = imageUploadService.uploadBatch(
          uploadFiles,
          ImagePolicyType.POST_ATTACHMENT,
          "posts/" + challengeId);

      PostImagesUploadResponse response = PostImagesUploadResponse.builder()
          .imageUrls(imageUrls)
          .uploadedCount(imageUrls.size())
          .build();

      return ResponseEntity.ok(ApiResponse.success(response, "이미지가 업로드되었습니다"));

    } catch (IllegalArgumentException e) {
      throw e;
    } catch (RuntimeException e) {
      log.error(
          "Post image upload error: endpoint=/challenges/{}/posts/images, policy={}, userId={}",
          challengeId,
          ImagePolicyType.POST_ATTACHMENT.name(),
          userId,
          e);
      throw e;
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
      String userId = resolveUserId(authHeader);

      // Check Membership
      Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
      if (memberInfo == null || "LEFT".equals(memberInfo.get("STATUS"))) {
        throw new IllegalArgumentException("MEMBER_001:User is not a challenge member");
      }

      // Upload File
      String uploadedPath = imageUploadStrategy.upload(file, "attachments");

      // Build Response
      // Note: We don't save to DB here as per requirement. ID is generated for
      // display.
      Long fileId = Math.abs(java.util.UUID.randomUUID().getMostSignificantBits());
      String fileUrl = buildAbsoluteUploadUrl(uploadedPath);

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
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  private List<MultipartFile> mergeUploadFiles(List<MultipartFile> files, List<MultipartFile> filesBracket) {
    List<MultipartFile> merged = new ArrayList<>();
    if (files != null) {
      merged.addAll(files);
    }
    if (filesBracket != null) {
      merged.addAll(filesBracket);
    }
    return merged.stream().filter(file -> file != null && !file.isEmpty()).toList();
  }

  private String buildAbsoluteUploadUrl(String uploadedPath) {
    String baseUrl = backendBaseUrl.endsWith("/")
        ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1)
        : backendBaseUrl;
    String normalizedPath = uploadedPath.startsWith("/") ? uploadedPath.substring(1) : uploadedPath;
    return baseUrl + "/uploads/" + normalizedPath;
  }

  private String resolveUserId(String authHeader) {
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
}


