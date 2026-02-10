package com.woorido.notification.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.util.JwtUtil;
import com.woorido.notification.domain.Notification;
import com.woorido.notification.dto.NotificationListResponse;
import com.woorido.notification.dto.NotificationResponse;
import com.woorido.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final JwtUtil jwtUtil;

  @GetMapping
  public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

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
  }

  @PutMapping("/{id}/read")
  public ResponseEntity<ApiResponse<Void>> markAsRead(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @PathVariable String id) {

    String userId = validateAndGetUserId(authHeader);
    notificationService.markAsRead(id, userId);

    return ResponseEntity.ok(ApiResponse.success(null));
  }

  private String validateAndGetUserId(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001:인증이 필요합니다");
    }

    String token = authHeader.substring(7);
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001:인증이 필요합니다");
    }

    return jwtUtil.getUserIdFromToken(token);
  }
}
