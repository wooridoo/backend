package com.woorido.auth.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.woorido.auth.dto.request.EmailConfirmRequest;
import com.woorido.auth.dto.request.EmailVerifyRequest;
import com.woorido.auth.dto.request.LoginRequest;
import com.woorido.auth.dto.request.LogoutRequest;
import com.woorido.auth.dto.request.PasswordResetExecuteRequest;
import com.woorido.auth.dto.request.PasswordResetRequest;
import com.woorido.auth.dto.request.RefreshRequest;
import com.woorido.auth.dto.request.SignupRequest;
import com.woorido.auth.dto.request.SocialAuthCompleteRequest;
import com.woorido.auth.dto.request.SocialAuthStartRequest;
import com.woorido.auth.dto.response.EmailConfirmResponse;
import com.woorido.auth.dto.response.EmailVerifyResponse;
import com.woorido.auth.dto.response.LoginResponse;
import com.woorido.auth.dto.response.LogoutResponse;
import com.woorido.auth.dto.response.PasswordResetExecuteResponse;
import com.woorido.auth.dto.response.PasswordResetResponse;
import com.woorido.auth.dto.response.RefreshResponse;
import com.woorido.auth.dto.response.SignupResponse;
import com.woorido.auth.dto.response.SocialAuthStartResponse;
import com.woorido.auth.dto.response.SocialProviderStatusResponse;
import com.woorido.auth.service.EmailVerificationService;
import com.woorido.auth.service.LoginService;
import com.woorido.auth.service.LogoutService;
import com.woorido.auth.service.PasswordResetService;
import com.woorido.auth.service.RefreshService;
import com.woorido.auth.service.SignupService;
import com.woorido.auth.social.SocialAuthService;
import com.woorido.common.dto.ApiResponse;
import com.woorido.common.entity.SocialProvider;

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
  private final SocialAuthService socialAuthService;

  @Value("${app.frontend.base-url:http://localhost:5173}")
  private String frontendBaseUrl;

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

  @PostMapping("/social/start")
  public ResponseEntity<ApiResponse<SocialAuthStartResponse>> startSocialAuth(
      @Valid @RequestBody SocialAuthStartRequest request) {
    try {
      SocialAuthStartResponse response = socialAuthService.start(request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_011")) {
          return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(message));
        }
        if (message.startsWith("AUTH_012") || message.startsWith("AUTH_013")) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
        }
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  @GetMapping("/social/providers")
  public ResponseEntity<ApiResponse<SocialProviderStatusResponse>> getSocialProviderStatuses() {
    SocialProviderStatusResponse response = socialAuthService.getProviderStatuses();
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping("/social/complete")
  public ResponseEntity<ApiResponse<LoginResponse>> completeSocialAuth(
      @Valid @RequestBody SocialAuthCompleteRequest request) {
    try {
      LoginResponse response = socialAuthService.complete(request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null) {
        if (message.startsWith("AUTH_011")) {
          return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(message));
        }
        if (message.startsWith("AUTH_012")
            || message.startsWith("AUTH_013")
            || message.startsWith("AUTH_014")
            || message.startsWith("AUTH_017")
            || message.startsWith("AUTH_018")
            || message.startsWith("AUTH_015")
            || message.startsWith("AUTH_016")) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
        }
        if (message.startsWith("USER_005")) {
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
        }
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  @GetMapping("/social/callback/{provider}")
  public ResponseEntity<Void> socialProviderCallback(
      @PathVariable("provider") String providerPathValue,
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "state", required = false) String state,
      @RequestParam(value = "error", required = false) String error,
      @RequestParam(value = "error_description", required = false) String errorDescription) {
    SocialProvider provider;
    try {
      provider = parseSocialProvider(providerPathValue);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }

    UriComponentsBuilder redirectBuilder = UriComponentsBuilder
        .fromHttpUrl(frontendBaseUrl)
        .path("/auth/social/callback")
        .queryParam("provider", provider.name());

    if (StringUtils.hasText(code)) {
      redirectBuilder.queryParam("code", code);
    }
    if (StringUtils.hasText(state)) {
      redirectBuilder.queryParam("state", state);
    }
    if (StringUtils.hasText(error)) {
      redirectBuilder.queryParam("error", error);
    }
    if (StringUtils.hasText(errorDescription)) {
      redirectBuilder.queryParam("error_description", errorDescription);
    }

    URI redirectUri = redirectBuilder.build(true).toUri();
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(redirectUri);
    return new ResponseEntity<>(headers, HttpStatus.FOUND);
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

  private SocialProvider parseSocialProvider(String rawValue) {
    if (!StringUtils.hasText(rawValue)) {
      throw new IllegalArgumentException("지원하지 않는 소셜 제공자입니다");
    }

    SocialProvider parsedProvider;
    try {
      parsedProvider = SocialProvider.valueOf(rawValue.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("지원하지 않는 소셜 제공자입니다");
    }

    if (parsedProvider != SocialProvider.GOOGLE && parsedProvider != SocialProvider.KAKAO) {
      throw new IllegalArgumentException("지원하지 않는 소셜 제공자입니다");
    }
    return parsedProvider;
  }
}
