package com.woorido.user.service;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.common.util.JwtUtil;
import com.woorido.user.dto.request.ChangePasswordRequest;
import com.woorido.user.dto.request.UserUpdateRequest;
import com.woorido.user.dto.request.WithdrawRequest;
import com.woorido.user.dto.response.ChangePasswordResponse;
import com.woorido.user.dto.response.NicknameCheckResponse;
import com.woorido.user.dto.response.UserProfileResponse;
import com.woorido.user.dto.response.UserUpdateResponse;
import com.woorido.user.dto.response.WithdrawResponse;
import com.woorido.user.strategy.PasswordValidator;
import com.woorido.user.strategy.WithdrawalValidator;
import com.woorido.account.domain.Account;
import com.woorido.account.repository.AccountMapper;
import com.woorido.common.mapper.ChallengeMemberMapper;
import com.woorido.user.dto.response.UserPublicProfileResponse;
import com.woorido.common.mapper.UserScoreMapper;
import com.woorido.common.mapper.MeetingVoteRecordMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final WithdrawalValidator withdrawalValidator;
    private final UserScoreMapper userScoreMapper;
    private final MeetingVoteRecordMapper meetingVoteRecordMapper;
    private final ChallengeMemberMapper challengeMemberMapper;
    private final AccountMapper accountMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * UUID 문자열에서 숫자만 추출하여 Long으로 변환
     * (응답 DTO의 userId 필드용)
     */
    private Long extractUserIdAsLong(String uuidString) {
        String digits = uuidString.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 1L;
        }
        return Long.parseLong(digits.substring(0, Math.min(10, digits.length())));
    }

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

        // 4. 추가 정보 조회 (실제 DB 연동)
        java.math.BigDecimal brix = userScoreMapper.findBrixByUserId(userId);
        double brixValue = (brix != null) ? brix.doubleValue() : 12.0;

        Account account = accountMapper.findByUserId(userId);
        long balance = (account != null) ? account.getBalance() : 0L;
        long locked = (account != null) ? account.getLockedBalance() : 0L;
        long accountId = (account != null) ? Long.parseLong(account.getId().replaceAll("[^0-9]", "").substring(0,
                Math.min(10, account.getId().replaceAll("[^0-9]", "").length()))) : 0L;

        int activeChallengeCount = challengeMemberMapper.countActiveMembershipsByUserId(userId);
        int completedChallengeCount = challengeMemberMapper.countCompletedChallengesByUserId(userId);
        // int totalSupportAmount = challengeMemberMapper.sumTotalSupport(userId); //
        // MOCK: 아직 쿼리 없음, 일단 0

        // 5. 응답 생성
        return UserProfileResponse.builder()
                .userId(user.getId() != null ? Long.parseLong(user.getId().replaceAll("[^0-9]", "").substring(0,
                        Math.min(10, user.getId().replaceAll("[^0-9]", "").length()))) : 1L)
                .uuid(user.getId()) // UUID 추가
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate() != null ? user.getBirthDate().format(DATE_FORMATTER) : null)
                .profileImage(user.getProfileImageUrl())
                .status(user.getAccountStatus())
                .brix(brixValue)
                .account(UserProfileResponse.AccountInfo.builder()
                        .accountId(accountId)
                        .balance(balance)
                        .availableBalance(balance) // 가용 잔액과 잔액의 차이는 비즈니스 로직에 따라 다름 (일단 동일하게)
                        .lockedBalance(locked)
                        .build())
                .stats(UserProfileResponse.StatsInfo.builder()
                        .challengeCount(activeChallengeCount)
                        .completedChallenges(completedChallengeCount)
                        .totalSupportAmount(0L) // TODO: 추후 구현
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
     * 비밀번호 변경
     * - 현재 비밀번호 확인
     * - 새 비밀번호 유효성 검증
     * - 비밀번호 암호화 및 업데이트
     */
    public ChangePasswordResponse changePassword(String accessToken, ChangePasswordRequest request) {
        // 1. 토큰 유효성 검증
        if (!jwtUtil.validateToken(accessToken)) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        // 2. 토큰에서 사용자 ID 추출
        String userId = jwtUtil.getUserIdFromToken(accessToken);

        log.info("CHANGE PASSWORD 요청 - userId: {}", userId);

        // 3. 사용자 조회
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        // 4. 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("USER_003:현재 비밀번호가 일치하지 않습니다");
        }

        // 5. 새 비밀번호 유효성 검증 (형식 및 일치 여부)
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new RuntimeException("VALIDATION_001:현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다");
        }
        passwordValidator.validate(request.getNewPassword(), request.getNewPasswordConfirm());

        // 6. 비밀번호 암호화 및 업데이트
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(userId, encodedPassword);

        // 7. 응답 생성
        return ChangePasswordResponse.builder()
                .passwordChanged(true)
                .build();
    }

    /**
     * 회원 탈퇴
     * - 비밀번호 확인
     * - 탈퇴 가능 여부 확인 (챌린지/크레딧)
     * - Soft Delete 처리
     */
    public WithdrawResponse withdrawUser(String accessToken, WithdrawRequest request) {
        // 1. 토큰 유효성 검증
        if (!jwtUtil.validateToken(accessToken)) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        // 2. 토큰에서 사용자 ID 추출
        String userId = jwtUtil.getUserIdFromToken(accessToken);

        log.info("WITHDRAW USER 요청 - userId: {}", userId);

        // 3. 사용자 조회
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("AUTH_001:인증이 필요합니다");
        }

        // 4. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("USER_003:비밀번호가 일치하지 않습니다");
        }

        // 5. 탈퇴 가능 여부 검증 (Strategy Pattern)
        withdrawalValidator.validateCanWithdraw(userId);

        // 6. Soft Delete 처리 (WITHDRAWN 상태로 변경) + 탈퇴 사유 저장
        userMapper.softDeleteUser(userId, "WITHDRAWN", request.getReason());

        // 7. 응답 생성
        // 탈퇴 처리 시점 (현재)
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // 데이터 완전 삭제 예정일 (30일 후)
        java.time.LocalDateTime deleteAt = now.plusDays(30);

        return WithdrawResponse.builder()
                .userId(extractUserIdAsLong(user.getId()))
                .status("WITHDRAWN")
                .withdrawnAt(now.format(DATETIME_FORMATTER))
                .dataDeletedAt(deleteAt.format(DATETIME_FORMATTER))
                .build();
    }

    /**
     * 사용자 공개 프로필 조회 (API 013)
     */
    public UserPublicProfileResponse getUserPublicProfile(Long userIdLong, String currentUserId) {
        // Long userId -> String uuid (역매핑 불가능하므로 DB에서 Long으로 조회하거나, API 설계를 UUID로 변경하거나
        // 해야 함)
        // 현재 상황: API 013 path param이 Long userId임. 하지만 DB는 UUID를 사용.
        // 해결책 1: API를 UUID 받도록 변경 (권장)
        // 해결책 2: Long userId로 매핑되는 UUID를 찾는 별도 테이블이나 컬럼이 있어야 함 (없음)
        // * 중요: 현재 구현의 한계로 인해, API 013의 userId는 사실상 UUID의 해시값이거나 가상의 값임.
        // 사용자 편의를 위해 일단 "모든 사용자 조회"가 불가능하거나, 클라이언트가 UUID를 알아야 한다고 가정.
        // 하지만 요구사항은 Long userId임.
        // --> User 테이블에 sequence_id가 없으므로 정확한 매핑 불가.
        // --> 임시로: 1. 클라이언트가 UUID를 보낸다고 가정 (String userId) -> Controller에서 String으로 받기?
        // --> 아니면 2. 테스트용으로 1L -> 특정 UUID 매핑 하드코딩?
        // --> 정석: User 테이블에 auto_increment ID 컬럼 추가 필요.

        // * 여기서는 일단 UUID를 그대로 String으로 받는다고 가정하고 Controller를 String으로 수정할 것을 제안하거나,
        // * Controller에서 String으로 받도록 유도. (UUID는 String이므로)
        // * 하지만 요구사항 명세표에 "Long"이라고 되어 있음.
        // * 현 상태에서는 기능 구현을 위해 String userId (UUID)를 받는 메서드로 작성. Controller에서
        // PathVariable String으로 처리 권장.

        throw new UnsupportedOperationException("이 메서드는 userId String 버전을 사용해야 합니다.");
    }

    // 오버로딩: 실제 구현 (UUID 사용)
    public UserPublicProfileResponse getUserPublicProfile(String targetUserId, String currentUserId) {
        // 1. 대상 사용자 조회
        User targetUser = userMapper.findById(targetUserId);
        if (targetUser == null) {
            throw new RuntimeException("USER_001:사용자를 찾을 수 없습니다");
        }

        // 2. 추가 정보 조회
        java.math.BigDecimal brix = userScoreMapper.findBrixByUserId(targetUserId);
        double brixValue = (brix != null) ? brix.doubleValue() : 12.0;

        int completedChallenges = challengeMemberMapper.countCompletedChallengesByUserId(targetUserId);
        int totalMeetings = meetingVoteRecordMapper.countAttendedMeetingsByUserId(targetUserId);

        // 공통 챌린지
        java.util.List<java.util.Map<String, Object>> commonChallenges = challengeMemberMapper
                .findCommonChallenges(currentUserId, targetUserId);

        // 3. 응답 생성
        return UserPublicProfileResponse.builder()
                .userId(extractUserIdAsLong(targetUser.getId())) // UUID -> Long 변환
                .nickname(targetUser.getNickname())
                .profileImage(targetUser.getProfileImageUrl())
                .brix(brixValue)
                .stats(UserPublicProfileResponse.Stats.builder()
                        .completedChallenges(completedChallenges)
                        .totalMeetings(totalMeetings)
                        .build())
                .commonChallenges(commonChallenges)
                .isVerified("Y".equals(targetUser.getIsVerified()))
                .createdAt(
                        targetUser.getCreatedAt() != null ? targetUser.getCreatedAt().format(DATETIME_FORMATTER) : null)
                .build();
    }
}
