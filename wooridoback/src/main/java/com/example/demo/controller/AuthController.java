package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.SignupRequest;
import com.example.demo.dto.SignupResponse;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/signup")
    public Map<String, Object> signup(@RequestBody SignupRequest request) {
        // 실제 데이터베이스 저장은 나중에 구현하고, 명세서의 'Response 예시'대로 응답합니다.
        SignupResponse data = SignupResponse.builder()
                .userId(1L)
                .email(request.getEmail())
                .nickname("홍길동")
                .status("ACTIVE")
                .createdAt("2026-01-14T10:30:00Z")
                .build();

        return Map.of(
            "success", true,
            "data", data,
            "message", "회원가입이 완료되었습니다",
            "timestamp", "2026-01-14T10:30:00Z"
        );
    }
}