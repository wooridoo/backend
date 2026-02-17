package com.woorido.user.service;

import java.time.format.DateTimeFormatter;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.woorido.account.domain.Account;
import com.woorido.account.repository.AccountMapper;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.common.util.JwtUtil;
import com.woorido.user.dto.request.UserUpdateRequest;
import com.woorido.user.dto.response.NicknameCheckResponse;
import com.woorido.user.dto.response.UserProfileResponse;
import com.woorido.user.dto.response.UserUpdateResponse;
import com.woorido.user.dto.response.UserWithdrawResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    // 학습 포인트:
    // - 프로필 조회/수정은 "토큰 검증 -> 사용자 조회 -> 응답 조립" 공통 구조를 따른다.

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * 내 정보 조회
     * - Access Token에서 사용자 ID 추출
     * - 사용자 정보 조회 및 반환
     */
    public UserProfileResponse getMyProfile(String accessToken) {
        // 1. 토큰 유효성 검증
        if (!jwtUtil.validateToken(accessToken)) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        // 2. 토큰에서 사용자 ID 추출
        String userId = jwtUtil.getUserIdFromToken(accessToken);

        // 3. 사용자 정보 조회
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        // 4. 실제 계좌 정보 조회
        Account account = accountMapper.findByUserId(userId);
        UserProfileResponse.AccountInfo accountInfo;
        if (account != null) {
            long totalBalance = account.getBalance();
            long locked = account.getLockedBalance();
            long available = totalBalance - locked;
            accountInfo = UserProfileResponse.AccountInfo.builder()
                    .accountId(account.getId())
                    .balance(totalBalance)
                    .availableBalance(available)
                    .lockedBalance(locked)
                    .build();
        } else {
            accountInfo = UserProfileResponse.AccountInfo.builder()
                    .accountId(null)
                    .balance(0L)
                    .availableBalance(0L)
                    .lockedBalance(0L)
                    .build();
        }

        // 5. 실제 통계 정보 조회
        int challengeCount = userMapper.countChallengesByUserId(userId);
        int completedChallenges = userMapper.countCompletedChallengesByUserId(userId);
        long totalSupportAmount = userMapper.sumTotalSupportAmountByUserId(userId);

        // 6. 응답 생성
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate() != null ? user.getBirthDate().format(DATE_FORMATTER) : null)
                .profileImage(user.getProfileImageUrl())
                .status(user.getAccountStatus().name())
                // TODO: 실제 브릭스 계산 로직이 준비되면 하드코딩 값을 교체한다.
                .brix(12.0) // 기본값 12
                .account(accountInfo)
                .stats(UserProfileResponse.StatsInfo.builder()
                        .challengeCount(challengeCount)
                        .completedChallenges(completedChallenges)
                        .totalSupportAmount(totalSupportAmount)
                        .build())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DATETIME_FORMATTER) : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().format(DATETIME_FORMATTER) : null)
                .build();
    }

    /**
     * 내 정보 수정
     * - Access Token에서 사용자 ID 추출
     * - 닉네임 중복 체크
     * - 사용자 정보 업데이트
     */
    public UserUpdateResponse updateMyProfile(String accessToken, UserUpdateRequest request) {
        // 1. 토큰 유효성 검증
        if (!jwtUtil.validateToken(accessToken)) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        // 2. 토큰에서 사용자 ID 추출
        String userId = jwtUtil.getUserIdFromToken(accessToken);

        // 3. 사용자 존재 확인
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        // 4. 닉네임 중복 체크 (변경 시에만)
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            int count = userMapper.countByNicknameExcludingUser(request.getNickname(), userId);
            if (count > 0) {
                throw new RuntimeException("USER_007:이미 사용 중인 닉네임입니다");
            }
        }

        // 5. 프로필 업데이트
        userMapper.updateUserProfile(userId, request.getNickname(), request.getPhone(), request.getProfileImage());

        // 6. 업데이트된 사용자 정보 조회
        User updatedUser = userMapper.findById(userId);

        // 7. 응답 생성
        return UserUpdateResponse.builder()
                .userId(updatedUser.getId())
                .nickname(updatedUser.getNickname())
                .phone(updatedUser.getPhone())
                .profileImage(updatedUser.getProfileImageUrl())
                .updatedAt(java.time.LocalDateTime.now().format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 회원 탈퇴 (API 012)
     */
    public UserWithdrawResponse withdrawUser(String accessToken,
            com.woorido.user.dto.request.UserWithdrawRequest request) {
        // 1. 토큰 유효성 검증
        if (!jwtUtil.validateToken(accessToken)) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        User user = userMapper.findById(userId);

        // 2. 비밀번호 확인
        if (request.getPassword() != null) {
            if (user.getPasswordHash() != null
                    && !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new RuntimeException("USER_003:비밀번호가 일치하지 않습니다");
            }
        }

        // 3. 사용자 상태 업데이트 (Soft Delete)
        userMapper.updateAccountStatus(userId, "WITHDRAWN");

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return UserWithdrawResponse.builder()
                .userId(user.getId())
                .status("WITHDRAWN")
                .withdrawnAt(now.format(DATETIME_FORMATTER))
                .dataDeletedAt(now.plusDays(30).format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 닉네임 중복 체크
     * - 닉네임 길이 검증 (2-20자)
     * - DB에서 중복 확인
     */
    public NicknameCheckResponse checkNicknameAvailability(String nickname) {

        // 1. 닉네임 길이 검증
        if (nickname == null || nickname.length() < 2 || nickname.length() > 20) {
            throw new RuntimeException("USER_006:닉네임은 2-20자여야 합니다");
        }

        // 2. 중복 체크
        int count = userMapper.countByNickname(nickname);

        // 3. 응답 생성
        if (count > 0) {
            return NicknameCheckResponse.unavailable(nickname);
        }
        return NicknameCheckResponse.available(nickname);
    }
}
