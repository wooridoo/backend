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

  private final NotificationMapper notificationMapper;

  @Transactional(readOnly = true)
  public List<Notification> getNotifications(String userId) {
    return notificationMapper.findAllByUserId(userId);
  }

  @Transactional
  public void markAsRead(String notificationId, String userId) {
    Notification notification = notificationMapper.findById(notificationId)
        .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

    if (!notification.getUserId().equals(userId)) {
      throw new IllegalArgumentException("Unauthorized access to notification");
    }

    notificationMapper.updateIsRead(notificationId);
  }

  @Transactional(readOnly = true)
  public int getUnreadCount(String userId) {
    return notificationMapper.countUnreadByUserId(userId);
  }
}
