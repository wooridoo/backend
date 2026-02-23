package com.woorido.account.controller;

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
import com.woorido.common.util.AuthHeaderResolver;

import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AuthHeaderResolver authHeaderResolver;

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
        String accessToken = authHeaderResolver.resolveToken(authHeader);

        TransactionSearchRequest request = new TransactionSearchRequest();
        request.setType(type);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(page);
        request.setSize(size);

        TransactionHistoryResponse response = accountService.getTransactionHistory(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 크레딧 충전 요청 API
     * POST /accounts/charge
     */
    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<CreditChargeResponse>> requestCreditCharge(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody CreditChargeRequest request) {
        String accessToken = authHeaderResolver.resolveToken(authHeader);
        CreditChargeResponse response = accountService.requestCreditCharge(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 충전 콜백 API (API 018)
     * POST /accounts/charge/callback
     */
    @PostMapping("/charge/callback")
    public ResponseEntity<ApiResponse<ChargeCallbackResponse>> processChargeCallback(
            @RequestBody ChargeCallbackRequest request) {
        ChargeCallbackResponse response = accountService.processChargeCallback(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 출금 요청 (API 019)
     * POST /accounts/withdraw
     */
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WithdrawResponse>> requestWithdraw(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody WithdrawRequest request) {
        String accessToken = authHeaderResolver.resolveToken(authHeader);
        WithdrawResponse response = accountService.requestWithdraw(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 서포트 수동 납입 (API 020)
     * POST /accounts/support
     */
    @PostMapping("/support")
    public ResponseEntity<ApiResponse<SupportResponse>> requestSupport(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SupportRequest request) {
        String accessToken = authHeaderResolver.resolveToken(authHeader);
        SupportResponse response = accountService.requestSupport(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyAccountResponse>> getMyAccount(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String accessToken = authHeaderResolver.resolveToken(authHeader);
        MyAccountResponse response = accountService.getMyAccount(accessToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
