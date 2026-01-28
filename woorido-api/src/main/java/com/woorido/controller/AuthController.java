package com.woorido.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.dto.request.LoginRequest;
import com.woorido.dto.response.ApiResponse;
import com.woorido.dto.response.LoginResponse;
import com.woorido.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        
        System.out.println("========== LOGIN 요청 들어옴 ==========");
        System.out.println("email: " + request.getEmail());
        System.out.println("password: " + request.getPassword());
        
        try {
            LoginResponse response = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            System.out.println("에러 발생: " + e.getMessage());
            e.printStackTrace();
            
            String message = e.getMessage();
            if (message.startsWith("AUTH_001") || message.startsWith("AUTH_002")) {
                return ResponseEntity.status(401).body(ApiResponse.error(null));
            } else if (message.startsWith("USER_005")) {
                return ResponseEntity.status(403).body(ApiResponse.error(null));
            }
            return ResponseEntity.status(500).body(ApiResponse.error(null));
        }
    }
}