package com.woorido.notification.mapper;

import com.woorido.notification.domain.Notification;
import com.woorido.notification.domain.NotificationSettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface NotificationMapper {
  void save(Notification notification);

  List<Notification> findAllByUserId(@Param("userId") String userId);

  Optional<Notification> findById(@Param("id") String id);

  void updateIsRead(@Param("id") String id);

  int countUnreadByUserId(@Param("userId") String userId);

  int markAllAsReadByUserId(@Param("userId") String userId);

  NotificationSettings findSettingsByUserId(@Param("userId") String userId);

  int insertSettings(NotificationSettings settings);

  int updateSettings(NotificationSettings settings);
}
