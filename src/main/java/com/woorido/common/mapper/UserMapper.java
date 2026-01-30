package com.woorido.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.common.entity.User;

@Mapper
public interface UserMapper {

        // ID로 사용자 조회
        User findById(@Param("id") String id);

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

        // 닉네임 중복 체크 (본인 제외)
        int countByNicknameExcludingUser(@Param("nickname") String nickname, @Param("userId") String userId);

        // 사용자 프로필 업데이트
        void updateUserProfile(
                        @Param("id") String id,
                        @Param("nickname") String nickname,
                        @Param("phone") String phone,
                        @Param("profileImage") String profileImage);

        // 닉네임 중복 체크 (전체)
        int countByNickname(@Param("nickname") String nickname);
}
