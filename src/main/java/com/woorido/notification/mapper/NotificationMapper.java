package com.woorido.notification.mapper;

import com.woorido.notification.domain.Notification;
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
}
