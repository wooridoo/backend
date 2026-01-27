package com.woorido.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.entity.User;

@Mapper
public interface UserMapper {

  // 로그인용 조회
  User findByEmail(@Param("email") String email);

  // 이메일 중복 체크 (회원가입용)
  int countByEmail(@Param("email") String email);

  // 회원가입 - 사용자 등록
  void insertUser(User user);

  // 로그인 시간 업데이트
  void updateLastLoginAt(@Param("id") String id);

  // 로그인 실패 횟수 증가
  void incrementFailedLoginAttempts(@Param("id") String id);

  // 로그인 실패 횟수 초기화
  void resetFailedLoginAttempts(@Param("id") String id);

  // 계정 잠금
  void lockAccount(@Param("id") String id, @Param("lockedUntil") java.time.LocalDateTime lockedUntil);
}
