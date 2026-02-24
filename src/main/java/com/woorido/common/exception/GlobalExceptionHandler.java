package com.woorido.common.exception;

import com.woorido.common.dto.ApiResponse;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  // Learning note:
  // - Converts code-based exceptions to HTTP status and unified API error response.

  private static final Set<String> NOT_FOUND_CODES = Set.of(
      "ACCOUNT_001",
      "CHALLENGE_001",
      "COMMENT_002",
      "LEDGER_001",
      "MEETING_001",
      "POST_001",
      "USER_001",
      "VOTE_001");

  private static final Set<String> FORBIDDEN_CODES = Set.of(
      "MEMBER_001",
      "MEMBER_002",
      "CHALLENGE_003",
      "CHALLENGE_004",
      "COMMENT_003",
      "POST_002",
      "POST_004",
      "POST_005",
      "NOTIFICATION_002",
      "VOTE_003",
      "VOTE_007",
      "VOTE_008");

  private static final Set<String> CONFLICT_CODES = Set.of(
      "ACCOUNT_010",
      "CHALLENGE_011",
      "USER_007",
      "VOTE_006");

  private static final Set<String> BAD_REQUEST_PREFIXES = Set.of(
      "ACCOUNT_",
      "AUTH_",
      "BRIX_",
      "CHALLENGE_",
      "COMMENT_",
      "FILE_",
      "IMAGE_",
      "LEDGER_",
      "MEETING_",
      "NOTIFICATION_",
      "POST_",
      "SEARCH_",
      "SUPPORT_",
      "USER_",
      "VALIDATION_",
      "VOTE_");

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("IMAGE_002:업로드 가능한 최대 파일 용량을 초과했습니다"));
  }

  @ExceptionHandler(MultipartException.class)
  public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("IMAGE_002:업로드 가능한 최대 파일 용량을 초과했습니다"));
  }

  @ExceptionHandler(ImageValidationException.class)
  public ResponseEntity<ApiResponse<Void>> handleImageValidationException(ImageValidationException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(e.getApiMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
    return handleCodedException(e, false);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e) {
    return handleCodedException(e, false);
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException e) {
    return handleCodedException(e, true);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
    return handleCodedException(e, false);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    log.error("Unhandled exception", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("서버 오류가 발생했습니다"));
  }

  private ResponseEntity<ApiResponse<Void>> handleCodedException(Exception e, boolean defaultForbidden) {
    String message = e.getMessage();
    String code = extractCode(message);
    HttpStatus status = resolveStatus(code, defaultForbidden);

    if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
      log.error("Unhandled exception", e);
      return ResponseEntity.status(status).body(ApiResponse.error("서버 오류가 발생했습니다"));
    }

    if (message == null || message.isBlank()) {
      return ResponseEntity.status(status).body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
    return ResponseEntity.status(status).body(ApiResponse.error(message));
  }

  private HttpStatus resolveStatus(String code, boolean defaultForbidden) {
    if (code == null) {
      return defaultForbidden ? HttpStatus.FORBIDDEN : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    if (code.startsWith("AUTH_")) {
      return HttpStatus.UNAUTHORIZED;
    }
    if ("LEDGER_004".equals(code)) {
      return HttpStatus.SERVICE_UNAVAILABLE;
    }
    if ("LEDGER_010".equals(code) || "LEDGER_011".equals(code)) {
      return HttpStatus.BAD_GATEWAY;
    }
    if (NOT_FOUND_CODES.contains(code)) {
      return HttpStatus.NOT_FOUND;
    }
    if (FORBIDDEN_CODES.contains(code)) {
      return HttpStatus.FORBIDDEN;
    }
    if (CONFLICT_CODES.contains(code)) {
      return HttpStatus.CONFLICT;
    }
    if (startsWithAny(code, BAD_REQUEST_PREFIXES)) {
      return HttpStatus.BAD_REQUEST;
    }
    return defaultForbidden ? HttpStatus.FORBIDDEN : HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private String extractCode(String message) {
    if (message == null || message.isBlank()) {
      return null;
    }
    int separator = message.indexOf(':');
    String candidate = separator >= 0 ? message.substring(0, separator) : message;
    return candidate.trim().isEmpty() ? null : candidate.trim();
  }

  private boolean startsWithAny(String value, Set<String> prefixes) {
    for (String prefix : prefixes) {
      if (value.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
