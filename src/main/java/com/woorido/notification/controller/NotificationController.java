package com.woorido.notification.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.util.JwtUtil;
import com.woorido.notification.domain.Notification;
import com.woorido.notification.dto.NotificationListResponse;
import com.woorido.notification.dto.NotificationResponse;
import com.woorido.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
  // Learning note:
  // - Controller parses request/header and delegates business rules to Service.
  // - Keep API response mapping here, keep domain rules in Service.

  private final NotificationService notificationService;
  private final JwtUtil jwtUtil;

  @GetMapping
  public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    try {
      String userId = validateAndGetUserId(authHeader);

      List<Notification> notifications = notificationService.getNotifications(userId);
      int unreadCount = notificationService.getUnreadCount(userId);

      List<NotificationResponse> content = notifications.stream()
          .map(NotificationResponse::from)
          .collect(Collectors.toList());

      NotificationListResponse response = NotificationListResponse.builder()
          .content(content)
          .unreadCount(unreadCount)
          .totalElements(content.size())
          .totalPages(1)
          .build();

      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PutMapping("/{id}/read")
  public ResponseEntity<ApiResponse<Void>> markAsRead(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @PathVariable String id) {

    try {
      String userId = validateAndGetUserId(authHeader);
      notificationService.markAsRead(id, userId);
      return ResponseEntity.ok(ApiResponse.success(null));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  private String validateAndGetUserId(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001:인증이 필요합니다");
    }

    String token = authHeader.substring(7);
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
    if (message.startsWith("NOTIFICATION_002")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
    }
    if (message.startsWith("NOTIFICATION_001")) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("서버 오류가 발생했습니다: " + message));
  }
}
