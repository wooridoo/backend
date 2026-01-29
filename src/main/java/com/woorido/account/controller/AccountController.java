package com.woorido.account.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.account.dto.request.ChargeCallbackRequest;
import com.woorido.account.dto.response.ChargeCallbackResponse;
import com.woorido.account.dto.request.CreditChargeRequest;
import com.woorido.account.dto.request.TransactionSearchRequest;
import com.woorido.account.dto.request.WithdrawRequest;
import com.woorido.account.dto.response.CreditChargeResponse;
import com.woorido.account.dto.response.MyAccountResponse;
import com.woorido.account.dto.response.TransactionHistoryResponse;
import com.woorido.account.dto.response.WithdrawResponse;
import com.woorido.account.dto.request.SupportRequest;
import com.woorido.account.dto.response.SupportResponse;
import com.woorido.account.service.AccountService;
import com.woorido.common.dto.ApiResponse;

import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 거래 내역 조회 API
     * GET /accounts/me/transactions
     */
    @GetMapping("/me/transactions")
    public ResponseEntity<ApiResponse<TransactionHistoryResponse>> getTransactionHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {

        try {
            // 인증 검증
            if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }
            String accessToken = authHeader.substring(7);

            // 요청 DTO 생성
            TransactionSearchRequest request = new TransactionSearchRequest();
            request.setType(type);
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setPage(page);
            request.setSize(size);

            // 서비스 호출
            TransactionHistoryResponse response = accountService.getTransactionHistory(accessToken, request);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.startsWith("AUTH_001")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error(message));
                } else if (message.startsWith("ACCOUNT_001")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error(message));
                }
            }
            // 그 외 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(message != null ? message : "서버 오류가 발생했습니다"));
        }
    }

    /**
     * 크레딧 충전 요청 API
     * POST /accounts/charge
     */
    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<CreditChargeResponse>> requestCreditCharge(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody CreditChargeRequest request) {

        try {
            // 인증 검증
            if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }
            String accessToken = authHeader.substring(7);

            CreditChargeResponse response = accountService.requestCreditCharge(accessToken, request);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.startsWith("AUTH_001")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error(message));
                } else if (message.startsWith("ACCOUNT_002") || message.startsWith("ACCOUNT_007")
                        || message.startsWith("ACCOUNT_008")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error(message));
                }
            }
            // 그 외 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(message != null ? message : "서버 오류가 발생했습니다"));
        }
    }

    /**
     * 충전 콜백 API (API 018)
     * POST /accounts/charge/callback
     */
    @PostMapping("/charge/callback")
    public ResponseEntity<ApiResponse<ChargeCallbackResponse>> processChargeCallback(
            @RequestBody ChargeCallbackRequest request) {

        try {
            ChargeCallbackResponse response = accountService.processChargeCallback(request);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.startsWith("ACCOUNT_009") || message.startsWith("ACCOUNT_010")
                        || message.startsWith("ACCOUNT_011")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 400 Bad Request
                            .body(ApiResponse.error(message));
                } else if (message.startsWith("ACCOUNT_012") || message.startsWith("ACCOUNT_013")) {
                    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED) // 402 or 400? Spec says 200 OK for
                                                                              // failure callback usually, but here
                                                                              // request is "Process", so error.
                            .body(ApiResponse.error(message));
                }
            }
            // 그 외 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(message != null ? message : "서버 오류가 발생했습니다"));
        }
    }

    /**
     * 출금 요청 (API 019)
     * POST /accounts/withdraw
     */
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WithdrawResponse>> requestWithdraw(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody WithdrawRequest request) {

        try {
            // 인증 검증
            if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }
            String accessToken = authHeader.substring(7);

            WithdrawResponse response = accountService.requestWithdraw(accessToken, request);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.startsWith("AUTH_001")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
                } else if (message.startsWith("ACCOUNT_003") || message.startsWith("ACCOUNT_004") ||
                        message.startsWith("ACCOUNT_005") || message.startsWith("ACCOUNT_006")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(message != null ? message : "서버 오류가 발생했습니다"));
        }
    }

    /**
     * 서포트 수동 납입 (API 020)
     * POST /accounts/support
     */
    @PostMapping("/support")
    public ResponseEntity<ApiResponse<SupportResponse>> requestSupport(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SupportRequest request) {

        try {
            // 인증 검증
            if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
                throw new RuntimeException("AUTH_001:인증이 필요합니다");
            }
            String accessToken = authHeader.substring(7);

            SupportResponse response = accountService.requestSupport(accessToken, request);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.startsWith("AUTH_001")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
                } else if (message.startsWith("ACCOUNT_004") || message.startsWith("SUPPORT_001")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
                } else if (message.startsWith("CHALLENGE_003")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
                } else if (message.startsWith("CHALLENGE_001")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(message != null ? message : "서버 오류가 발생했습니다"));
        }
    }

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
