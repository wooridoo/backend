package com.woorido.django.brix.controller;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woorido.common.dto.ApiResponse;
import com.woorido.django.brix.dto.BrixBatchResult;
import com.woorido.django.brix.dto.BrixRecalculateRequest;
import com.woorido.django.brix.service.BrixBatchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/django/brix")
public class BrixInternalController {

  @Value("${brix.batch.zone:Asia/Seoul}")
  private String batchZone;

  @Value("${brix.batch.internal-api-key:${django.brix.api-key:}}")
  private String internalApiKey;

  private final BrixBatchService brixBatchService;

  @PostMapping("/recalculate")
  public ResponseEntity<ApiResponse<BrixBatchResult>> recalculate(
      @RequestHeader(value = "X-Internal-Api-Key", required = false) String requestApiKey,
      @RequestBody(required = false) BrixRecalculateRequest request) {
    if (internalApiKey == null || internalApiKey.isBlank()
        || requestApiKey == null
        || !requestApiKey.equals(internalApiKey)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("AUTH_001:인증이 필요합니다"));
    }

    LocalDateTime cutoffAt = LocalDateTime.now(ZoneId.of(batchZone));
    if (request != null && request.getCutoffAt() != null && !request.getCutoffAt().isBlank()) {
      try {
        cutoffAt = parseCutoffAt(request.getCutoffAt());
      } catch (DateTimeParseException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_001:cutoffAt must be ISO-8601 datetime"));
      }
    }

    try {
      BrixBatchResult result = brixBatchService.recalculate(cutoffAt);
      return ResponseEntity.ok(ApiResponse.success(result, "브릭스 수동 집계가 완료되었습니다"));
    } catch (RuntimeException e) {
      String message = e.getMessage();
      if (message != null && message.startsWith("BRIX_")) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
  }

  private LocalDateTime parseCutoffAt(String value) {
    try {
      return LocalDateTime.parse(value);
    } catch (DateTimeParseException firstException) {
      try {
        return OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.of(batchZone))
            .toLocalDateTime();
      } catch (DateTimeParseException ignored) {
        return ZonedDateTime.parse(value)
            .withZoneSameInstant(ZoneId.of(batchZone))
            .toLocalDateTime();
      }
    }
  }
}
