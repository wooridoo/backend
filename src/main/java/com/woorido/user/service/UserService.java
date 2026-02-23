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
import com.woorido.user.dto.request.SocialOnboardingRequest;
import com.woorido.user.dto.request.UserPasswordChangeRequest;
import com.woorido.user.dto.response.NicknameCheckResponse;
import com.woorido.user.dto.response.SocialOnboardingCompleteResponse;
import com.woorido.user.dto.response.UserPasswordChangeResponse;
import com.woorido.user.dto.response.UserProfileResponse;
import com.woorido.user.dto.response.UserPublicProfileResponse;
import com.woorido.user.dto.response.UserUpdateResponse;
import com.woorido.user.dto.response.UserWithdrawResponse;

import lombok.RequiredArgsConstructor;
import java.util.Collections;

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
        Double brixScore = userMapper.findTotalScoreByUserId(userId);

        // 6. 응답 생성
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate() != null ? user.getBirthDate().format(DATE_FORMATTER) : null)
                .profileImage(user.getProfileImageUrl())
                .status(user.getAccountStatus().name())
                .hasPassword(user.getPasswordHash() != null && !user.getPasswordHash().isBlank())
                .requiresOnboarding(isSocialOnboardingRequired(user))
                .brix(brixScore != null ? brixScore : 12.0)
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
     * 소셜 신규가입 사용자의 필수 온보딩 정보를 저장합니다.
     */
    public SocialOnboardingCompleteResponse completeSocialOnboarding(String accessToken, SocialOnboardingRequest request) {
        if (!jwtUtil.validateToken(accessToken)) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        String userId = jwtUtil.getUserIdFromToken(accessToken);
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }
        if (user.getSocialProvider() == null) {
            throw new RuntimeException("AUTH_013:지원하지 않는 소셜 인증 요청입니다");
        }
        if (request.getNickname() == null || request.getNickname().isBlank()) {
            throw new RuntimeException("USER_006:닉네임을 입력해 주세요");
        }
        String normalizedNickname = request.getNickname().trim();
        if (normalizedNickname.length() < 2 || normalizedNickname.length() > 20) {
            throw new RuntimeException("USER_006:닉네임은 2-20자여야 합니다");
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new RuntimeException("VALIDATION_001:전화번호를 입력해 주세요");
        }
        String normalizedPhone = request.getPhone().trim();
        if (!normalizedPhone.matches("^010-\\d{4}-\\d{4}$")) {
            throw new RuntimeException("VALIDATION_001:전화번호 형식이 올바르지 않습니다");
        }
        if (!request.isTermsAgreed() || !request.isPrivacyAgreed()) {
            throw new RuntimeException("VALIDATION_001:필수 약관 동의가 필요합니다");
        }

        if (!normalizedNickname.equals(user.getNickname())) {
            int count = userMapper.countByNicknameExcludingUser(normalizedNickname, userId);
            if (count > 0) {
                throw new RuntimeException("USER_007:이미 사용 중인 닉네임입니다");
            }
        }

        userMapper.completeSocialOnboarding(
                userId,
                normalizedNickname,
                normalizedPhone,
                request.isMarketingAgreed() ? "Y" : "N");

        UserProfileResponse profile = getMyProfile(accessToken);
        return SocialOnboardingCompleteResponse.builder()
                .completed(true)
                .user(profile)
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
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        if (hasPassword) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new RuntimeException("USER_009:비밀번호를 입력해주세요");
            }
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
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

    /**
     * 비밀번호 변경.
     */
    public UserPasswordChangeResponse changePassword(String accessToken, UserPasswordChangeRequest request) {
        if (!jwtUtil.validateToken(accessToken)) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        String userId = jwtUtil.getUserIdFromToken(accessToken);
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("USER_003:현재 비밀번호가 일치하지 않습니다");
        }

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new RuntimeException("VALIDATION_001:새 비밀번호 확인이 일치하지 않습니다");
        }

        if (!isValidPassword(request.getNewPassword())) {
            throw new RuntimeException("VALIDATION_001:비밀번호 형식이 올바르지 않습니다");
        }

        userMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));

        return UserPasswordChangeResponse.builder()
                .passwordChanged(true)
                .build();
    }

    /**
     * 사용자 공개 프로필 조회.
     */
    public UserPublicProfileResponse getUserProfile(String accessToken, String targetUserId) {
        if (!jwtUtil.validateToken(accessToken)) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        User user = userMapper.findById(targetUserId);
        if (user == null) {
            throw new RuntimeException("USER_001:사용자를 찾을 수 없습니다");
        }

        int completedChallenges = userMapper.countCompletedChallengesByUserId(targetUserId);
        Double brixScore = userMapper.findTotalScoreByUserId(targetUserId);
        return UserPublicProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImageUrl())
                .brix(brixScore != null ? brixScore : 12.0)
                .stats(UserPublicProfileResponse.Stats.builder()
                        .completedChallenges(completedChallenges)
                        .totalMeetings(0)
                        .build())
                .commonChallenges(Collections.emptyList())
                .isVerified("Y".equalsIgnoreCase(user.getIsVerified()))
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DATETIME_FORMATTER) : null)
                .build();
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 20) {
            return false;
        }

        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        return hasLetter && hasDigit && hasSpecial;
    }

    private boolean isSocialOnboardingRequired(User user) {
        if (user.getSocialProvider() == null) {
            return false;
        }

        boolean agreedTerms = "Y".equalsIgnoreCase(user.getAgreedTerms());
        boolean agreedPrivacy = "Y".equalsIgnoreCase(user.getAgreedPrivacy());
        return !agreedTerms || !agreedPrivacy;
    }
}
