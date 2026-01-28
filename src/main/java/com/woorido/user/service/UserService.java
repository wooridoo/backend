package com.woorido.user.service;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.common.util.JwtUtil;
import com.woorido.user.dto.request.UserUpdateRequest;
import com.woorido.user.dto.response.NicknameCheckResponse;
import com.woorido.user.dto.response.UserProfileResponse;
import com.woorido.user.dto.response.UserUpdateResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

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

        System.out.println("========== GET MY PROFILE ==========");
        System.out.println("userId: " + userId);
        System.out.println("=====================================");

        // 3. 사용자 정보 조회
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        // 4. 응답 생성
        return UserProfileResponse.builder()
                .userId(user.getId() != null ? Long.parseLong(user.getId().replaceAll("[^0-9]", "").substring(0,
                        Math.min(10, user.getId().replaceAll("[^0-9]", "").length()))) : 1L)
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate() != null ? user.getBirthDate().format(DATE_FORMATTER) : null)
                .profileImage(user.getProfileImageUrl())
                .status(user.getAccountStatus())
                .brix(85.5) // TODO: 실제 brix 계산 로직 필요
                .account(UserProfileResponse.AccountInfo.builder()
                        .accountId(1L) // TODO: 실제 계정 정보 연동 필요
                        .balance(500000L)
                        .availableBalance(450000L)
                        .lockedBalance(50000L)
                        .build())
                .stats(UserProfileResponse.StatsInfo.builder()
                        .challengeCount(3) // TODO: 실제 통계 정보 연동 필요
                        .completedChallenges(2)
                        .totalSupportAmount(1500000L)
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

        System.out.println("========== UPDATE MY PROFILE ==========");
        System.out.println("userId: " + userId);
        System.out.println("nickname: " + request.getNickname());
        System.out.println("phone: " + request.getPhone());
        System.out.println("========================================");

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
                .userId(updatedUser.getId() != null
                        ? Long.parseLong(updatedUser.getId().replaceAll("[^0-9]", "").substring(0,
                                Math.min(10, updatedUser.getId().replaceAll("[^0-9]", "").length())))
                        : 1L)
                .nickname(updatedUser.getNickname())
                .phone(updatedUser.getPhone())
                .profileImage(updatedUser.getProfileImageUrl())
                .updatedAt(java.time.LocalDateTime.now().format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 닉네임 중복 체크
     * - 닉네임 길이 검증 (2-20자)
     * - DB에서 중복 확인
     */
    public NicknameCheckResponse checkNicknameAvailability(String nickname) {
        System.out.println("========== CHECK NICKNAME ==========");
        System.out.println("nickname: " + nickname);
        System.out.println("=====================================");

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
