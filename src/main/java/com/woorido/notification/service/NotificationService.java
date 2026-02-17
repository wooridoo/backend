package com.woorido.notification.service;

import com.woorido.notification.domain.Notification;
import com.woorido.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
   * 사용자 미읽음 알림 개수 조회.
   */
  @Transactional(readOnly = true)
  public int getUnreadCount(String userId) {
    return notificationMapper.countUnreadByUserId(userId);
  }
}
