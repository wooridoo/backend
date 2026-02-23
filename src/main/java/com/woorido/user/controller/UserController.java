package com.woorido.user.controller;

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
import com.woorido.common.util.AuthHeaderResolver;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthHeaderResolver authHeaderResolver;

    /**
     * 내 정보 조회 API
     * GET /users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String accessToken = authHeaderResolver.resolveToken(authHeader);
        UserProfileResponse response = userService.getMyProfile(accessToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 내 정보 수정 API
     * PUT /users/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserUpdateResponse>> updateMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UserUpdateRequest request) {
        String accessToken = authHeaderResolver.resolveToken(authHeader);
        UserUpdateResponse response = userService.updateMyProfile(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(response, "정보가 수정되었습니다"));
    }

    /**
     * 소셜 신규가입 사용자 온보딩 완료 API
     * PUT /users/me/social-onboarding
     */
    @PutMapping("/me/social-onboarding")
    public ResponseEntity<ApiResponse<SocialOnboardingCompleteResponse>> completeSocialOnboarding(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SocialOnboardingRequest request) {
        String accessToken = authHeaderResolver.resolveToken(authHeader);
        SocialOnboardingCompleteResponse response = userService.completeSocialOnboarding(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(response, "소셜 가입 정보 입력이 완료되었습니다"));
    }

    /**
     * 비밀번호 변경 API
     * PUT /users/me/password
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<UserPasswordChangeResponse>> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UserPasswordChangeRequest request) {
        String accessToken = authHeaderResolver.resolveToken(authHeader);
        UserPasswordChangeResponse response = userService.changePassword(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 변경되었습니다"));
    }

    /**
     * 회원 탈퇴 API
     * DELETE /users/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<UserWithdrawResponse>> withdraw(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody com.woorido.user.dto.request.UserWithdrawRequest request) {
        String accessToken = authHeaderResolver.resolveToken(authHeader);
        UserWithdrawResponse response = userService.withdrawUser(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(response, "탈퇴 처리되었습니다. 30일 내 재가입 시 데이터가 복구됩니다."));
    }

    /**
     * 사용자 공개 정보 조회 API
     * GET /users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPublicProfileResponse>> getUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("userId") String userId) {
        String accessToken = authHeaderResolver.resolveToken(authHeader);
        UserPublicProfileResponse response = userService.getUserProfile(accessToken, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 닉네임 중복 체크 API
     * GET /users/check-nickname
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname(
            @RequestParam("nickname") String nickname) {
        NicknameCheckResponse response = userService.checkNicknameAvailability(nickname);
        if (response.getIsAvailable()) {
            return ResponseEntity.ok(ApiResponse.success(response, "사용 가능한 닉네임입니다"));
        }
        return ResponseEntity.ok(ApiResponse.success(response, "이미 사용 중인 닉네임입니다"));
    }
}
