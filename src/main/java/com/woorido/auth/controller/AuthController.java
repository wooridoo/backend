package com.woorido.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.auth.dto.request.LoginRequest;
import com.woorido.auth.dto.request.LogoutRequest;
import com.woorido.auth.dto.request.PasswordResetRequest;
import com.woorido.auth.dto.request.RefreshRequest;
import com.woorido.auth.dto.request.SignupRequest;
import com.woorido.auth.dto.response.LoginResponse;
import com.woorido.auth.dto.response.LogoutResponse;
import com.woorido.auth.dto.response.PasswordResetResponse;
import com.woorido.auth.dto.response.RefreshResponse;
import com.woorido.auth.dto.response.SignupResponse;
import com.woorido.auth.service.LoginService;
import com.woorido.auth.service.LogoutService;
import com.woorido.auth.service.PasswordResetService;
import com.woorido.auth.service.RefreshService;
import com.woorido.auth.service.SignupService;
import com.woorido.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final LogoutService logoutService;
    private final PasswordResetService passwordResetService;
    private final RefreshService refreshService;
    private final SignupService signupService;

    /**
     * 로그인 API
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {

        System.out.println("========== LOGIN 요청 들어옴 ==========");
        System.out.println("email: " + request.getEmail());

        try {
            LoginResponse response = loginService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            System.out.println("에러 발생: " + e.getMessage());
            e.printStackTrace();

            String message = e.getMessage();
            if (message.startsWith("AUTH_001") || message.startsWith("AUTH_002")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(message));
            } else if (message.startsWith("USER_005")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다"));
        }
    }

    /**
     * 회원가입 API
     * POST /auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody SignupRequest request) {

        System.out.println("========== SIGNUP 요청 들어옴 ==========");
        System.out.println("email: " + request.getEmail());
        System.out.println("nickname: " + request.getNickname());

        try {
            SignupResponse response = signupService.signup(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "회원가입이 완료되었습니다"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("USER_002")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다"));
        }
    }

    /**
     * 로그아웃 API
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(@Valid @RequestBody LogoutRequest request) {

        System.out.println("========== LOGOUT 요청 들어옴 ==========");
        System.out.println("refreshToken: " + request.getRefreshToken().substring(0, 20) + "...");

        try {
            LogoutResponse response = logoutService.logout(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success(response, "로그아웃되었습니다"));
        } catch (RuntimeException e) {
            System.out.println("에러 발생: " + e.getMessage());

            String message = e.getMessage();
            if (message.startsWith("AUTH_001")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다"));
        }
    }

    /**
     * 토큰 갱신 API
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request) {

        System.out.println("========== TOKEN REFRESH 요청 들어옴 ==========");
        System.out.println("refreshToken: " + request.getRefreshToken().substring(0, 20) + "...");

        try {
            RefreshResponse response = refreshService.refresh(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            System.out.println("에러 발생: " + e.getMessage());

            String message = e.getMessage();
            if (message.startsWith("AUTH_004")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다"));
        }
    }

    /**
     * 비밀번호 재설정 요청 API
     * POST /auth/password/reset
     */
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<PasswordResetResponse>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {

        System.out.println("========== PASSWORD RESET 요청 들어옴 ==========");
        System.out.println("email: " + request.getEmail());

        try {
            PasswordResetResponse response = passwordResetService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success(response, "비밀번호 재설정 링크가 발송되었습니다"));
        } catch (RuntimeException e) {
            System.out.println("에러 발생: " + e.getMessage());

            String message = e.getMessage();
            if (message.startsWith("USER_001")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다"));
        }
    }
}
