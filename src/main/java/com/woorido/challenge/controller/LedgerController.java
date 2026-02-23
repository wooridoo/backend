package com.woorido.challenge.controller;

import com.woorido.challenge.dto.request.CreateLedgerEntryRequest;
import com.woorido.challenge.dto.request.UpdateLedgerEntryRequest;
import com.woorido.challenge.dto.response.LedgerEntryResponse;
import com.woorido.challenge.dto.response.LedgerListResponse;
import com.woorido.challenge.dto.response.LedgerSummaryResponse;
import com.woorido.challenge.service.LedgerService;
import com.woorido.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LedgerController {
  private final LedgerService ledgerService;

  @GetMapping("/challenges/{challengeId}/ledger")
  public ResponseEntity<ApiResponse<LedgerListResponse>> getLedger(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size) {

    try {
      LedgerListResponse response = ledgerService.getLedger(challengeId, authorization, page, size);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @GetMapping("/challenges/{challengeId}/ledger/summary")
  public ResponseEntity<ApiResponse<LedgerSummaryResponse>> getLedgerSummary(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization) {

    try {
      LedgerSummaryResponse response = ledgerService.getLedgerSummary(challengeId, authorization);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PostMapping("/challenges/{challengeId}/ledger")
  public ResponseEntity<ApiResponse<LedgerEntryResponse>> createLedgerEntry(
      @PathVariable("challengeId") String challengeId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody CreateLedgerEntryRequest request) {

    try {
      LedgerEntryResponse response = ledgerService.createLedgerEntry(challengeId, authorization, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "장부 항목이 등록되었습니다"));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PutMapping("/ledger/{entryId}")
  public ResponseEntity<ApiResponse<LedgerEntryResponse>> updateLedgerEntry(
      @PathVariable("entryId") String entryId,
      @RequestHeader("Authorization") String authorization,
      @RequestBody UpdateLedgerEntryRequest request) {

    try {
      LedgerEntryResponse response = ledgerService.updateLedgerEntry(entryId, authorization, request);
      return ResponseEntity.ok(ApiResponse.success(response, "장부 항목이 수정되었습니다"));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  private <T> ResponseEntity<ApiResponse<T>> handleError(RuntimeException e) {
    String message = e.getMessage() != null ? e.getMessage() : "서버 오류가 발생했습니다";

    if (message.startsWith("AUTH_")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }
    if (message.startsWith("CHALLENGE_003") || message.startsWith("LEDGER_003")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
    }
    if (message.startsWith("CHALLENGE_001") || message.startsWith("LEDGER_001")) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }
    if (message.startsWith("No enum constant")) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("LEDGER_002:지원하지 않는 장부 유형입니다"));
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("서버 오류가 발생했습니다"));
  }
}
