package com.woorido.common.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.common.entity.User;

@Mapper
public interface UserMapper {

        // ID로 사용자 조회
        User findById(@Param("id") String id);

        // 로그인용 조회
        User findByEmail(@Param("email") String email);

        // 소셜 로그인용 조회
        User findBySocial(@Param("socialProvider") String socialProvider, @Param("socialId") String socialId);

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

        // 비밀번호 재설정 토큰 저장
        void updatePasswordResetToken(
                        @Param("id") String id,
                        @Param("resetToken") String resetToken,
                        @Param("expiresAt") java.time.LocalDateTime expiresAt);

        // 닉네임 중복 체크 (본인 제외)
        int countByNicknameExcludingUser(@Param("nickname") String nickname, @Param("userId") String userId);

        // 사용자 프로필 업데이트
        void updateUserProfile(
                        @Param("id") String id,
                        @Param("nickname") String nickname,
                        @Param("phone") String phone,
                        @Param("profileImage") String profileImage);

        // 소셜 온보딩 완료 처리
        void completeSocialOnboarding(
                        @Param("id") String id,
                        @Param("nickname") String nickname,
                        @Param("phone") String phone,
                        @Param("agreedMarketing") String agreedMarketing);

        // 닉네임 중복 체크 (전체)
        int countByNickname(@Param("nickname") String nickname);

        // 사용자 상태 업데이트
        void updateAccountStatus(@Param("id") String id, @Param("status") String status);

        // 기존 이메일 계정에 소셜 계정 연결
        void linkSocialAccount(@Param("id") String id, @Param("socialProvider") String socialProvider,
                        @Param("socialId") String socialId);

        // 비밀번호 재설정 토큰으로 사용자 조회
        User findByPasswordResetToken(@Param("token") String token);

        // 비밀번호 업데이트
        void updatePassword(@Param("id") String id, @Param("password") String password);

        // 비밀번호 재설정 토큰 초기화
        void clearPasswordResetToken(@Param("id") String id);

        // 사용자가 참여 중인 챌린지 수
        int countChallengesByUserId(@Param("userId") String userId);

        // 사용자가 완료한 챌린지 수
        int countCompletedChallengesByUserId(@Param("userId") String userId);

        // 사용자의 총 서포트 금액
        long sumTotalSupportAmountByUserId(@Param("userId") String userId);

        // 사용자 당도(브릭스) 조회
        Double findTotalScoreByUserId(@Param("userId") String userId);

        // 회원가입 시 초기 당도 레코드 생성
        int insertInitialUserScore(@Param("id") String id, @Param("userId") String userId,
                        @Param("totalScore") Double totalScore);

        // 당도 점수 업서트 (월 배치 반영)
        int upsertTotalScoreByUserId(@Param("id") String id, @Param("userId") String userId,
                        @Param("totalScore") Double totalScore, @Param("calculatedAt") LocalDateTime calculatedAt,
                        @Param("calculatedMonth") String calculatedMonth);
}
