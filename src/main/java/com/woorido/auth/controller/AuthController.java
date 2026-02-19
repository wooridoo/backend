package com.woorido.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.auth.dto.request.EmailConfirmRequest;
import com.woorido.auth.dto.request.EmailVerifyRequest;
import com.woorido.auth.dto.request.LoginRequest;
import com.woorido.auth.dto.request.LogoutRequest;
import com.woorido.auth.dto.request.PasswordResetExecuteRequest;
import com.woorido.auth.dto.request.PasswordResetRequest;
import com.woorido.auth.dto.request.RefreshRequest;
import com.woorido.auth.dto.request.SignupRequest;
import com.woorido.auth.dto.response.EmailConfirmResponse;
import com.woorido.auth.dto.response.EmailVerifyResponse;
import com.woorido.auth.dto.response.LoginResponse;
import com.woorido.auth.dto.response.LogoutResponse;
import com.woorido.auth.dto.response.PasswordResetExecuteResponse;
import com.woorido.auth.dto.response.PasswordResetResponse;
import com.woorido.auth.dto.response.RefreshResponse;
import com.woorido.auth.dto.response.SignupResponse;
import com.woorido.auth.service.EmailVerificationService;
import com.woorido.auth.service.LoginService;
import com.woorido.auth.service.LogoutService;
import com.woorido.auth.service.PasswordResetService;
import com.woorido.auth.service.RefreshService;
import com.woorido.auth.service.SignupService;
import com.woorido.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final LoginService loginService;
  private final LogoutService logoutService;
  private final PasswordResetService passwordResetService;
  private final RefreshService refreshService;
  private final SignupService signupService;
  private final EmailVerificationService emailVerificationService;

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
    try {
      LoginResponse response = loginService.login(request.getEmail(), request.getPassword());
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      log.warn("로그인 실패: {}", e.getMessage());
      String message = e.getMessage();
      if (message != null && (message.startsWith("AUTH_001") || message.startsWith("AUTH_002"))) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
      }
      if (message != null && message.startsWith("USER_005")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody SignupRequest request) {
    try {
      SignupResponse response = signupService.signup(request);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(response, "회원가입이 완료되었습니다"));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("USER_002")) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  @PostMapping("/email/verify")
  public ResponseEntity<ApiResponse<EmailVerifyResponse>> verifyEmail(
      @Valid @RequestBody EmailVerifyRequest request) {
    try {
      EmailVerifyResponse response = emailVerificationService.issueVerifyCode(request.getEmail());
      return ResponseEntity.ok(ApiResponse.success(response, "인증 코드가 발송되었습니다"));
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    }
  }

  @PostMapping("/email/confirm")
  public ResponseEntity<ApiResponse<EmailConfirmResponse>> confirmEmail(
      @Valid @RequestBody EmailConfirmRequest request) {
    try {
      EmailConfirmResponse response = emailVerificationService.confirm(request.getEmail(), request.getCode());
      return ResponseEntity.ok(ApiResponse.success(response, "이메일 인증이 완료되었습니다"));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("AUTH_007")) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<LogoutResponse>> logout(@Valid @RequestBody LogoutRequest request) {
    try {
      LogoutResponse response = logoutService.logout(request.getRefreshToken());
      return ResponseEntity.ok(ApiResponse.success(response, "로그아웃되었습니다"));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("AUTH_001")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
    try {
      RefreshResponse response = refreshService.refresh(request.getRefreshToken());
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("AUTH_004")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  @PostMapping("/password/reset")
  public ResponseEntity<ApiResponse<PasswordResetResponse>> requestPasswordReset(
      @Valid @RequestBody PasswordResetRequest request) {

    try {
      PasswordResetResponse response = passwordResetService.requestPasswordReset(request.getEmail());
      return ResponseEntity.ok(ApiResponse.success(response, "비밀번호 재설정 링크가 발송되었습니다"));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("USER_001")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
      }
      if (message != null && message.startsWith("AUTH_010")) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  @PutMapping("/password/reset")
  public ResponseEntity<ApiResponse<PasswordResetExecuteResponse>> resetPassword(
      @Valid @RequestBody PasswordResetExecuteRequest request) {

    try {
      PasswordResetExecuteResponse response = passwordResetService.resetPassword(request);
      return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 재설정되었습니다"));

    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && (message.startsWith("AUTH_009") || message.startsWith("VALIDATION_001"))) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }
}
