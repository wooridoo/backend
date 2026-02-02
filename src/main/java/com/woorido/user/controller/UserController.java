package com.woorido.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.common.dto.ApiResponse;
import com.woorido.user.dto.request.ChangePasswordRequest;
import com.woorido.user.dto.request.UserUpdateRequest;
import com.woorido.user.dto.request.WithdrawRequest;
import com.woorido.user.dto.response.ChangePasswordResponse;
import com.woorido.user.dto.response.NicknameCheckResponse;
import com.woorido.user.dto.response.UserProfileResponse;
import com.woorido.user.dto.response.UserUpdateResponse;
import com.woorido.user.dto.response.WithdrawResponse;
import com.woorido.user.dto.response.UserPublicProfileResponse;
import com.woorido.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

        try {
            // Authorization 헤더에서 Bearer 토큰 추출
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }

            String accessToken = authHeader.substring(7); // "Bearer " 제거

            UserProfileResponse response = userService.getMyProfile(accessToken);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (RuntimeException e) {
            log.warn("에러 발생: {}", e.getMessage());

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
     * 사용자 공개 프로필 조회 API (API 013)
     * GET /users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPublicProfileResponse>> getUserPublicProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @org.springframework.web.bind.annotation.PathVariable("userId") String userId) {

        log.info("GET /users/{} 요청 들어옴", userId);

        try {
            // 1. 요청자 식별 (Optional)
            String currentUserId = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String accessToken = authHeader.substring(7);
                    // 토큰에서 userId 추출을 위해 UserService 등의 도움 필요하지만
                    // 여기서는 간단히 null 처리 혹은 추후 보완
                } catch (Exception e) {
                    // ignore
                }
            }

            // 2. 서비스 호출
            UserPublicProfileResponse response = userService.getUserPublicProfile(userId, currentUserId);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (RuntimeException e) {
            log.error("에러 발생", e); // 스택 트레이스 출력
            String message = e.getMessage();
            if (message != null && message.startsWith("USER_001")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
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

        try {
            // Authorization 헤더에서 Bearer 토큰 추출
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }

            String accessToken = authHeader.substring(7); // "Bearer " 제거

            UserUpdateResponse response = userService.updateMyProfile(accessToken, request);
            return ResponseEntity.ok(ApiResponse.success(response, "정보가 수정되었습니다"));

        } catch (RuntimeException e) {
            log.warn("에러 발생: {}", e.getMessage());

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

        try {
            NicknameCheckResponse response = userService.checkNicknameAvailability(nickname);

            if (response.getIsAvailable()) {
                return ResponseEntity.ok(ApiResponse.success(response, "사용 가능한 닉네임입니다"));
            } else {
                return ResponseEntity.ok(ApiResponse.success(response, "이미 사용 중인 닉네임입니다"));
            }

        } catch (RuntimeException e) {
            log.warn("에러 발생: {}", e.getMessage());

            String message = e.getMessage();
            if (message != null && message.startsWith("USER_006")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다"));
        }
    }

    /**
     * 비밀번호 변경 API
     * PUT /users/me/password
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {

        log.info("PUT /users/me/password 요청 들어옴");

        try {
            // Authorization 헤더에서 Bearer 토큰 추출
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }

            String accessToken = authHeader.substring(7); // "Bearer " 제거

            ChangePasswordResponse response = userService.changePassword(accessToken, request);
            return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 변경되었습니다"));

        } catch (RuntimeException e) {
            log.warn("에러 발생: {}", e.getMessage());

            String message = e.getMessage();
            if (message != null && message.startsWith("AUTH_001")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(message));
            } else if (message != null && message.startsWith("USER_003")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(message));
            } else if (message != null && message.startsWith("VALIDATION_001")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다"));
        }
    }

    /**
     * 회원 탈퇴 API
     * DELETE /users/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<WithdrawResponse>> withdrawUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody WithdrawRequest request) {

        log.info("DELETE /users/me 요청 들어옴");

        try {
            // Authorization 헤더에서 Bearer 토큰 추출
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }

            String accessToken = authHeader.substring(7); // "Bearer " 제거

            WithdrawResponse response = userService.withdrawUser(accessToken, request);
            return ResponseEntity.ok(ApiResponse.success(response, "탈퇴 처리되었습니다. 30일 내 재가입 시 데이터가 복구됩니다."));

        } catch (RuntimeException e) {
            log.warn("에러 발생: {}", e.getMessage());

            String message = e.getMessage();
            if (message != null && message.startsWith("AUTH_001")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(message));
            } else if (message != null && message.startsWith("USER_003")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(message));
            } else if (message != null && (message.startsWith("USER_008") || message.startsWith("USER_009"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다"));
        }
    }
}
