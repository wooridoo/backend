package com.woorido.expense.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.util.JwtUtil;
import com.woorido.expense.dto.request.ApproveExpenseRequest;
import com.woorido.expense.dto.request.CreateExpenseRequest;
import com.woorido.expense.dto.request.UpdateExpenseRequest;
import com.woorido.expense.dto.response.ExpenseListResponse;
import com.woorido.expense.dto.response.ExpenseResponse;
import com.woorido.expense.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/challenges/{challengeId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {
  private final ExpenseService expenseService;
  private final JwtUtil jwtUtil;

  @GetMapping
  public ResponseEntity<ApiResponse<ExpenseListResponse>> getExpenses(
      @PathVariable String challengeId,
      @RequestHeader("Authorization") String authorization,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size) {
    try {
      String userId = extractUserId(authorization);
      ExpenseListResponse response = expenseService.getExpenses(challengeId, userId, status, page, size);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @GetMapping("/{expenseId}")
  public ResponseEntity<ApiResponse<ExpenseResponse>> getExpense(
      @PathVariable String challengeId,
      @PathVariable String expenseId,
      @RequestHeader("Authorization") String authorization) {
    try {
      String userId = extractUserId(authorization);
      ExpenseResponse response = expenseService.getExpense(challengeId, expenseId, userId);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PostMapping
  public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
      @PathVariable String challengeId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody CreateExpenseRequest request) {
    try {
      String userId = extractUserId(authorization);
      ExpenseResponse response = expenseService.createExpense(challengeId, userId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PutMapping("/{expenseId}/approve")
  public ResponseEntity<ApiResponse<ExpenseResponse>> approveExpense(
      @PathVariable String challengeId,
      @PathVariable String expenseId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody ApproveExpenseRequest request) {
    try {
      String userId = extractUserId(authorization);
      ExpenseResponse response = expenseService.approveExpense(challengeId, expenseId, userId, request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PutMapping("/{expenseId}")
  public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
      @PathVariable String challengeId,
      @PathVariable String expenseId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody UpdateExpenseRequest request) {
    try {
      String userId = extractUserId(authorization);
      ExpenseResponse response = expenseService.updateExpense(challengeId, expenseId, userId, request);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @DeleteMapping("/{expenseId}")
  public ResponseEntity<ApiResponse<Void>> deleteExpense(
      @PathVariable String challengeId,
      @PathVariable String expenseId,
      @RequestHeader("Authorization") String authorization) {
    try {
      String userId = extractUserId(authorization);
      expenseService.deleteExpense(challengeId, expenseId, userId);
      return ResponseEntity.ok(ApiResponse.success(null));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  private String extractUserId(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001:인증이 필요합니다");
    }
    String token = authorization.substring(7);
    if (!jwtUtil.validateToken(token) || !jwtUtil.isAccessToken(token)) {
      throw new RuntimeException("AUTH_001:인증이 필요합니다");
    }
    return jwtUtil.getUserIdFromToken(token);
  }

  private <T> ResponseEntity<ApiResponse<T>> handleError(RuntimeException e) {
    String message = e.getMessage() != null ? e.getMessage() : "서버 오류가 발생했습니다";
    if (message.startsWith("AUTH_")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }
    if (message.startsWith("CHALLENGE_003") || message.startsWith("EXPENSE_005")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
    }
    if (message.startsWith("CHALLENGE_001") || message.startsWith("EXPENSE_001") || message.startsWith("MEETING_001")) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }
    if (message.startsWith("EXPENSE_") || message.startsWith("ACCOUNT_")) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
  }
}
