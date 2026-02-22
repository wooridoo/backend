package com.woorido.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.common.dto.ApiResponse;
import com.woorido.user.dto.request.UserUpdateRequest;
import com.woorido.user.dto.request.UserPasswordChangeRequest;
import com.woorido.user.dto.request.SocialOnboardingRequest;
import com.woorido.user.dto.response.NicknameCheckResponse;
import com.woorido.user.dto.response.SocialOnboardingCompleteResponse;
import com.woorido.user.dto.response.UserPasswordChangeResponse;
import com.woorido.user.dto.response.UserProfileResponse;
import com.woorido.user.dto.response.UserPublicProfileResponse;
import com.woorido.user.dto.response.UserUpdateResponse;
import com.woorido.user.dto.response.UserWithdrawResponse;
import com.woorido.user.service.UserService;

import org.springframework.web.bind.annotation.DeleteMapping;

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

        try {
            // Authorization 헤더에서 Bearer 토큰 추출
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }

            String accessToken = authHeader.substring(7); // "Bearer " 제거

            UserProfileResponse response = userService.getMyProfile(accessToken);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (RuntimeException e) {

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

        try {
            // Authorization 헤더에서 Bearer 토큰 추출
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }

            String accessToken = authHeader.substring(7); // "Bearer " 제거

            UserUpdateResponse response = userService.updateMyProfile(accessToken, request);
            return ResponseEntity.ok(ApiResponse.success(response, "정보가 수정되었습니다"));

        } catch (RuntimeException e) {

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
     * 소셜 신규가입 사용자 온보딩 완료 API
     * PUT /users/me/social-onboarding
     */
    @PutMapping("/me/social-onboarding")
    public ResponseEntity<ApiResponse<SocialOnboardingCompleteResponse>> completeSocialOnboarding(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SocialOnboardingRequest request) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }

            String accessToken = authHeader.substring(7);
            SocialOnboardingCompleteResponse response = userService.completeSocialOnboarding(accessToken, request);
            return ResponseEntity.ok(ApiResponse.success(response, "소셜 가입 정보 입력이 완료되었습니다"));
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.startsWith("AUTH_001")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
                }
                if (message.startsWith("USER_007")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
                }
                if (message.startsWith("AUTH_013")
                        || message.startsWith("VALIDATION_001")
                        || message.startsWith("USER_006")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
                }
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
    public ResponseEntity<ApiResponse<UserPasswordChangeResponse>> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UserPasswordChangeRequest request) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }

            String accessToken = authHeader.substring(7);
            UserPasswordChangeResponse response = userService.changePassword(accessToken, request);
            return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 변경되었습니다"));
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.startsWith("AUTH_001"))
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
                if (message.startsWith("USER_003") || message.startsWith("VALIDATION_001"))
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
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
    public ResponseEntity<ApiResponse<UserWithdrawResponse>> withdraw(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody com.woorido.user.dto.request.UserWithdrawRequest request) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }
            String accessToken = authHeader.substring(7);

            UserWithdrawResponse response = userService.withdrawUser(accessToken, request);
            return ResponseEntity.ok(ApiResponse.success(response, "탈퇴 처리되었습니다. 30일 내 재가입 시 데이터가 복구됩니다."));

        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.startsWith("AUTH_001"))
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
                if (message.startsWith("USER_003") || message.startsWith("USER_008") || message.startsWith("USER_009"))
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
        }
    }

    /**
     * 사용자 공개 정보 조회 API
     * GET /users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPublicProfileResponse>> getUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("userId") String userId) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }
            String accessToken = authHeader.substring(7);
            UserPublicProfileResponse response = userService.getUserProfile(accessToken, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.startsWith("AUTH_001"))
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
                if (message.startsWith("USER_001"))
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
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
