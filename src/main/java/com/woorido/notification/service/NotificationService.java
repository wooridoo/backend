package com.woorido.notification.service;

import com.woorido.notification.domain.Notification;
import com.woorido.notification.domain.NotificationSettings;
import com.woorido.notification.domain.NotificationType;
import com.woorido.notification.dto.request.UpdateNotificationSettingsRequest;
import com.woorido.notification.mapper.NotificationMapper;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
  // 학습 포인트:
  // - 알림은 "내 것만 읽음 처리 가능" 규칙을 서비스 레이어에서 강제한다.

  private final NotificationMapper notificationMapper;
  private static final long SOCIAL_AGGREGATE_WINDOW_MILLIS = 30_000L;
  private static final String TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";
  private static final Object SOCIAL_AGGREGATION_LOCK = new Object();
  private final ConcurrentHashMap<String, SocialAggregationState> socialAggregationStateByKey = new ConcurrentHashMap<>();

  /**
   * 사용자 알림 목록 조회.
   */
  @Transactional(readOnly = true)
  public List<Notification> getNotifications(String userId, int page, int size, String type, Boolean isRead) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.max(size, 1);
    int offset = safePage * safeSize;
    List<String> types = resolveTypes(type);
    return notificationMapper.findByUserIdWithFilters(userId, types, isRead, offset, safeSize);
  }

  @Transactional(readOnly = true)
  public long countNotifications(String userId, String type, Boolean isRead) {
    List<String> types = resolveTypes(type);
    return notificationMapper.countByUserIdWithFilters(userId, types, isRead);
  }

  /**
   * 알림 상세 조회.
   */
  @Transactional(readOnly = true)
  public Notification getNotification(String notificationId, String userId) {
    Notification notification = notificationMapper.findById(notificationId)
        .orElseThrow(() -> new RuntimeException("NOTIFICATION_001:알림을 찾을 수 없습니다"));
    if (!notification.getUserId().equals(userId)) {
      throw new RuntimeException("NOTIFICATION_002:알림 접근 권한이 없습니다");
    }
    return notification;
  }

  /**
   * 알림 읽음 처리.
   * 본인 알림인지 검증한 뒤 읽음 상태를 갱신한다.
   */
  @Transactional
  public Notification markAsRead(String notificationId, String userId) {
    // 1) 알림 존재 검증
    Notification notification = notificationMapper.findById(notificationId)
        .orElseThrow(() -> new RuntimeException("NOTIFICATION_001:알림을 찾을 수 없습니다"));

    // 2) 소유자 검증(다른 사용자의 알림 접근 차단)
    if (!notification.getUserId().equals(userId)) {
      throw new RuntimeException("NOTIFICATION_002:알림 접근 권한이 없습니다");
    }

    // 3) 읽음 처리
    notificationMapper.updateIsRead(notificationId);
    return notificationMapper.findById(notificationId)
        .orElseThrow(() -> new RuntimeException("NOTIFICATION_001:알림을 찾을 수 없습니다"));
  }

  /**
   * 사용자 전체 알림 읽음 처리.
   */
  @Transactional
  public int markAllAsRead(String userId) {
    return notificationMapper.updateAllAsReadByUserId(userId);
  }

  /**
   * 사용자 미읽음 알림 개수 조회.
   */
  @Transactional(readOnly = true)
  public int getUnreadCount(String userId) {
    return notificationMapper.countUnreadByUserId(userId);
  }

  /**
   * 소셜 알림 생성.
   * 동일 key(type + targetId + recipient) 30초 내 중복은 1건으로 집계한다.
   */
  @Transactional
  public void publishSocialNotification(
      NotificationType type,
      String recipientUserId,
      String actorUserId,
      String targetId,
      String title,
      String singleMessage,
      String aggregateMessage,
      String linkUrl,
      String relatedEntityType,
      String relatedEntityId) {

    if (recipientUserId == null || recipientUserId.isBlank()) {
      return;
    }
    if (actorUserId != null && actorUserId.equals(recipientUserId)) {
      return;
    }
    if (type == null) {
      return;
    }

    NotificationSettings settings = getNotificationSettingsOrDefault(recipientUserId);
    if (!Boolean.TRUE.equals(settings.getSnsNotification())) {
      return;
    }

    // 현재는 푸시 채널을 별도 운영하지 않는다.
    // quiet hours 에서는 push 를 억제하고 DB 기록은 유지하는 정책을 맞추기 위해
    // suppress 여부만 계산해둔다.
    boolean suppressPush = isQuietHours(settings);
    if (suppressPush) {
      // no-op: DB 저장은 계속 진행한다.
    }

    String dedupeTarget = targetId == null ? "-" : targetId;
    String dedupeKey = type.name() + "|" + dedupeTarget + "|" + recipientUserId;
    long now = System.currentTimeMillis();

    synchronized (SOCIAL_AGGREGATION_LOCK) {
      if (socialAggregationStateByKey.size() > 10_000) {
        socialAggregationStateByKey.entrySet()
            .removeIf(entry -> now - entry.getValue().lastUpdatedAtMillis > SOCIAL_AGGREGATE_WINDOW_MILLIS);
      }

      SocialAggregationState state = socialAggregationStateByKey.get(dedupeKey);
      if (state != null && (now - state.lastUpdatedAtMillis) <= SOCIAL_AGGREGATE_WINDOW_MILLIS) {
        state.count += 1;
        state.lastUpdatedAtMillis = now;
        String mergedContent = buildAggregatedMessage(singleMessage, aggregateMessage, state.count);
        notificationMapper.updateNotificationContent(
            state.notificationId,
            title,
            mergedContent,
            linkUrl,
            relatedEntityType,
            relatedEntityId);
        return;
      }

      Notification notification = Notification.builder()
          .id(UUID.randomUUID().toString())
          .userId(recipientUserId)
          .type(type)
          .title(title)
          .content(singleMessage)
          .linkUrl(linkUrl)
          .relatedEntityType(relatedEntityType)
          .relatedEntityId(relatedEntityId)
          .isRead(false)
          .build();
      notificationMapper.save(notification);
      socialAggregationStateByKey.put(
          dedupeKey,
          new SocialAggregationState(notification.getId(), 1, now));
    }
  }

  /**
   * 알림 설정 조회.
   */
  @Transactional(readOnly = true)
  public NotificationSettings getNotificationSettings(String userId) {
    return getNotificationSettingsOrDefault(userId);
  }

  /**
   * 알림 설정 수정.
   */
  @Transactional
  public NotificationSettings updateNotificationSettings(String userId, UpdateNotificationSettingsRequest request) {
    NotificationSettings current = notificationMapper.findSettingsByUserId(userId)
        .orElseGet(() -> defaultSettings(userId));

    String quietHoursStart = request.getQuietHoursStart() != null
        ? normalizeQuietHoursTime(request.getQuietHoursStart())
        : current.getQuietHoursStart();
    String quietHoursEnd = request.getQuietHoursEnd() != null
        ? normalizeQuietHoursTime(request.getQuietHoursEnd())
        : current.getQuietHoursEnd();

    NotificationSettings updated = current.toBuilder()
        .id(current.getId() != null ? current.getId() : UUID.randomUUID().toString())
        .userId(userId)
        .pushEnabled(coalesce(request.getPushEnabled(), current.getPushEnabled()))
        .emailEnabled(coalesce(request.getEmailEnabled(), current.getEmailEnabled()))
        .smsEnabled(coalesce(request.getSmsEnabled(), current.getSmsEnabled()))
        .voteNotification(coalesce(request.getVoteNotification(), current.getVoteNotification()))
        .meetingNotification(coalesce(request.getMeetingNotification(), current.getMeetingNotification()))
        .expenseNotification(coalesce(request.getExpenseNotification(), current.getExpenseNotification()))
        .snsNotification(coalesce(request.getSnsNotification(), current.getSnsNotification()))
        .systemNotification(coalesce(request.getSystemNotification(), current.getSystemNotification()))
        .quietHoursEnabled(coalesce(request.getQuietHoursEnabled(), current.getQuietHoursEnabled()))
        .quietHoursStart(quietHoursStart)
        .quietHoursEnd(quietHoursEnd)
        .build();

    validateQuietHours(updated);

    int updatedCount = notificationMapper.updateSettings(updated);
    if (updatedCount == 0) {
      notificationMapper.insertSettings(updated);
    }

    return notificationMapper.findSettingsByUserId(userId).orElse(updated);
  }

  private List<String> resolveTypes(String type) {
    if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) {
      return null;
    }

    String normalized = type.trim().toUpperCase(Locale.ROOT);
    List<String> types = Arrays.stream(NotificationType.values())
        .filter(notificationType -> notificationType.getCategory().equals(normalized))
        .map(Enum::name)
        .collect(Collectors.toList());

    if (types.isEmpty()) {
      throw new RuntimeException("NOTIFICATION_003:지원하지 않는 알림 유형입니다");
    }
    return types;
  }

  private NotificationSettings getNotificationSettingsOrDefault(String userId) {
    return notificationMapper.findSettingsByUserId(userId)
        .orElseGet(() -> defaultSettings(userId));
  }

  private NotificationSettings defaultSettings(String userId) {
    return NotificationSettings.builder()
        .id(UUID.randomUUID().toString())
        .userId(userId)
        .pushEnabled(true)
        .emailEnabled(false)
        .smsEnabled(false)
        .voteNotification(true)
        .meetingNotification(true)
        .expenseNotification(true)
        .snsNotification(true)
        .systemNotification(true)
        .quietHoursEnabled(false)
        .quietHoursStart(null)
        .quietHoursEnd(null)
        .build();
  }

  private String buildAggregatedMessage(String singleMessage, String aggregateMessage, int count) {
    if (count <= 1) {
      return singleMessage;
    }
    String base = (aggregateMessage == null || aggregateMessage.isBlank()) ? singleMessage : aggregateMessage;
    return base + " (" + count + "건)";
  }

  private boolean coalesce(Boolean requested, Boolean fallback) {
    return requested != null ? requested : Boolean.TRUE.equals(fallback);
  }

  private String normalizeQuietHoursTime(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed;
  }

  private void validateQuietHours(NotificationSettings settings) {
    if (!Boolean.TRUE.equals(settings.getQuietHoursEnabled())) {
      return;
    }

    String start = settings.getQuietHoursStart();
    String end = settings.getQuietHoursEnd();
    if (start == null || end == null || !start.matches(TIME_PATTERN) || !end.matches(TIME_PATTERN)) {
      throw new RuntimeException("NOTIFICATION_003:quiet hours 형식이 올바르지 않습니다(HH:mm)");
    }
    if (Objects.equals(start, end)) {
      throw new RuntimeException("NOTIFICATION_003:quiet hours 시작/종료 시간이 동일할 수 없습니다");
    }
  }

  private boolean isQuietHours(NotificationSettings settings) {
    if (!Boolean.TRUE.equals(settings.getQuietHoursEnabled())) {
      return false;
    }
    String start = settings.getQuietHoursStart();
    String end = settings.getQuietHoursEnd();
    if (start == null || end == null || !start.matches(TIME_PATTERN) || !end.matches(TIME_PATTERN)) {
      return false;
    }

    LocalTime now = LocalTime.now();
    LocalTime startTime = LocalTime.parse(start);
    LocalTime endTime = LocalTime.parse(end);

    if (startTime.isBefore(endTime)) {
      return !now.isBefore(startTime) && now.isBefore(endTime);
    }
    return !now.isBefore(startTime) || now.isBefore(endTime);
  }

  private static class SocialAggregationState {
    private final String notificationId;
    private int count;
    private long lastUpdatedAtMillis;

    private SocialAggregationState(String notificationId, int count, long lastUpdatedAtMillis) {
      this.notificationId = notificationId;
      this.count = count;
      this.lastUpdatedAtMillis = lastUpdatedAtMillis;
    }
  }
}
