package com.woorido.common.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.dto.ImageUploadResponse;
import com.woorido.common.image.ImagePolicyType;
import com.woorido.common.image.ImageUploadService;
import com.woorido.common.util.AuthHeaderResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ImageUploadController {

  private final AuthHeaderResolver authHeaderResolver;
  private final ImageUploadService imageUploadService;

  @PostMapping("/uploads/challenges/banner")
  public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadChallengeBanner(
      @RequestParam("file") MultipartFile file,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String userId = null;
    try {
      userId = requireUserId(authHeader);
      String imageUrl = imageUploadService.uploadSingle(
          file,
          ImagePolicyType.CHALLENGE_BANNER,
          "challenges/banner");

      return ResponseEntity.ok(ApiResponse.success(
          ImageUploadResponse.builder().imageUrl(imageUrl).build(),
          "대표 이미지가 업로드되었습니다"));
    } catch (RuntimeException e) {
      return handleRuntimeError(e, "Banner Upload Error", "/uploads/challenges/banner", userId,
          ImagePolicyType.CHALLENGE_BANNER);
    }
  }

  @PostMapping("/uploads/challenges/thumbnail")
  public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadChallengeThumbnail(
      @RequestParam("file") MultipartFile file,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String userId = null;
    try {
      userId = requireUserId(authHeader);
      String imageUrl = imageUploadService.uploadSingle(
          file,
          ImagePolicyType.CHALLENGE_THUMBNAIL,
          "challenges/thumbnail");

      return ResponseEntity.ok(ApiResponse.success(
          ImageUploadResponse.builder().imageUrl(imageUrl).build(),
          "프로필 이미지가 업로드되었습니다"));
    } catch (RuntimeException e) {
      return handleRuntimeError(e, "Thumbnail Upload Error", "/uploads/challenges/thumbnail", userId,
          ImagePolicyType.CHALLENGE_THUMBNAIL);
    }
  }

  @PostMapping("/users/me/profile-image")
  public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadMyProfileImage(
      @RequestParam("file") MultipartFile file,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String userId = null;
    try {
      userId = requireUserId(authHeader);
      String imageUrl = imageUploadService.uploadSingle(
          file,
          ImagePolicyType.USER_PROFILE,
          "users/profile/" + userId);

      return ResponseEntity.ok(ApiResponse.success(
          ImageUploadResponse.builder().imageUrl(imageUrl).build(),
          "프로필 이미지가 업로드되었습니다"));
    } catch (RuntimeException e) {
      return handleRuntimeError(e, "Profile Upload Error", "/users/me/profile-image", userId,
          ImagePolicyType.USER_PROFILE);
    }
  }

  private String requireUserId(String authHeader) {
    return authHeaderResolver.resolveUserId(authHeader);
  }

  private ResponseEntity<ApiResponse<ImageUploadResponse>> handleRuntimeError(
      RuntimeException e,
      String logLabel,
      String endpoint,
      String userId,
      ImagePolicyType policyType) {
    String message = e.getMessage();
    if (message != null && message.startsWith("AUTH_")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }
    log.error(
        "{}: endpoint={}, policy={}, userId={}",
        logLabel,
        endpoint,
        policyType.name(),
        userId,
        e);
    throw e;
  }
}
