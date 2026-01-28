package com.woorido.account.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.account.dto.response.MyAccountResponse;
import com.woorido.account.service.AccountService;
import com.woorido.common.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 내 어카운트 조회 API
     * GET /accounts/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyAccountResponse>> getMyAccount(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // Authorization 헤더 검증 (대소문자 무시)
            if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }

            String accessToken = authHeader.substring(7); // "Bearer " 제거

            MyAccountResponse response = accountService.getMyAccount(accessToken);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.startsWith("AUTH_001")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error(message));
                } else if (message.startsWith("ACCOUNT_001")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND) // 계좌 없음
                            .body(ApiResponse.error(message));
                }
            }
            // 그 외 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(message != null ? message : "서버 오류가 발생했습니다"));
        }
    }
}
