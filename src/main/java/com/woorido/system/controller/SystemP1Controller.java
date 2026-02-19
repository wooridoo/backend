package com.woorido.system.controller;

import com.woorido.challenge.repository.ChallengeMapper;
import com.woorido.common.dto.ApiResponse;
import com.woorido.common.util.JwtUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SystemP1Controller {
  private final JwtUtil jwtUtil;
  private final ChallengeMapper challengeMapper;

  @PostMapping("/reports")
  public ResponseEntity<ApiResponse<Map<String, Object>>> createReport(
      @RequestHeader("Authorization") String authorization,
      @RequestBody CreateReportRequest request) {

    try {
      requireAuth(authorization);
      Map<String, Object> response = Map.of(
          "reportId", UUID.randomUUID().toString(),
          "targetType", request.getTargetType(),
          "targetId", request.getTargetId(),
          "status", "PENDING",
          "createdAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(response, "신고가 접수되었습니다"));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PostMapping("/refunds")
  public ResponseEntity<ApiResponse<Map<String, Object>>> requestRefund(
      @RequestHeader("Authorization") String authorization,
      @RequestBody RefundRequest request) {

    try {
      requireAuth(authorization);
      if (request.getChallengeId() == null || challengeMapper.findById(request.getChallengeId()) == null) {
        throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
      }

      long requestedAmount = request.getAmount() != null ? request.getAmount() : 0L;
      Map<String, Object> response = Map.of(
          "refundId", UUID.randomUUID().toString(),
          "challengeId", request.getChallengeId(),
          "requestedAmount", requestedAmount,
          "estimatedAmount", requestedAmount,
          "fee", 0,
          "status", "PENDING",
          "createdAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      return ResponseEntity.ok(ApiResponse.success(response, "환불 요청이 접수되었습니다"));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @GetMapping("/search")
  public ResponseEntity<ApiResponse<Object>> search(
      @RequestHeader("Authorization") String authorization,
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size) {

    try {
      requireAuth(authorization);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(ApiResponse.error("SEARCH_001:Django 검색 서비스가 준비되지 않았습니다"));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @GetMapping("/search/challenges")
  public ResponseEntity<ApiResponse<Object>> searchChallenges(
      @RequestHeader("Authorization") String authorization,
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size) {

    try {
      requireAuth(authorization);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(ApiResponse.error("SEARCH_001:Django 검색 서비스가 준비되지 않았습니다"));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  private void requireAuth(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001:인증이 필요합니다");
    }
    String token = authorization.substring(7);
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001:인증이 필요합니다");
    }
  }

  private <T> ResponseEntity<ApiResponse<T>> handleError(RuntimeException e) {
    String message = e.getMessage() != null ? e.getMessage() : "서버 오류가 발생했습니다";
    if (message.startsWith("AUTH_")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }
    if (message.startsWith("CHALLENGE_001")) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }
    if (message.startsWith("SEARCH_")) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(message));
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
  }

  @Getter
  @Setter
  public static class CreateReportRequest {
    private String targetType;
    private String targetId;
    private String reason;
    private String description;
    private List<String> evidenceUrls;
  }

  @Getter
  @Setter
  public static class RefundRequest {
    private String challengeId;
    private String reason;
    private Long amount;
  }
}
