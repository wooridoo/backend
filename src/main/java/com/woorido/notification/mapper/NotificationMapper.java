package com.woorido.notification.mapper;

import com.woorido.notification.domain.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface NotificationMapper {
  void save(Notification notification);

  List<Notification> findByUserIdWithFilters(
      @Param("userId") String userId,
      @Param("types") List<String> types,
      @Param("isRead") Boolean isRead,
      @Param("offset") int offset,
      @Param("limit") int limit);

  long countByUserIdWithFilters(
      @Param("userId") String userId,
      @Param("types") List<String> types,
      @Param("isRead") Boolean isRead);

  Optional<Notification> findById(@Param("id") String id);

  void updateIsRead(@Param("id") String id);

  int updateAllAsReadByUserId(@Param("userId") String userId);

  int countUnreadByUserId(@Param("userId") String userId);
}
