package com.woorido.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.dto.request.LoginRequest;
import com.woorido.dto.request.SignupRequest;
import com.woorido.dto.response.ApiResponse;
import com.woorido.dto.response.LoginResponse;
import com.woorido.dto.response.SignupResponse;
import com.woorido.service.LoginService;
import com.woorido.service.SignupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final LoginService loginService;
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
      if (message.startsWith("USER_002")) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }
}
