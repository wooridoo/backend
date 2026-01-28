package com.woorido.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.entity.User;

@Mapper
public interface UserMapper {

    User findByEmail(@Param("email") String email);

    void updateLastLoginAt(@Param("id") String id);

    void incrementFailedLoginAttempts(@Param("id") String id);

    void resetFailedLoginAttempts(@Param("id") String id);

    void lockAccount(@Param("id") String id, @Param("lockedUntil") java.time.LocalDateTime lockedUntil);
}
