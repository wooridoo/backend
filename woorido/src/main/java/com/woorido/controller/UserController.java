package com.woorido.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.dto.request.UserUpdateRequest;
import com.woorido.dto.response.ApiResponse;
import com.woorido.dto.response.NicknameCheckResponse;
import com.woorido.dto.response.UserProfileResponse;
import com.woorido.dto.response.UserUpdateResponse;
import com.woorido.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  /**
   * 내 정보 조회 API
   * GET /users/me
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    System.out.println("========== GET /users/me 요청 들어옴 ==========");

    try {
      // Authorization 헤더에서 Bearer 토큰 추출
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("AUTH_001:인증이 필요합니다");
      }

      String accessToken = authHeader.substring(7); // "Bearer " 제거
      System.out.println("accessToken: " + accessToken.substring(0, 20) + "...");

      UserProfileResponse response = userService.getMyProfile(accessToken);
      return ResponseEntity.ok(ApiResponse.success(response));

    } catch (RuntimeException e) {
      System.out.println("에러 발생: " + e.getMessage());

      String message = e.getMessage();
      if (message != null && message.startsWith("AUTH_001")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 내 정보 수정 API
   * PUT /users/me
   */
  @PutMapping("/me")
  public ResponseEntity<ApiResponse<UserUpdateResponse>> updateMyProfile(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @Valid @RequestBody UserUpdateRequest request) {

    System.out.println("========== PUT /users/me 요청 들어옴 ==========");

    try {
      // Authorization 헤더에서 Bearer 토큰 추출
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("AUTH_001:인증이 필요합니다");
      }

      String accessToken = authHeader.substring(7); // "Bearer " 제거
      System.out.println("accessToken: " + accessToken.substring(0, 20) + "...");

      UserUpdateResponse response = userService.updateMyProfile(accessToken, request);
      return ResponseEntity.ok(ApiResponse.success(response, "정보가 수정되었습니다"));

    } catch (RuntimeException e) {
      System.out.println("에러 발생: " + e.getMessage());

      String message = e.getMessage();
      if (message != null && message.startsWith("AUTH_001")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(message));
      } else if (message != null && message.startsWith("USER_006")) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(message));
      } else if (message != null && message.startsWith("USER_007")) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  /**
   * 닉네임 중복 체크 API
   * GET /users/check-nickname
   */
  @GetMapping("/check-nickname")
  public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname(
      @RequestParam("nickname") String nickname) {

    System.out.println("========== GET /users/check-nickname 요청 들어옴 ==========");
    System.out.println("nickname: " + nickname);

    try {
      NicknameCheckResponse response = userService.checkNicknameAvailability(nickname);

      if (response.getAvailable()) {
        return ResponseEntity.ok(ApiResponse.success(response, "사용 가능한 닉네임입니다"));
      } else {
        return ResponseEntity.ok(ApiResponse.success(response, "이미 사용 중인 닉네임입니다"));
      }

    } catch (RuntimeException e) {
      System.out.println("에러 발생: " + e.getMessage());

      String message = e.getMessage();
      if (message != null && message.startsWith("USER_006")) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }
}
