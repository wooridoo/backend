package com.woorido.notification.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.util.AuthHeaderResolver;
import com.woorido.notification.domain.Notification;
import com.woorido.notification.domain.NotificationSettings;
import com.woorido.notification.dto.request.UpdateNotificationSettingsRequest;
import com.woorido.notification.dto.NotificationListResponse;
import com.woorido.notification.dto.NotificationMarkReadResponse;
import com.woorido.notification.dto.NotificationReadAllResponse;
import com.woorido.notification.dto.NotificationResponse;
import com.woorido.notification.dto.response.NotificationSettingsResponse;
import com.woorido.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
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
  private final AuthHeaderResolver authHeaderResolver;

  @GetMapping
  public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "type", required = false) String type,
      @RequestParam(value = "isRead", required = false) Boolean isRead) {

    try {
      String userId = validateAndGetUserId(authHeader);

      List<Notification> notifications = notificationService.getNotifications(userId, page, size, type, isRead);
      long totalElements = notificationService.countNotifications(userId, type, isRead);
      int unreadCount = notificationService.getUnreadCount(userId);

      List<NotificationResponse> content = notifications.stream()
          .map(NotificationResponse::from)
          .collect(Collectors.toList());

      int safeSize = Math.max(size, 1);
      int totalPages = (int) Math.ceil((double) totalElements / safeSize);
      NotificationListResponse response = NotificationListResponse.builder()
          .content(content)
          .unreadCount(unreadCount)
          .totalElements(totalElements)
          .totalPages(totalPages)
          .number(Math.max(page, 0))
          .size(safeSize)
          .build();

      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @GetMapping("/{notificationId}")
  public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @PathVariable("notificationId") String notificationId) {

    try {
      String userId = validateAndGetUserId(authHeader);
      Notification notification = notificationService.getNotification(notificationId, userId);
      return ResponseEntity.ok(ApiResponse.success(NotificationResponse.from(notification)));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PutMapping("/{notificationId}/read")
  public ResponseEntity<ApiResponse<NotificationMarkReadResponse>> markAsRead(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @PathVariable("notificationId") String notificationId) {

    try {
      String userId = validateAndGetUserId(authHeader);
      Notification notification = notificationService.markAsRead(notificationId, userId);
      NotificationMarkReadResponse response = NotificationMarkReadResponse.builder()
          .notificationId(notification.getId())
          .isRead(Boolean.TRUE.equals(notification.getIsRead()))
          .readAt(notification.getReadAt() != null
              ? notification.getReadAt().format(DateTimeFormatter.ISO_DATE_TIME)
              : null)
          .build();
      return ResponseEntity.ok(ApiResponse.success(response, "알림을 읽음 처리했습니다"));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PutMapping("/read-all")
  public ResponseEntity<ApiResponse<NotificationReadAllResponse>> markAllAsRead(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    try {
      String userId = validateAndGetUserId(authHeader);
      int readCount = notificationService.markAllAsRead(userId);
      NotificationReadAllResponse response = NotificationReadAllResponse.builder()
          .readCount(readCount)
          .build();
      return ResponseEntity.ok(ApiResponse.success(response, readCount + "개의 알림을 읽음 처리했습니다"));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @GetMapping("/settings")
  public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getSettings(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    try {
      String userId = validateAndGetUserId(authHeader);
      NotificationSettings settings = notificationService.getNotificationSettings(userId);
      return ResponseEntity.ok(ApiResponse.success(NotificationSettingsResponse.from(settings)));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  @PutMapping("/settings")
  public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateSettings(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody UpdateNotificationSettingsRequest request) {
    try {
      String userId = validateAndGetUserId(authHeader);
      NotificationSettings settings = notificationService.updateNotificationSettings(userId, request);
      return ResponseEntity.ok(ApiResponse.success(NotificationSettingsResponse.from(settings), "알림 설정이 저장되었습니다"));
    } catch (RuntimeException e) {
      return handleError(e);
    }
  }

  private String validateAndGetUserId(String authHeader) {
    return authHeaderResolver.resolveAccessUserId(authHeader);
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
    if (message.startsWith("NOTIFICATION_003")) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("서버 오류가 발생했습니다"));
  }
}
