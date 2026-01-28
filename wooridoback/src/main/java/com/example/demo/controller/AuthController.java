package com.example.demo.controller;

import com.example.demo.dto.SignupRequest;
import com.example.demo.dto.SignupResponse;
import com.example.demo.service.AuthService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        SignupResponse responseData = authService.signup(request);

        // API 정의서의 Response 예시 규격 준수
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "data", responseData,
            "message", "회원가입이 완료되었습니다",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}