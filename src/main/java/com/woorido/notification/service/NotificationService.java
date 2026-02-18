package com.woorido.notification.service;

import com.woorido.notification.domain.Notification;
import com.woorido.notification.domain.NotificationSettings;
import com.woorido.notification.dto.UpdateNotificationSettingsRequest;
import com.woorido.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
  // 학습 포인트:
  // - 알림은 "내 것만 읽음 처리 가능" 규칙을 서비스 레이어에서 강제한다.

  private final NotificationMapper notificationMapper;

  /**
   * 사용자 알림 목록 조회.
   */
  @Transactional(readOnly = true)
  public List<Notification> getNotifications(String userId) {
    return notificationMapper.findAllByUserId(userId);
  }

  /**
   * 알림 읽음 처리.
   * 본인 알림인지 검증한 뒤 읽음 상태를 갱신한다.
   */
  @Transactional
  public void markAsRead(String notificationId, String userId) {
    // 1) 알림 존재 검증
    Notification notification = notificationMapper.findById(notificationId)
        .orElseThrow(() -> new RuntimeException("NOTIFICATION_001:알림을 찾을 수 없습니다"));

    // 2) 소유자 검증(다른 사용자의 알림 접근 차단)
    if (!notification.getUserId().equals(userId)) {
      throw new RuntimeException("NOTIFICATION_002:알림 접근 권한이 없습니다");
    }

    // 3) 읽음 처리
    notificationMapper.updateIsRead(notificationId);
  }

  /**
   * ?뚮┝ ?⑴굅 議고쉶.
   */
  @Transactional(readOnly = true)
  public Notification getNotification(String notificationId, String userId) {
    Notification notification = notificationMapper.findById(notificationId)
        .orElseThrow(() -> new RuntimeException("NOTIFICATION_001:?뚮┝??李얠쓣 ???놁뒿?덈떎"));

    if (!notification.getUserId().equals(userId)) {
      throw new RuntimeException("NOTIFICATION_002:?뚮┝ ?묎렐 沅뚰븳???놁뒿?덈떎");
    }
    return notification;
  }

  /**
   * ?뚮┝ ?꾩껜 ?쎌쓬 泥섎━.
   */
  @Transactional
  public void markAllAsRead(String userId) {
    notificationMapper.markAllAsReadByUserId(userId);
  }

  /**
   * ?뚮┝ ?ㅼ젙 議고쉶.
   */
  @Transactional(readOnly = true)
  public NotificationSettings getSettings(String userId) {
    return getOrCreateSettings(userId);
  }

  /**
   * ?뚮┝ ?ㅼ젙 ?섏젙.
   */
  @Transactional
  public NotificationSettings updateSettings(String userId, UpdateNotificationSettingsRequest request) {
    NotificationSettings current = getOrCreateSettings(userId);
    NotificationSettings update = NotificationSettings.builder()
        .id(current.getId())
        .userId(userId)
        .pushEnabled(request.getPushEnabled())
        .emailEnabled(request.getEmailEnabled())
        .smsEnabled(request.getSmsEnabled())
        .voteNotification(request.getVoteNotification())
        .meetingNotification(request.getMeetingNotification())
        .expenseNotification(request.getExpenseNotification())
        .snsNotification(request.getSnsNotification())
        .systemNotification(request.getSystemNotification())
        .quietHoursEnabled(request.getQuietHoursEnabled())
        .quietHoursStart(request.getQuietHoursStart())
        .quietHoursEnd(request.getQuietHoursEnd())
        .build();

    notificationMapper.updateSettings(update);
    return getOrCreateSettings(userId);
  }

  /**
   * 사용자 미읽음 알림 개수 조회.
   */
  @Transactional(readOnly = true)
  public int getUnreadCount(String userId) {
    return notificationMapper.countUnreadByUserId(userId);
  }

  private NotificationSettings getOrCreateSettings(String userId) {
    NotificationSettings settings = notificationMapper.findSettingsByUserId(userId);
    if (settings != null) {
      return settings;
    }

    NotificationSettings defaults = NotificationSettings.builder()
        .id(UUID.randomUUID().toString())
        .userId(userId)
        .build();
    notificationMapper.insertSettings(defaults);

    NotificationSettings created = notificationMapper.findSettingsByUserId(userId);
    if (created == null) {
      throw new RuntimeException("NOTIFICATION_003:?ㅼ젙 ?앹꽦???ㅽ뙣?덉뒿?덈떎");
    }
    return created;
  }
}
