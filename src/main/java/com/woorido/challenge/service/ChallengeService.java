package com.woorido.challenge.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woorido.account.domain.Account;
import com.woorido.account.domain.AccountTransaction;
import com.woorido.account.domain.TransactionType;
import com.woorido.account.factory.AccountTransactionFactory;
import com.woorido.account.repository.AccountMapper;
import com.woorido.challenge.domain.Challenge;
import com.woorido.challenge.domain.ChallengeMember;
import com.woorido.challenge.dto.request.ChallengeListRequest;
import com.woorido.challenge.dto.request.CreateChallengeRequest;
import com.woorido.challenge.dto.request.MyChallengesRequest;
import com.woorido.challenge.dto.request.UpdateChallengeRequest;
import com.woorido.challenge.dto.response.ChallengeAccountResponse;
import com.woorido.challenge.dto.response.ChallengeDetailResponse;
import com.woorido.challenge.dto.response.ChallengeListResponse;
import com.woorido.challenge.dto.response.CreateChallengeResponse;
import com.woorido.challenge.dto.response.JoinChallengeResponse;
import com.woorido.challenge.dto.response.MyChallengesResponse;
import com.woorido.challenge.dto.response.UpdateChallengeResponse;
import com.woorido.challenge.dto.response.LeaveChallengeResponse;
import com.woorido.challenge.dto.response.LeaveChallengeResponse.Refund;
import com.woorido.challenge.dto.response.ChallengeMemberListResponse;
import com.woorido.challenge.repository.ChallengeMapper;
import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.challenge.repository.LedgerMapper;
import com.woorido.challenge.domain.LedgerEntry;
import com.woorido.common.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeService {

  private static final int MAX_LEADER_CHALLENGES = 3;

  private final ChallengeMapper challengeMapper;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final AccountMapper accountMapper;
  private final AccountTransactionFactory transactionFactory;
  private final JwtUtil jwtUtil;
  private final LedgerMapper ledgerMapper;

  /**
   * 챌린지 생성 (API 022)
   */
  @Transactional
  public CreateChallengeResponse createChallenge(String accessToken, CreateChallengeRequest request) {

    // 1. 토큰 검증 및 사용자 ID 추출
    String token = extractToken(accessToken);
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001: 유효하지 않은 토큰입니다");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. 리더 챌린지 한도 확인 (최대 3개)
    int leaderCount = challengeMapper.countLeaderChallenges(userId);
    if (leaderCount >= MAX_LEADER_CHALLENGES) {
      throw new RuntimeException("CHALLENGE_007: 리더는 챌린지 생성 한도(3개)를 초과할 수 없습니다");
    }

    // 3. 유효성 검증
    validateRequest(request);

    // 4. 챌린지 생성
    String challengeId = UUID.randomUUID().toString();
    Challenge challenge = Challenge.builder()
        .id(challengeId)
        .name(request.getName())
        .description(request.getDescription())
        .category(request.getCategory())
        .creatorId(userId)
        .currentMembers(1) // 리더 포함
        .minMembers(3)
        .maxMembers(request.getMaxMembers())
        .balance(0L)
        .monthlyFee(request.getSupportAmount())
        .depositAmount(request.getDepositAmount())
        .status("RECRUITING")
        .thumbnailUrl(request.getThumbnailImage())
        .build();

    challengeMapper.insert(challenge);

    // 5. 챌린지 멤버 생성 (리더)
    String memberId = UUID.randomUUID().toString();
    String depositStatus = request.getDepositAmount() > 0 ? "LOCKED" : "NONE";
    LocalDateTime depositLockedAt = request.getDepositAmount() > 0 ? LocalDateTime.now() : null;

    ChallengeMember member = ChallengeMember.builder()
        .id(memberId)
        .challengeId(challengeId)
        .userId(userId)
        .role("LEADER")
        .depositStatus(depositStatus)
        .depositLockedAt(depositLockedAt)
        .entryFeeAmount(0L)
        .privilegeStatus("ACTIVE")
        .totalSupportPaid(0L)
        .autoPayEnabled("Y")
        .build();

    challengeMemberMapper.insert(member);

    // 6. 보증금 잠금 처리
    if (request.getDepositAmount() > 0) {
      lockDeposit(userId, challengeId, request.getDepositAmount());
    }

    // 7. 응답 생성
    LocalDateTime now = LocalDateTime.now();
    return CreateChallengeResponse.builder()
        .challengeId(challengeId)
        .name(request.getName())
        .status("RECRUITING")
        .memberCount(CreateChallengeResponse.MemberCount.builder()
            .current(1)
            .max(request.getMaxMembers())
            .build())
        .myRole("LEADER")
        .createdAt(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .message("챌린지가 생성되었습니다")
        .build();
  }

  /**
   * Authorization 헤더에서 Bearer 토큰 추출
   */
  private String extractToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001: 인증 토큰이 필요합니다");
    }
    return authorization.substring(7);
  }

  /**
   * 요청 유효성 검증
   */
  private void validateRequest(CreateChallengeRequest request) {
    // supportAmount는 10,000원 단위여야 함
    if (request.getSupportAmount() % 10000 != 0) {
      throw new RuntimeException("VALIDATION_001: 월 서포트 금액은 10,000원 단위여야 합니다");
    }

    // depositAmount는 supportAmount의 1~3배
    long minDeposit = request.getSupportAmount();
    long maxDeposit = request.getSupportAmount() * 3;
    if (request.getDepositAmount() < minDeposit || request.getDepositAmount() > maxDeposit) {
      throw new RuntimeException("VALIDATION_001: 보증금은 서포트 금액의 1~3배여야 합니다");
    }

    // startDate는 7일 후 이상
    LocalDate startDate = LocalDate.parse(request.getStartDate());
    LocalDate minStartDate = LocalDate.now().plusDays(7);
    if (startDate.isBefore(minStartDate)) {
      throw new RuntimeException("VALIDATION_001: 시작일은 최소 7일 후여야 합니다");
    }
  }

  /**
   * 보증금 잠금 처리
   */
  private void lockDeposit(String userId, String challengeId, Long depositAmount) {
    Account account = accountMapper.findByUserId(userId);
    if (account == null) {
      throw new RuntimeException("ACCOUNT_001: 계좌를 찾을 수 없습니다");
    }

    // 잔액 확인
    if (account.getBalance() < depositAmount) {
      throw new RuntimeException("ACCOUNT_002: 잔액이 부족합니다");
    }

    // 스냅샷 저장
    Long balanceBefore = account.getBalance();
    Long lockedBefore = account.getLockedBalance();

    // 잔액 변경 및 잠금
    account.setBalance(balanceBefore - depositAmount);
    account.setLockedBalance(lockedBefore + depositAmount);

    // 낙관적 락으로 업데이트
    int updated = accountMapper.update(account);
    if (updated == 0) {
      throw new RuntimeException("ACCOUNT_003: 동시성 문제가 발생했습니다. 다시 시도해주세요");
    }

    // 트랜잭션 기록
    AccountTransaction tx = transactionFactory.createLockTransaction(
        account.getId(),
        depositAmount,
        balanceBefore,
        account.getBalance(),
        lockedBefore,
        account.getLockedBalance(),
        challengeId,
        "챌린지 보증금 잠금");
    accountMapper.saveTransaction(tx);

  }

  /**
   * 챌린지 목록 조회 (API 023)
   */
  @Transactional(readOnly = true)
  public ChallengeListResponse getChallengeList(ChallengeListRequest request) {

    // 1. 목록 조회
    List<Map<String, Object>> challenges = challengeMapper.findAllWithFilter(
        request.getStatus(),
        request.getCategory(),
        request.getSortField(),
        request.getSortDirection(),
        request.getOffset(),
        request.getSize());

    // 2. 총 개수 조회
    long totalElements = challengeMapper.countAllWithFilter(
        request.getStatus(),
        request.getCategory());

    // 3. 결과 변환
    List<ChallengeListResponse.ChallengeItem> content = new ArrayList<>();
    for (Map<String, Object> row : challenges) {
      ChallengeListResponse.ChallengeItem item = ChallengeListResponse.ChallengeItem.builder()
          .challengeId(getString(row, "CHALLENGE_ID"))
          .name(getString(row, "NAME"))
          .description(getString(row, "DESCRIPTION"))
          .category(getString(row, "CATEGORY"))
          .status(getString(row, "STATUS"))
          .memberCount(ChallengeListResponse.MemberCount.builder()
              .current(getInteger(row, "CURRENT_MEMBERS"))
              .max(getInteger(row, "MAX_MEMBERS"))
              .build())
          .supportAmount(getLong(row, "SUPPORT_AMOUNT"))
          .thumbnailImage(getString(row, "THUMBNAIL_IMAGE"))
          .isVerified("Y".equals(getString(row, "IS_VERIFIED")))
          .leader(ChallengeListResponse.Leader.builder()
              .userId(getString(row, "LEADER_USER_ID"))
              .nickname(getString(row, "LEADER_NICKNAME"))
              .build())
          .createdAt(formatTimestamp(row.get("CREATED_AT")))
          .build();
      content.add(item);
    }

    // 4. 페이지 정보 계산
    int totalPages = (int) Math.ceil((double) totalElements / request.getSize());

    return ChallengeListResponse.builder()
        .content(content)
        .page(ChallengeListResponse.PageInfo.builder()
            .number(request.getPage())
            .size(request.getSize())
            .totalElements(totalElements)
            .totalPages(totalPages)
            .build())
        .build();
  }

  // Helper methods for Map value extraction
  private String getString(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value != null ? value.toString() : null;
  }

  private Integer getInteger(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (value == null)
      return null;
    if (value instanceof Number)
      return ((Number) value).intValue();
    return Integer.parseInt(value.toString());
  }

  private Long getLong(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (value == null)
      return null;
    if (value instanceof Number)
      return ((Number) value).longValue();
    return Long.parseLong(value.toString());
  }

  private String formatTimestamp(Object timestamp) {
    if (timestamp == null)
      return null;
    if (timestamp instanceof java.sql.Timestamp) {
      return ((java.sql.Timestamp) timestamp).toLocalDateTime()
          .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    return timestamp.toString();
  }

  /**
   * 챌린지 상세 조회 (API 024)
   */
  @Transactional(readOnly = true)
  public ChallengeDetailResponse getChallengeDetail(String challengeId, String accessToken) {

    // 1. 챌린지 상세 조회
    Map<String, Object> challenge = challengeMapper.findDetailById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001: 챌린지를 찾을 수 없습니다");
    }

    // 2. 토큰에서 사용자 ID 추출 (선택적)
    String userId = null;
    Boolean isMember = false;
    ChallengeDetailResponse.MyMembership myMembership = null;

    if (accessToken != null && accessToken.startsWith("Bearer ")) {
      try {
        String token = accessToken.substring(7);
        if (jwtUtil.validateToken(token)) {
          userId = jwtUtil.getUserIdFromToken(token);

          // 3. 사용자 멤버십 조회
          Map<String, Object> membership = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
          if (membership != null) {
            isMember = true;
            myMembership = ChallengeDetailResponse.MyMembership.builder()
                .memberId(getString(membership, "MEMBER_ID"))
                .role(getString(membership, "ROLE"))
                .joinedAt(formatTimestamp(membership.get("JOINED_AT")))
                .status(getString(membership, "STATUS"))
                .build();
          }
        }
      } catch (Exception e) {
        // 토큰 검증 실패시 비회원으로 처리
        System.out.println("토큰 검증 실패 (비회원 조회): " + e.getMessage());
      }
    }

    // 4. 응답 생성
    return ChallengeDetailResponse.builder()
        .challengeId(getString(challenge, "CHALLENGE_ID"))
        .name(getString(challenge, "NAME"))
        .description(getString(challenge, "DESCRIPTION"))
        .category(getString(challenge, "CATEGORY"))
        .status(getString(challenge, "STATUS"))
        .memberCount(ChallengeDetailResponse.MemberCount.builder()
            .current(getInteger(challenge, "CURRENT_MEMBERS"))
            .max(getInteger(challenge, "MAX_MEMBERS"))
            .build())
        .supportAmount(getLong(challenge, "SUPPORT_AMOUNT"))
        .depositAmount(getLong(challenge, "DEPOSIT_AMOUNT"))
        .thumbnailImage(getString(challenge, "THUMBNAIL_IMAGE"))
        .isVerified("Y".equals(getString(challenge, "IS_VERIFIED")))
        .leader(ChallengeDetailResponse.Leader.builder()
            .id(getString(challenge, "LEADER_ID"))
            .nickname(getString(challenge, "LEADER_NICKNAME"))
            .build())
        .account(ChallengeDetailResponse.Account.builder()
            .balance(getLong(challenge, "BALANCE"))
            .build())
        .isMember(isMember)
        .myMembership(myMembership)
        .startedAt(formatTimestamp(challenge.get("STARTED_AT")))
        .createdAt(formatTimestamp(challenge.get("CREATED_AT")))
        .build();
  }

  /**
   * 챌린지 수정 (API 025)
   */
  @Transactional
  public UpdateChallengeResponse updateChallenge(String challengeId, String accessToken,
      UpdateChallengeRequest request) {

    // 1. 토큰 검증 및 사용자 ID 추출
    if (accessToken == null || !accessToken.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001: 인증이 필요합니다");
    }
    String token = accessToken.substring(7);
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001: 유효하지 않은 토큰입니다");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. 챌린지 조회
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001: 챌린지를 찾을 수 없습니다");
    }

    // 3. 리더 권한 확인
    int isLeader = challengeMapper.isLeader(challengeId, userId);
    if (isLeader == 0) {
      throw new RuntimeException("CHALLENGE_004: 리더만 수정할 수 있습니다");
    }

    // 4. maxMembers 검증 (현재 인원 이상, 증가만 가능)
    if (request.getMaxMembers() != null) {
      if (request.getMaxMembers() < challenge.getCurrentMembers()) {
        throw new RuntimeException("VALIDATION_001: 최대 인원은 현재 인원(" + challenge.getCurrentMembers() + ")명 이상이어야 합니다");
      }
      if (request.getMaxMembers() < challenge.getMaxMembers()) {
        throw new RuntimeException("VALIDATION_001: 최대 인원은 증가만 가능합니다");
      }
    }

    // 5. 수정할 필드 설정 (null이 아닌 값만 업데이트)
    if (request.getName() != null) {
      challenge.setName(request.getName());
    }
    if (request.getDescription() != null) {
      challenge.setDescription(request.getDescription());
    }
    if (request.getThumbnailImage() != null) {
      challenge.setThumbnailUrl(request.getThumbnailImage());
    }
    if (request.getRules() != null) {
      challenge.setRules(request.getRules());
    }
    if (request.getMaxMembers() != null) {
      challenge.setMaxMembers(request.getMaxMembers());
    }

    // 6. 업데이트 실행
    challengeMapper.update(challenge);

    return UpdateChallengeResponse.builder()
        .challengeId(challenge.getId())
        .name(challenge.getName())
        .description(challenge.getDescription())
        .maxMembers(challenge.getMaxMembers())
        .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .message("챌린지 정보가 수정되었습니다")
        .build();
  }

  /**
   * API 027: 내 챌린지 목록 조회
   */
  public MyChallengesResponse getMyChallenges(String accessToken, MyChallengesRequest request) {

    // 1. 토큰 검증 및 사용자 ID 추출
    String token = accessToken.replace("Bearer ", "");
    String userId = jwtUtil.getUserIdFromToken(token);
    System.out.println("userId: " + userId);

    // 2. 내 챌린지 목록 조회
    List<Map<String, Object>> myChallenges = challengeMapper.findMyChallenges(
        userId, request.getRole(), request.getStatus());

    // 3. 응답 데이터 변환
    List<MyChallengesResponse.MyChallengeItem> challengeItems = new ArrayList<>();
    int leaderCount = 0;
    int followerCount = 0;
    long totalMonthlySupport = 0;

    for (Map<String, Object> row : myChallenges) {
      String role = row.get("MY_ROLE") != null ? row.get("MY_ROLE").toString() : null;

      // Summary 계산
      if ("LEADER".equals(role)) {
        leaderCount++;
      } else if ("FOLLOWER".equals(role)) {
        followerCount++;
      }

      Long supportAmount = row.get("SUPPORT_AMOUNT") != null ? ((Number) row.get("SUPPORT_AMOUNT")).longValue() : 0L;
      totalMonthlySupport += supportAmount;

      MyChallengesResponse.MyChallengeItem item = MyChallengesResponse.MyChallengeItem.builder()
          .challengeId(row.get("CHALLENGE_ID") != null ? row.get("CHALLENGE_ID").toString() : null)
          .name(row.get("NAME") != null ? row.get("NAME").toString() : null)
          .status(row.get("STATUS") != null ? row.get("STATUS").toString() : null)
          .myRole(role)
          .myStatus(row.get("MY_STATUS") != null ? row.get("MY_STATUS").toString() : null)
          .memberCount(MyChallengesResponse.MemberCount.builder()
              .current(row.get("CURRENT_MEMBERS") != null ? ((Number) row.get("CURRENT_MEMBERS")).intValue() : 0)
              .max(row.get("MAX_MEMBERS") != null ? ((Number) row.get("MAX_MEMBERS")).intValue() : 0)
              .build())
          .supportAmount(supportAmount)
          .thumbnailImage(row.get("THUMBNAIL_IMAGE") != null ? row.get("THUMBNAIL_IMAGE").toString() : null)
          .build();

      challengeItems.add(item);
    }

    // 4. Summary 생성
    MyChallengesResponse.Summary summary = MyChallengesResponse.Summary.builder()
        .totalChallenges(challengeItems.size())
        .asLeader(leaderCount)
        .asFollower(followerCount)
        .monthlySupport(totalMonthlySupport)
        .build();

    return MyChallengesResponse.builder()
        .challenges(challengeItems)
        .summary(summary)
        .build();
  }

  /**
   * API 028: 챌린지 어카운트 조회
   */
  public ChallengeAccountResponse getChallengeAccount(String challengeId, String accessToken) {

    // 1. 토큰 검증 및 사용자 ID 추출
    String token = accessToken.replace("Bearer ", "");
    String userId = jwtUtil.getUserIdFromToken(token);
    System.out.println("userId: " + userId);

    // 2. 챌린지 존재 확인
    Map<String, Object> accountData = challengeMapper.findChallengeAccount(challengeId);
    if (accountData == null) {
      throw new IllegalArgumentException("CHALLENGE_001");
    }

    // 3. 멤버 여부 확인
    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId);
    if (isMember == 0) {
      throw new SecurityException("CHALLENGE_003");
    }

    // 4. 최근 거래 내역 조회
    List<Map<String, Object>> recentEntries = challengeMapper.findRecentLedgerEntries(challengeId, 10);
    List<ChallengeAccountResponse.Transaction> transactions = new ArrayList<>();

    for (Map<String, Object> entry : recentEntries) {
      ChallengeAccountResponse.Transaction tx = ChallengeAccountResponse.Transaction.builder()
          .transactionId(entry.get("TRANSACTION_ID") != null ? entry.get("TRANSACTION_ID").toString() : null)
          .amount(entry.get("AMOUNT") != null ? ((Number) entry.get("AMOUNT")).longValue() : 0L)
          .type(entry.get("TYPE") != null ? entry.get("TYPE").toString() : null)
          .description(entry.get("DESCRIPTION") != null ? entry.get("DESCRIPTION").toString() : null)
          .createdAt(entry.get("CREATED_AT") != null ? entry.get("CREATED_AT").toString() : null)
          .build();
      transactions.add(tx);
    }

    // 5. 잔액 정보 추출
    Long balance = accountData.get("BALANCE") != null ? ((Number) accountData.get("BALANCE")).longValue() : 0L;
    Long lockedDeposits = accountData.get("LOCKED_DEPOSITS") != null
        ? ((Number) accountData.get("LOCKED_DEPOSITS")).longValue()
        : 0L;
    Long totalIncome = accountData.get("TOTAL_INCOME") != null ? ((Number) accountData.get("TOTAL_INCOME")).longValue()
        : 0L;
    Long totalExpense = accountData.get("TOTAL_EXPENSE") != null
        ? ((Number) accountData.get("TOTAL_EXPENSE")).longValue()
        : 0L;
    Long monthlyFee = accountData.get("MONTHLY_FEE") != null ? ((Number) accountData.get("MONTHLY_FEE")).longValue()
        : 0L;
    Integer currentMembers = accountData.get("CURRENT_MEMBERS") != null
        ? ((Number) accountData.get("CURRENT_MEMBERS")).intValue()
        : 0;

    // 6. Stats 계산
    ChallengeAccountResponse.Stats stats = ChallengeAccountResponse.Stats.builder()
        .totalSupport(totalIncome)
        .totalExpense(totalExpense)
        .totalFee(0L) // 수수료는 별도 계산 필요
        .monthlyAverage(monthlyFee * currentMembers)
        .build();

    // 7. SupportStatus (실제 납부 현황 계산)
    List<ChallengeMember> members = challengeMemberMapper.findAllByChallengeId(challengeId);
    int paidCount = 0;
    int unpaidCount = 0;
    String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

    if (monthlyFee == 0) {
      paidCount = members.size();
    } else {
      for (ChallengeMember m : members) {
        if (m.getLastSupportPaidAt() != null) {
          String paidMonth = m.getLastSupportPaidAt().format(DateTimeFormatter.ofPattern("yyyy-MM"));
          if (currentMonth.equals(paidMonth)) {
            paidCount++;
          } else {
            unpaidCount++;
          }
        } else {
          unpaidCount++;
        }
      }
    }

    ChallengeAccountResponse.SupportStatus supportStatus = ChallengeAccountResponse.SupportStatus.builder()
        .paid(paidCount)
        .unpaid(unpaidCount)
        .total(members.size())
        .build();

    return ChallengeAccountResponse.builder()
        .challengeId(challengeId)
        .balance(balance)
        .lockedDeposits(lockedDeposits)
        .availableBalance(balance) // 사용 가능 잔액 = 잔액
        .stats(stats)
        .recentTransactions(transactions)
        .supportStatus(supportStatus)
        .build();
  }

  /**
   * API 030: 챌린지 가입
   */
  @Transactional
  public JoinChallengeResponse joinChallenge(String challengeId, String accessToken) {

    // 1. 토큰 검증 및 사용자 ID 추출
    String token = accessToken.replace("Bearer ", "");
    String userId = jwtUtil.getUserIdFromToken(token);
    System.out.println("userId: " + userId);

    // 2. 챌린지 존재 확인
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new IllegalArgumentException("CHALLENGE_001");
    }

    // 3. 모집 중인 챌린지인지 확인
    if (!"RECRUITING".equals(challenge.getStatus())) {
      throw new IllegalStateException("CHALLENGE_006");
    }

    // 4. 멤버십 상태 확인 (신규/재가입/이미가입 분기)
    Map<String, Object> existingMember = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    boolean isRejoin = false;
    String existingMemberId = null;

    if (existingMember != null) {
      String status = (String) existingMember.get("STATUS");
      if ("ACTIVE".equals(status)) {
        throw new IllegalStateException("CHALLENGE_002");
      }
      // 탈퇴 상태면 재가입 진행
      isRejoin = true;
      existingMemberId = (String) existingMember.get("MEMBER_ID");
    }

    // 5. 정원 초과 확인
    if (challenge.getCurrentMembers() >= challenge.getMaxMembers()) {
      throw new IllegalStateException("CHALLENGE_005");
    }

    // 6. 사용자 계좌 조회
    Account account = accountMapper.findByUserId(userId);
    if (account == null) {
      throw new IllegalArgumentException("ACCOUNT_001");
    }

    // 7. 비용 계산
    Long deposit = challenge.getDepositAmount() != null ? challenge.getDepositAmount() : 0L;
    Long entryFee = challenge.getMonthlyFee() != null
        ? challenge.getMonthlyFee() / (challenge.getCurrentMembers() > 0 ? challenge.getCurrentMembers() : 1)
        : 0L;
    Long firstSupport = 0L; // 납입일 7일 전 이내면 첫 서포트 필요 (생략)
    Long totalCost = deposit + entryFee + firstSupport;

    // 8. 잔액 확인
    if (account.getBalance() < totalCost) {
      throw new IllegalStateException("ACCOUNT_004");
    }

    // 9. 잔액 차감 및 보증금 잠금
    Long balanceBefore = account.getBalance();
    Long lockedBefore = account.getLockedBalance();

    // 비용 차감 (가용 잔액에서 차감)
    if (entryFee > 0) {
      account.setBalance(account.getBalance() - entryFee);
    }
    if (firstSupport > 0) {
      account.setBalance(account.getBalance() - firstSupport);
    }

    // 보증금 잠금 (가용 잔액 차감 및 잠금액 증가)
    if (deposit > 0) {
      account.setBalance(account.getBalance() - deposit);
      account.setLockedBalance(account.getLockedBalance() + deposit);
    }

    int updateResult = accountMapper.update(account);
    if (updateResult == 0) {
      throw new RuntimeException("계좌 업데이트 실패 - 동시성 문제");
    }

    // Transaction 기록
    // 9-1. 입장료 (ENTRY_FEE)
    if (entryFee > 0) {
      AccountTransaction entryFeeTx = AccountTransaction.builder()
          .id(UUID.randomUUID().toString())
          .accountId(account.getId())
          .type(TransactionType.ENTRY_FEE)
          .amount(-entryFee)
          .balanceBefore(balanceBefore)
          .balanceAfter(balanceBefore - entryFee)
          .lockedBefore(lockedBefore)
          .lockedAfter(lockedBefore)
          .relatedChallengeId(challengeId)
          .description("챌린지 입장료")
          .createdAt(LocalDateTime.now())
          .build();
      accountMapper.saveTransaction(entryFeeTx);
      balanceBefore -= entryFee; // 다음 트랜잭션을 위해 갱신
    }

    // 9-2. 첫 서포트 (SUPPORT)
    if (firstSupport > 0) {
      AccountTransaction supportTx = AccountTransaction.builder()
          .id(UUID.randomUUID().toString())
          .accountId(account.getId())
          .type(TransactionType.SUPPORT)
          .amount(-firstSupport)
          .balanceBefore(balanceBefore)
          .balanceAfter(balanceBefore - firstSupport)
          .lockedBefore(lockedBefore)
          .lockedAfter(lockedBefore)
          .relatedChallengeId(challengeId)
          .description("챌린지 첫 서포트")
          .createdAt(LocalDateTime.now())
          .build();
      accountMapper.saveTransaction(supportTx);
      balanceBefore -= firstSupport;
    }

    // 9-3. 보증금 잠금 (LOCK)
    if (deposit > 0) {
      AccountTransaction lockTx = AccountTransaction.builder()
          .id(UUID.randomUUID().toString())
          .accountId(account.getId())
          .type(TransactionType.LOCK)
          .amount(-deposit) // 가용잔액 감소
          .balanceBefore(balanceBefore)
          .balanceAfter(balanceBefore - deposit)
          .lockedBefore(lockedBefore)
          .lockedAfter(lockedBefore + deposit)
          .relatedChallengeId(challengeId)
          .description("챌린지 보증금 잠금")
          .createdAt(LocalDateTime.now())
          .build();
      accountMapper.saveTransaction(lockTx);
    }

    // 10. 챌린지 멤버 등록
    // 10. 챌린지 멤버 등록
    String memberId = isRejoin ? existingMemberId : UUID.randomUUID().toString();
    ChallengeMember member = ChallengeMember.builder()
        .id(memberId)
        .challengeId(challengeId)
        .userId(userId)
        .role("FOLLOWER")
        .depositStatus("LOCKED")
        .privilegeStatus("ACTIVE")
        .entryFeeAmount(entryFee)
        .totalSupportPaid(0L)
        .autoPayEnabled("Y")
        .joinedAt(LocalDateTime.now())
        .build();

    if (isRejoin) {
      challengeMemberMapper.updateRejoinMember(member);
    } else {
      challengeMemberMapper.insert(member);
    }

    // 11. 챌린지 멤버 수 증가
    int incResult = challengeMapper.incrementCurrentMembers(challengeId);
    if (incResult == 0) {
      throw new IllegalStateException("CHALLENGE_005"); // 정원 초과
    }

    // [NEW] 12. 챌린지 밸런스 및 장부(Ledger) 업데이트
    Long totalIncome = 0L;
    if (entryFee > 0)
      totalIncome += entryFee;
    if (firstSupport > 0)
      totalIncome += firstSupport;

    if (totalIncome > 0) {
      Long chBalanceBefore = challenge.getBalance();
      Long chBalanceAfter = chBalanceBefore + totalIncome;

      challenge.setBalance(chBalanceAfter);
      int chUpdateResult = challengeMapper.updateBalance(challenge);
      if (chUpdateResult == 0) {
        throw new RuntimeException("CHALLENGE_003: 챌린지 정보 업데이트 실패 (Concurrent Update)");
      }

      if (entryFee > 0) {
        LedgerEntry ledger = LedgerEntry.builder()
            .id(UUID.randomUUID().toString())
            .challengeId(challengeId)
            .type("ENTRY_FEE")
            .amount(entryFee)
            .description("챌린지 입장료")
            .balanceBefore(chBalanceBefore)
            .balanceAfter(chBalanceBefore + entryFee)
            .relatedUserId(userId)
            .createdAt(LocalDateTime.now())
            .build();
        ledgerMapper.insert(ledger);
        chBalanceBefore += entryFee;
      }

      if (firstSupport > 0) {
        LedgerEntry ledger = LedgerEntry.builder()
            .id(UUID.randomUUID().toString())
            .challengeId(challengeId)
            .type("SUPPORT")
            .amount(firstSupport)
            .description("챌린지 첫 서포트")
            .balanceBefore(chBalanceBefore)
            .balanceAfter(chBalanceBefore + firstSupport)
            .relatedUserId(userId)
            .createdAt(LocalDateTime.now())
            .build();
        ledgerMapper.insert(ledger);
      }
    }

    // 13. 응답 생성
    JoinChallengeResponse.Breakdown breakdown = JoinChallengeResponse.Breakdown.builder()
        .entryFee(entryFee)
        .deposit(deposit)
        .firstSupport(firstSupport)
        .total(totalCost)
        .build();

    return JoinChallengeResponse.builder()
        .memberId(memberId)
        .challengeId(challengeId)
        .challengeName(challenge.getName())
        .role("FOLLOWER")
        .status("ACTIVE")
        .breakdown(breakdown)
        .newBalance(account.getBalance())
        .joinedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .message("챌린지에 가입되었습니다")
        .build();
  }

  /**
   * API 031: 챌린지 탈퇴
   */
  @Transactional
  public LeaveChallengeResponse leaveChallenge(String challengeId, String accessToken) {

    // 1. 토큰 검증 및 사용자 ID 추출 (Bearer 제거)
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    String userId = jwtUtil.getUserIdFromToken(token);
    System.out.println("userId: " + userId);

    // 2. 리더 권한 확인 (리더는 탈퇴 불가)
    int isLeader = challengeMapper.isLeader(challengeId, userId);
    if (isLeader > 0) {
      throw new RuntimeException("MEMBER_002: 리더는 탈퇴할 수 없습니다 (위임 후 탈퇴)");
    }

    // 3. 챌린지 존재 여부 확인
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001: 챌린지를 찾을 수 없습니다");
    }

    // 4. 멤버 여부 확인
    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId);
    if (isMember == 0) {
      throw new RuntimeException("CHALLENGE_003: 챌린지 멤버가 아닙니다");
    }

    // 5. 환불 금액 계산
    Long deposit = challenge.getDepositAmount() != null ? challenge.getDepositAmount() : 0L;
    Long deducted = 0L; // 차감 금액 (나중에 구현)
    Long netRefund = deposit > deducted ? deposit - deducted : 0L;

    // 6. 사용자 계좌 환불 처리
    Account account = accountMapper.findByUserId(userId);
    if (account == null) {
      throw new RuntimeException("ACCOUNT_001: 계좌를 찾을 수 없습니다");
    }

    if (deposit > 0) {
      Long balanceBefore = account.getBalance();
      Long lockedBefore = account.getLockedBalance();

      // 잔액 증가, 잠금액 감소
      account.setBalance(balanceBefore + netRefund);
      account.setLockedBalance(lockedBefore - deposit);

      int updateResult = accountMapper.update(account);
      if (updateResult == 0) {
        throw new RuntimeException("ACCOUNT_003: 계좌 업데이트 실패 - 동시성 문제");
      }

      // 7. Transaction 기록 (REFUND)
      AccountTransaction refundTx = AccountTransaction.builder()
          .id(UUID.randomUUID().toString())
          .accountId(account.getId())
          .type(TransactionType.REFUND)
          .amount(netRefund)
          .balanceBefore(balanceBefore)
          .balanceAfter(account.getBalance())
          .lockedBefore(lockedBefore)
          .lockedAfter(account.getLockedBalance())
          .relatedChallengeId(challengeId)
          .description("챌린지 탈퇴 환불")
          .createdAt(LocalDateTime.now())
          .build();
      accountMapper.saveTransaction(refundTx);
    }

    // 8. 챌린지 밸런스 및 멤버 수 감소
    challengeMapper.decrementCurrentMembers(challengeId);

    // 9. 멤버 상태 업데이트 (Soft Delete)
    int result = challengeMemberMapper.updateLeaveMember(userId, challengeId, "SELF_LEAVE");
    if (result == 0) {
      throw new RuntimeException("MEMBER_004: 멤버 정보 업데이트 실패");
    }

    // 10. 응답 생성
    Refund refund = Refund.builder()
        .deposit(deposit)
        .deducted(deducted)
        .netRefund(netRefund)
        .build();

    return LeaveChallengeResponse.builder()
        .challengeId(challengeId)
        .challengeName(challenge.getName())
        .refund(refund)
        .newBalance(account.getBalance())
        .leftAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .build();
  }

  /**
   * API 032: 챌린지 멤버 목록 조회
   */
  public ChallengeMemberListResponse getChallengeMembers(String challengeId, String accessToken) {

    // 1. 토큰 검증 및 사용자 ID 추출
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    String requestUserId = jwtUtil.getUserIdFromToken(token);

    // 2. 챌린지 존재 여부 확인
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001: 챌린지를 찾을 수 없습니다");
    }

    // 3. 요청자가 멤버인지 확인 (멤버만 조회 가능)
    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, requestUserId);
    if (isMember == 0) {
      throw new RuntimeException("CHALLENGE_003: 챌린지 멤버가 아닙니다");
    }

    // 4. 멤버 목록 조회 (User Join)
    List<Map<String, Object>> membersData = challengeMemberMapper.findMembersWithUserInfo(challengeId);

    // 5. Response 매핑
    List<ChallengeMemberListResponse.MemberInfo> memberList = new ArrayList<>();
    int activeCount = 0;
    int overdueCount = 0;
    int graceCount = 0;

    for (Map<String, Object> data : membersData) {
      String status = (String) data.get("STATUS");
      // String role = (String) data.get("ROLE");
      // java.math.BigDecimal or Long conversion might be needed for numbers depending
      // on Driver

      // Count stats
      if ("ACTIVE".equals(status))
        activeCount++;
      else if ("OVERDUE".equals(status))
        overdueCount++;
      // GRACE_PERIOD logic not implemented yet, map to ACTIVE or OVERDUE for now

      ChallengeMemberListResponse.UserInfo userInfo = ChallengeMemberListResponse.UserInfo.builder()
          .userId((String) data.get("USER_ID"))
          .nickname((String) data.get("NICKNAME"))
          .profileImage((String) data.get("PROFILE_IMAGE"))
          .brix(0.0) // Temporary
          .build();

      ChallengeMemberListResponse.SupportStatus supportStatus = ChallengeMemberListResponse.SupportStatus.builder()
          .thisMonth("PAID") // Temporary logic
          .consecutivePaid(1) // Temporary logic
          .overdueCount(0)
          .build();

      memberList.add(ChallengeMemberListResponse.MemberInfo.builder()
          .memberId((String) data.get("MEMBER_ID"))
          .user(userInfo)
          .role((String) data.get("ROLE"))
          .status(status)
          .supportStatus(supportStatus)
          .attendanceRate(100.0) // Temporary logic
          .joinedAt(data.get("JOINED_AT") != null ? data.get("JOINED_AT").toString() : null)
          .build());
    }

    ChallengeMemberListResponse.Summary summary = ChallengeMemberListResponse.Summary.builder()
        .total(memberList.size())
        .active(activeCount)
        .overdue(overdueCount)
        .gracePeriod(graceCount)
        .build();

    return ChallengeMemberListResponse.builder()
        .members(memberList)
        .summary(summary)
        .build();
  }
}
