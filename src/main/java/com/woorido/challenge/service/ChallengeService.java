package com.woorido.challenge.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woorido.account.domain.Account;
import com.woorido.account.domain.AccountTransaction;
import com.woorido.account.domain.TransactionType;
import com.woorido.account.factory.AccountTransactionFactory;
import com.woorido.account.repository.AccountMapper;
import com.woorido.challenge.domain.Challenge;
import com.woorido.challenge.domain.ChallengeCategory;
import com.woorido.challenge.domain.ChallengeMember;
import com.woorido.challenge.domain.ChallengeRole;
import com.woorido.challenge.domain.ChallengeStatus;
import com.woorido.challenge.domain.DepositStatus;
import com.woorido.challenge.domain.PrivilegeStatus;
import com.woorido.challenge.dto.request.ChallengeListRequest;
import com.woorido.challenge.dto.request.CreateChallengeRequest;
import com.woorido.challenge.dto.request.MyChallengesRequest;
import com.woorido.challenge.dto.request.UpdateChallengeRequest;
import com.woorido.challenge.dto.response.ChallengeAccountResponse;
import com.woorido.challenge.dto.response.ChallengeDetailResponse;
import com.woorido.challenge.dto.response.ChallengeLedgerGraphResponse;
import com.woorido.challenge.dto.response.ChallengeListResponse;
import com.woorido.challenge.dto.response.CreateChallengeResponse;
import com.woorido.challenge.dto.response.JoinChallengeResponse;
import com.woorido.challenge.dto.response.MyChallengesResponse;
import com.woorido.challenge.dto.response.UpdateChallengeResponse;
import com.woorido.challenge.dto.response.LeaveChallengeResponse;
import com.woorido.challenge.dto.response.ChallengeDeleteResponse;
import com.woorido.challenge.dto.response.DelegateLeaderResponse;

import com.woorido.challenge.dto.response.ChallengeMemberListResponse;
import com.woorido.challenge.dto.request.UpdateSupportSettingsRequest;
import com.woorido.challenge.dto.response.UpdateSupportSettingsResponse;
import com.woorido.challenge.repository.ChallengeMapper;
import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.challenge.repository.LedgerMapper;
import com.woorido.challenge.domain.LedgerEntry;
import com.woorido.common.util.JwtUtil;
import com.woorido.django.ledger.client.DjangoLedgerClient;
import com.woorido.django.ledger.dto.DjangoLedgerGraphRequest;
import com.woorido.django.ledger.dto.DjangoLedgerGraphResponse;
import com.woorido.meeting.repository.MeetingMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeService {
  // Learning note:
  // - Read flow as: validate auth/role -> execute domain logic -> persist via Mapper.

  private static final int MAX_LEADER_CHALLENGES = 3;
  private static final int DEFAULT_GRAPH_MONTHS = 6;
  private static final int MIN_GRAPH_MONTHS = 1;
  private static final int MAX_GRAPH_MONTHS = 24;
  private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
  private static final String LEDGER_STATUS_OK = "LEDGER_OK";
  private static final String LEDGER_STATUS_NETWORK = "LEDGER_004";
  private static final String GRAPH_SOURCE_DJANGO = "DJANGO";
  private static final String GRAPH_SOURCE_JAVA_FALLBACK = "JAVA_FALLBACK";

  private final ChallengeMapper challengeMapper;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final AccountMapper accountMapper;
  private final AccountTransactionFactory accountTransactionFactory;
  private final JwtUtil jwtUtil;
  private final LedgerMapper ledgerMapper;
  private final DjangoLedgerClient djangoLedgerClient;
  private final MeetingMapper meetingMapper;

  /**
   * 챌린지를 생성하고 생성자를 리더 멤버로 등록한다.
   * - 토큰 인증
   * - 리더 생성 개수 제한(최대 3개)
   * - 이름 중복/요청값 검증
   * - 보증금 잠금(선택)
   */
  @Transactional
  // [학습] 챌린지를 생성하고 리더 멤버를 등록한다.
  public CreateChallengeResponse createChallenge(String accessToken, CreateChallengeRequest request) {

    String token = extractToken(accessToken);
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001:Invalid access token");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    int leaderCount = challengeMapper.countLeaderChallenges(userId);
    if (leaderCount >= MAX_LEADER_CHALLENGES) {
      throw new RuntimeException("CHALLENGE_007:리더는 동시에 최대 3개의 챌린지만 생성할 수 있습니다");
    }

    validateRequest(request);


    String normalizedName = request.getName().trim();
    if (challengeMapper.countByName(normalizedName) > 0) {
      throw new RuntimeException("CHALLENGE_011:이미 동일한 이름의 챌린지가 존재합니다");
    }

    LocalDateTime now = LocalDateTime.now();
    String challengeId = UUID.randomUUID().toString();
    Challenge challenge = Challenge.builder()
        .id(challengeId)
        .name(normalizedName)
        .description(request.getDescription())
        .category(ChallengeCategory.valueOf(request.getCategory()))
        .creatorId(userId)
        .currentMembers(1)
        .minMembers(3)
        .maxMembers(request.getMaxMembers())
        .balance(0L)
        .monthlyFee(request.getSupportAmount())
        .depositAmount(request.getDepositAmount())
        .status(ChallengeStatus.RECRUITING)
        .thumbnailUrl(request.getThumbnailImage())
        .bannerUrl(request.getBannerImage())
        .rules(request.getRules())
        .build();

    challengeMapper.insert(challenge);

    String memberId = UUID.randomUUID().toString();
    DepositStatus depositStatus = request.getDepositAmount() > 0 ? DepositStatus.LOCKED : DepositStatus.NONE;
    LocalDateTime depositLockedAt = request.getDepositAmount() > 0 ? LocalDateTime.now() : null;

    ChallengeMember member = ChallengeMember.builder()
        .id(memberId)
        .challengeId(challengeId)
        .userId(userId)
        .role(ChallengeRole.LEADER)
        .depositStatus(depositStatus)
        .depositLockedAt(depositLockedAt)
        .entryFeeAmount(0L)
        .privilegeStatus(PrivilegeStatus.ACTIVE)
        .totalSupportPaid(0L)
        .autoPayEnabled("Y")
        .joinedAt(now)
        .build();

    challengeMemberMapper.insert(member);

    if (request.getDepositAmount() > 0) {
      lockDeposit(userId, challengeId, request.getDepositAmount());
    }

    return CreateChallengeResponse.builder()
        .challengeId(challengeId)
        .name(normalizedName)
        .status("RECRUITING")
        .memberCount(CreateChallengeResponse.MemberCount.builder()
            .current(1)
            .max(request.getMaxMembers())
            .build())
        .myRole("LEADER")
        .createdAt(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .message("Challenge created successfully")
        .build();
  }

  /**
   * Authorization 헤더에서 Bearer 토큰을 추출한다.
   */
  // [학습] Authorization 헤더에서 Bearer 토큰을 추출한다.
  private String extractToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001:Authorization header is required");
    }
    return authorization.substring(7);
  }

  /**
   * Authorization 헤더(Bearer) 또는 토큰 문자열에서 userId를 추출한다.
   */
  private String resolveUserId(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      throw new RuntimeException("AUTH_001:Authorization header is required");
    }
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001:Invalid access token");
    }
    return jwtUtil.getUserIdFromToken(token);
  }

  /**
   * 챌린지 생성 요청값을 정책 기준으로 검증한다.
   * - 후원금: 10,000원 단위
   * - 보증금: 후원금과 동일
   * - 시작일: 오늘 기준 최소 7일 이후
   */
  // [학습] 챌린지 생성 요청값의 정책을 검증한다.
  private void validateRequest(CreateChallengeRequest request) {
    if (request.getSupportAmount() % 10000 != 0) {
      throw new RuntimeException("VALIDATION_001:Support amount must be in units of 10000");
    }

    if (!request.getDepositAmount().equals(request.getSupportAmount())) {
      throw new RuntimeException("VALIDATION_001:Deposit amount must match support amount");
    }

    LocalDate startDate = LocalDate.parse(request.getStartDate());
    LocalDate minStartDate = LocalDate.now().plusDays(7);
    if (startDate.isBefore(minStartDate)) {
      throw new RuntimeException("VALIDATION_001:Start date must be at least 7 days later");
    }
  }

  /**
   * 계좌 잔액에서 보증금을 잠금 처리하고 거래 내역을 기록한다.
   */
  // [학습] 가입 보증금을 계좌에서 잠금 처리한다.
  private void lockDeposit(String userId, String challengeId, Long depositAmount) {
    Account account = accountMapper.findByUserId(userId);
    if (account == null) {
      throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
    }

    if (account.getBalance() < depositAmount) {
      throw new RuntimeException("ACCOUNT_002:잔액이 부족합니다");
    }

    Long balanceBefore = account.getBalance();
    Long lockedBefore = account.getLockedBalance();

    account.setBalance(balanceBefore - depositAmount);
    account.setLockedBalance(lockedBefore + depositAmount);

    int updated = accountMapper.update(account);
    if (updated == 0) {
      throw new RuntimeException("ACCOUNT_003:잔액 업데이트에 실패했습니다. 다시 시도해주세요");
    }

    AccountTransaction transaction = accountTransactionFactory.createLockTransaction(
        account.getId(),
        depositAmount,
        balanceBefore,
        account.getBalance(),
        lockedBefore,
        account.getLockedBalance(),
        challengeId,
        "챌린지 가입 보증금 예치");
    accountMapper.saveTransaction(transaction);

  }

  /**
   * 필터/정렬/페이지 조건으로 챌린지 목록을 조회한다.
   */
  @Transactional(readOnly = true)
  // [학습] 챌린지 목록을 필터/정렬 조건으로 조회한다.
  public ChallengeListResponse getChallengeList(ChallengeListRequest request) {

    List<Map<String, Object>> challenges = challengeMapper.findAllWithFilter(
        request.getStatus(),
        request.getCategory(),
        request.getSortField(),
        request.getSortDirection(),
        request.getOffset(),
        request.getSize());

    long totalElements = challengeMapper.countAllWithFilter(
        request.getStatus(),
        request.getCategory());

    List<ChallengeListResponse.ChallengeItem> content = new ArrayList<>();
    for (Map<String, Object> row : challenges) {
      ChallengeListResponse.ChallengeItem item = ChallengeListResponse.ChallengeItem.builder()
          .challengeId(getString(row, "CHALLENGE_ID"))
          .name(getString(row, "NAME"))
          .description(getString(row, "DESCRIPTION"))
          .category(getString(row, "CATEGORY"))
          .status(row.get("STATUS") != null ? row.get("STATUS").toString() : null)
          .memberCount(ChallengeListResponse.MemberCount.builder()
              .current(getInteger(row, "CURRENT_MEMBERS"))
              .max(getInteger(row, "MAX_MEMBERS"))
              .build())
          .supportAmount(getLong(row, "SUPPORT_AMOUNT"))
          .thumbnailImage(getString(row, "THUMBNAIL_IMAGE"))
          .bannerImage(getString(row, "BANNER_IMAGE"))
          .isVerified("Y".equals(getString(row, "IS_VERIFIED")))
          .leader(ChallengeListResponse.Leader.builder()
              .userId(getString(row, "LEADER_USER_ID"))
              .nickname(getString(row, "LEADER_NICKNAME"))
              .build())
          .createdAt(formatTimestamp(row.get("CREATED_AT")))
          .build();
      content.add(item);
    }

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
  // [학습] Map 값을 문자열로 안전하게 변환한다.
  private String getString(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value != null ? value.toString() : null;
  }

  // [학습] Map 값을 정수로 안전하게 변환한다.
  private Integer getInteger(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (value == null)
      return null;
    if (value instanceof Number)
      return ((Number) value).intValue();
    return Integer.parseInt(value.toString());
  }

  // [학습] Map 값을 Long으로 안전하게 변환한다.
  private Long getLong(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (value == null)
      return null;
    if (value instanceof Number)
      return ((Number) value).longValue();
    return Long.parseLong(value.toString());
  }

  // [학습] 타임스탬프 값을 API 응답 문자열로 포맷한다.
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
   * 챌린지 상세를 조회한다.
   * 토큰이 유효하면 현재 사용자 멤버십 정보도 함께 내려준다.
   */
  @Transactional(readOnly = true)
  // [학습] 챌린지 상세 정보를 조회한다.
  public ChallengeDetailResponse getChallengeDetail(String challengeId, String accessToken) {

    Map<String, Object> challenge = challengeMapper.findDetailById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    String userId = null;
    Boolean isMember = false;
    ChallengeDetailResponse.MyMembership myMembership = null;

    if (accessToken != null && !accessToken.isBlank()) {
      String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
      if (jwtUtil.validateToken(token)) {
        userId = jwtUtil.getUserIdFromToken(token);

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
    }

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
        .bannerImage(getString(challenge, "BANNER_IMAGE"))
        .rules(getString(challenge, "RULES"))
        .isVerified("Y".equals(getString(challenge, "IS_VERIFIED")))
        .leader(ChallengeDetailResponse.Leader.builder()
            .id(getString(challenge, "LEADER_ID"))
            .nickname(getString(challenge, "LEADER_NICKNAME"))
            .brix(challenge.get("LEADER_BRIX") != null ? Double.parseDouble(challenge.get("LEADER_BRIX").toString()) : 12.0)
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
   * 리더 권한으로 챌린지 정보를 수정한다.
   * 이름 중복과 최대 인원 정책을 먼저 검증한 뒤 업데이트한다.
   */
  @Transactional
  // [학습] 리더 권한으로 챌린지 정보를 수정한다.
  public UpdateChallengeResponse updateChallenge(String challengeId, String accessToken,
      UpdateChallengeRequest request) {

    if (accessToken == null || !accessToken.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001:Authorization header is required");
    }
    String token = accessToken.substring(7);
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001:Invalid access token");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    int isLeader = challengeMapper.isLeader(challengeId, userId);
    if (isLeader == 0) {
      throw new RuntimeException("CHALLENGE_004:리더만 접근할 수 있습니다");
    }

    if (request.getName() != null) {
      String normalizedName = request.getName().trim();
      if (challengeMapper.countByNameExcludingId(normalizedName, challengeId) > 0) {
        throw new RuntimeException("CHALLENGE_011:이미 동일한 이름의 챌린지가 존재합니다");
      }
    }

    if (request.getMaxMembers() != null) {
      if (request.getMaxMembers() < challenge.getCurrentMembers()) {
        throw new RuntimeException("VALIDATION_001:Max members must be greater than or equal to current members(" + challenge.getCurrentMembers() + ")");
      }
      if (request.getMaxMembers() < challenge.getMaxMembers()) {
        throw new RuntimeException("VALIDATION_001:최대 인원은 기존 설정 값보다 작게 변경할 수 없습니다");
      }
    }

    if (request.getName() != null) {
      challenge.setName(request.getName());
    }
    if (request.getDescription() != null) {
      challenge.setDescription(request.getDescription());
    }
    if (request.getThumbnailImage() != null) {
      challenge.setThumbnailUrl(request.getThumbnailImage());
    }
    if (request.getBannerImage() != null) {
      challenge.setBannerUrl(request.getBannerImage());
    }
    if (request.getRules() != null) {
      challenge.setRules(request.getRules());
    }
    if (request.getMaxMembers() != null) {
      challenge.setMaxMembers(request.getMaxMembers());
    }

    challengeMapper.update(challenge);

    return UpdateChallengeResponse.builder()
        .challengeId(challenge.getId())
        .name(challenge.getName())
        .description(challenge.getDescription())
        .maxMembers(challenge.getMaxMembers())
        .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .message("Challenge updated successfully")
        .build();
  }

  /**
   * 현재 로그인 사용자가 속한 챌린지 목록과 요약 통계를 조회한다.
   */
  // [학습] 내가 속한 챌린지 목록을 조회한다.
  public MyChallengesResponse getMyChallenges(String accessToken, MyChallengesRequest request) {

    String userId = resolveUserId(accessToken);

    List<Map<String, Object>> myChallenges = challengeMapper.findMyChallenges(
        userId, request.getRole(), request.getStatus());

    List<MyChallengesResponse.MyChallengeItem> challengeItems = new ArrayList<>();
    int leaderCount = 0;
    int followerCount = 0;
    long totalMonthlySupport = 0;

    for (Map<String, Object> row : myChallenges) {
      String role = row.get("MY_ROLE") != null ? row.get("MY_ROLE").toString() : null;

      if ("LEADER".equals(role)) {
        leaderCount++;
      } else if ("FOLLOWER".equals(role)) {
        followerCount++;
      }

      Long supportAmount = row.get("SUPPORT_AMOUNT") != null ? ((Number) row.get("SUPPORT_AMOUNT")).longValue() : 0L;
      totalMonthlySupport += supportAmount;

      MyChallengesResponse.MyChallengeItem item = MyChallengesResponse.MyChallengeItem.builder()
          .challengeId(row.get("CHALLENGE_ID") != null ? row.get("CHALLENGE_ID").toString() : null)
          .memberId(row.get("MEMBER_ID") != null ? row.get("MEMBER_ID").toString() : null)
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
          .bannerImage(row.get("BANNER_IMAGE") != null ? row.get("BANNER_IMAGE").toString() : null)
          .build();

      challengeItems.add(item);
    }

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
   * 챌린지 계좌(잔액/원장/납부상태)를 조회한다.
   * 멤버만 조회 가능하다.
   */
  // [학습] 챌린지 계정(잔액/원장) 정보를 조회한다.
  public ChallengeAccountResponse getChallengeAccount(String challengeId, String accessToken) {

    String userId = resolveUserId(accessToken);

    Map<String, Object> accountData = challengeMapper.findChallengeAccount(challengeId);
    if (accountData == null) {
      throw new IllegalArgumentException("CHALLENGE_001");
    }

    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId);
    if (isMember == 0) {
      throw new SecurityException("CHALLENGE_003");
    }

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

    Long balance = getLong(accountData, "BALANCE");
    if (balance == null)
      balance = 0L;

    Long lockedDeposits = getLong(accountData, "LOCKED_DEPOSITS");
    if (lockedDeposits == null)
      lockedDeposits = 0L;

    Long totalIncome = getLong(accountData, "TOTAL_INCOME");
    if (totalIncome == null)
      totalIncome = 0L;

    Long totalExpense = getLong(accountData, "TOTAL_EXPENSE");
    if (totalExpense == null)
      totalExpense = 0L;

    Long monthlyFee = getLong(accountData, "MONTHLY_FEE");
    if (monthlyFee == null)
      monthlyFee = 0L;

    Integer currentMembers = getInteger(accountData, "CURRENT_MEMBERS");
    if (currentMembers == null)
      currentMembers = 0;

    ChallengeAccountResponse.Stats stats = ChallengeAccountResponse.Stats.builder()
        .totalSupport(totalIncome)
        .totalExpense(totalExpense)
        .totalFee(0L)
        .monthlyAverage(monthlyFee * currentMembers)
        .build();

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
        .availableBalance(balance)
        .stats(stats)
        .recentTransactions(transactions)
        .supportStatus(supportStatus)
        .build();
  }

  /**
   * 챌린지 장부 그래프(월별 소비/월말 잔액) 데이터를 조회한다.
   */
  @Transactional(readOnly = true)
  public ChallengeLedgerGraphResponse getChallengeLedgerGraph(String challengeId, String accessToken, Integer months) {
    String userId = resolveUserId(accessToken);

    Map<String, Object> accountData = challengeMapper.findChallengeAccount(challengeId);
    if (accountData == null) {
      throw new IllegalArgumentException("CHALLENGE_001");
    }

    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId);
    if (isMember == 0) {
      throw new SecurityException("CHALLENGE_003");
    }

    int normalizedMonths = normalizeGraphMonths(months);
    LocalDateTime startAt = LocalDate.now(KST_ZONE)
        .minusMonths(normalizedMonths - 1L)
        .withDayOfMonth(1)
        .atStartOfDay();

    List<Map<String, Object>> ledgerRows = challengeMapper.findLedgerEntriesForGraph(challengeId, startAt);
    List<DjangoLedgerGraphRequest.LedgerEntryItem> entries = new ArrayList<>();
    for (Map<String, Object> row : ledgerRows) {
      Long amount = getLong(row, "AMOUNT");
      Long balanceAfter = getLong(row, "BALANCE_AFTER");
      entries.add(DjangoLedgerGraphRequest.LedgerEntryItem.builder()
          .createdAt(formatTimestamp(row.get("CREATED_AT")))
          .type(getString(row, "TYPE"))
          .amount(amount != null ? amount : 0L)
          .balanceAfter(balanceAfter)
          .build());
    }

    Long currentBalance = getLong(accountData, "BALANCE");
    if (currentBalance == null) {
      currentBalance = 0L;
    }

    DjangoLedgerGraphRequest request = DjangoLedgerGraphRequest.builder()
        .challengeId(challengeId)
        .months(normalizedMonths)
        .currentBalance(currentBalance)
        .entries(entries)
        .build();

    try {
      DjangoLedgerGraphResponse djangoResponse = djangoLedgerClient.calculateGraph(request);
      return buildDjangoLedgerGraphResponse(challengeId, normalizedMonths, djangoResponse);
    } catch (RuntimeException e) {
      String errorCode = extractErrorCode(e.getMessage());
      if (!LEDGER_STATUS_NETWORK.equals(errorCode)) {
        throw e;
      }
      return buildFallbackLedgerGraphResponse(challengeId, normalizedMonths, request);
    }
  }

  private int normalizeGraphMonths(Integer months) {
    if (months == null) {
      return DEFAULT_GRAPH_MONTHS;
    }
    if (months < MIN_GRAPH_MONTHS) {
      return MIN_GRAPH_MONTHS;
    }
    if (months > MAX_GRAPH_MONTHS) {
      return MAX_GRAPH_MONTHS;
    }
    return months;
  }

  private ChallengeLedgerGraphResponse buildDjangoLedgerGraphResponse(
      String challengeId,
      int normalizedMonths,
      DjangoLedgerGraphResponse djangoResponse) {
    List<ChallengeLedgerGraphResponse.MonthlyExpense> monthlyExpenses = new ArrayList<>();
    if (djangoResponse.getMonthlyExpenses() != null) {
      for (DjangoLedgerGraphResponse.MonthlyExpense expense : djangoResponse.getMonthlyExpenses()) {
        if (expense == null) {
          continue;
        }
        monthlyExpenses.add(ChallengeLedgerGraphResponse.MonthlyExpense.builder()
            .month(expense.getMonth())
            .expense(expense.getExpense() != null ? expense.getExpense() : 0L)
            .build());
      }
    }

    List<ChallengeLedgerGraphResponse.MonthlyBalance> monthlyBalances = new ArrayList<>();
    if (djangoResponse.getMonthlyBalances() != null) {
      for (DjangoLedgerGraphResponse.MonthlyBalance balance : djangoResponse.getMonthlyBalances()) {
        if (balance == null) {
          continue;
        }
        monthlyBalances.add(ChallengeLedgerGraphResponse.MonthlyBalance.builder()
            .month(balance.getMonth())
            .balance(balance.getBalance())
            .build());
      }
    }

    return ChallengeLedgerGraphResponse.builder()
        .challengeId(challengeId)
        .months(normalizedMonths)
        .calculatedAt(djangoResponse.getCalculatedAt())
        .graphSource(GRAPH_SOURCE_DJANGO)
        .graphStatusCode(LEDGER_STATUS_OK)
        .monthlyExpenses(monthlyExpenses)
        .monthlyBalances(monthlyBalances)
        .build();
  }

  private ChallengeLedgerGraphResponse buildFallbackLedgerGraphResponse(
      String challengeId,
      int normalizedMonths,
      DjangoLedgerGraphRequest request) {
    List<String> monthKeys = buildMonthKeys(normalizedMonths);
    Map<String, Long> monthlyExpensesMap = new LinkedHashMap<>();
    Map<String, Long> monthEndBalanceMap = new LinkedHashMap<>();
    Map<String, LocalDateTime> monthEndSeenAt = new LinkedHashMap<>();

    for (String monthKey : monthKeys) {
      monthlyExpensesMap.put(monthKey, 0L);
      monthEndBalanceMap.put(monthKey, null);
      monthEndSeenAt.put(monthKey, null);
    }

    List<DjangoLedgerGraphRequest.LedgerEntryItem> entries = request.getEntries() != null
        ? request.getEntries()
        : List.of();

    for (DjangoLedgerGraphRequest.LedgerEntryItem entry : entries) {
      if (entry == null) {
        continue;
      }
      LocalDateTime createdAt = parseLedgerCreatedAt(entry.getCreatedAt());
      if (createdAt == null) {
        continue;
      }

      String monthKey = createdAt.format(YEAR_MONTH_FORMAT);
      if (!monthlyExpensesMap.containsKey(monthKey)) {
        continue;
      }

      String entryType = entry.getType() == null ? "" : entry.getType().trim().toUpperCase(Locale.ROOT);
      long amount = entry.getAmount() == null ? 0L : entry.getAmount();
      if ("EXPENSE".equals(entryType)) {
        monthlyExpensesMap.put(monthKey, monthlyExpensesMap.get(monthKey) + Math.abs(amount));
      }

      if (entry.getBalanceAfter() == null) {
        continue;
      }

      LocalDateTime previousSeenAt = monthEndSeenAt.get(monthKey);
      if (previousSeenAt == null || !createdAt.isBefore(previousSeenAt)) {
        monthEndBalanceMap.put(monthKey, entry.getBalanceAfter());
        monthEndSeenAt.put(monthKey, createdAt);
      }
    }

    long currentBalance = request.getCurrentBalance() != null ? request.getCurrentBalance() : 0L;
    String currentMonthKey = YearMonth.now(KST_ZONE).format(YEAR_MONTH_FORMAT);
    if (monthEndBalanceMap.containsKey(currentMonthKey)) {
      monthEndBalanceMap.put(currentMonthKey, currentBalance);
    }

    List<Long> filledBalances = new ArrayList<>(monthKeys.size());
    Long lastSeen = null;
    for (String monthKey : monthKeys) {
      Long current = monthEndBalanceMap.get(monthKey);
      if (current == null) {
        filledBalances.add(lastSeen);
      } else {
        lastSeen = current;
        filledBalances.add(current);
      }
    }
    if (!filledBalances.isEmpty()) {
      filledBalances.set(filledBalances.size() - 1, currentBalance);
    }

    List<ChallengeLedgerGraphResponse.MonthlyExpense> monthlyExpenses = new ArrayList<>(monthKeys.size());
    List<ChallengeLedgerGraphResponse.MonthlyBalance> monthlyBalances = new ArrayList<>(monthKeys.size());
    for (int index = 0; index < monthKeys.size(); index++) {
      String monthKey = monthKeys.get(index);
      monthlyExpenses.add(ChallengeLedgerGraphResponse.MonthlyExpense.builder()
          .month(monthKey)
          .expense(monthlyExpensesMap.getOrDefault(monthKey, 0L))
          .build());
      monthlyBalances.add(ChallengeLedgerGraphResponse.MonthlyBalance.builder()
          .month(monthKey)
          .balance(filledBalances.get(index))
          .build());
    }

    return ChallengeLedgerGraphResponse.builder()
        .challengeId(challengeId)
        .months(normalizedMonths)
        .calculatedAt(Instant.now().toString())
        .graphSource(GRAPH_SOURCE_JAVA_FALLBACK)
        .graphStatusCode(LEDGER_STATUS_NETWORK)
        .monthlyExpenses(monthlyExpenses)
        .monthlyBalances(monthlyBalances)
        .build();
  }

  private List<String> buildMonthKeys(int normalizedMonths) {
    YearMonth currentMonth = YearMonth.now(KST_ZONE);
    YearMonth firstMonth = currentMonth.minusMonths(normalizedMonths - 1L);
    List<String> monthKeys = new ArrayList<>(normalizedMonths);
    for (int index = 0; index < normalizedMonths; index++) {
      monthKeys.add(firstMonth.plusMonths(index).format(YEAR_MONTH_FORMAT));
    }
    return monthKeys;
  }

  private LocalDateTime parseLedgerCreatedAt(String createdAtValue) {
    if (createdAtValue == null || createdAtValue.isBlank()) {
      return null;
    }

    String normalized = createdAtValue.trim();
    if (normalized.endsWith("Z")) {
      try {
        return Instant.parse(normalized).atZone(KST_ZONE).toLocalDateTime();
      } catch (DateTimeParseException ignored) {
        // fall through
      }
    }

    try {
      return LocalDateTime.parse(normalized);
    } catch (DateTimeParseException ignored) {
      // fall through
    }

    try {
      return OffsetDateTime.parse(normalized).atZoneSameInstant(KST_ZONE).toLocalDateTime();
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  private String extractErrorCode(String message) {
    if (message == null || message.isBlank()) {
      return "";
    }
    int separatorIndex = message.indexOf(':');
    String code = separatorIndex >= 0 ? message.substring(0, separatorIndex) : message;
    return Objects.requireNonNullElse(code, "").trim();
  }

  /**
   * 챌린지 가입을 처리한다.
   * - 가입 가능 상태 검증
   * - 가입 비용(가입비/보증금/첫 후원) 검증 및 차감
   * - 멤버 등록(재가입 포함)
   * - 챌린지 잔액/원장 반영
   */
  @Transactional
  // [학습] 챌린지 가입 및 가입금/보증금/첫 후원을 처리한다.
  public JoinChallengeResponse joinChallenge(String challengeId, String accessToken) {

    String userId = resolveUserId(accessToken);

    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new IllegalArgumentException("CHALLENGE_001");
    }

    if (ChallengeStatus.RECRUITING != challenge.getStatus()
        && ChallengeStatus.IN_PROGRESS != challenge.getStatus()) {
      throw new IllegalStateException("CHALLENGE_006");
    }

    Map<String, Object> existingMember = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    boolean isRejoin = false;
    String existingMemberId = null;

    if (existingMember != null) {
      String status = (String) existingMember.get("STATUS");
      if ("ACTIVE".equals(status)) {
        throw new IllegalStateException("CHALLENGE_002");
      }
      isRejoin = true;
      existingMemberId = (String) existingMember.get("MEMBER_ID");
    }

    if (challenge.getCurrentMembers() >= challenge.getMaxMembers()) {
      throw new IllegalStateException("CHALLENGE_005");
    }

    Account account = accountMapper.findByUserId(userId);
    if (account == null) {
      throw new IllegalArgumentException("ACCOUNT_001");
    }

    Long deposit = challenge.getDepositAmount() != null ? challenge.getDepositAmount() : 0L;

    int followerCount = challenge.getCurrentMembers() - 1;
    if (followerCount < 1)
      followerCount = 1;
    Long entryFee = (challenge.getBalance() != null && challenge.getBalance() > 0)
        ? challenge.getBalance() / followerCount
        : 0L;
    Long firstSupport = 0L;
    Long totalCost = deposit + entryFee + firstSupport;

    if (account.getBalance() < totalCost) {
      throw new IllegalStateException("ACCOUNT_004");
    }

    Long balanceBefore = account.getBalance();
    Long lockedBefore = account.getLockedBalance();

    if (entryFee > 0) {
      account.setBalance(account.getBalance() - entryFee);
    }
    if (firstSupport > 0) {
      account.setBalance(account.getBalance() - firstSupport);
    }

    if (deposit > 0) {
      account.setBalance(account.getBalance() - deposit);
      account.setLockedBalance(account.getLockedBalance() + deposit);
    }

    int updateResult = accountMapper.update(account);
    if (updateResult == 0) {
      throw new RuntimeException("ACCOUNT_014:동시 요청으로 계좌 처리에 실패했습니다. 다시 시도해주세요.");
    }

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
          .description("챌린지 가입비 결제")
          .createdAt(LocalDateTime.now())
          .build();
      accountMapper.saveTransaction(entryFeeTx);
      balanceBefore -= entryFee;
    }

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
          .description("챌린지 첫 후원금 납부")
          .createdAt(LocalDateTime.now())
          .build();
      accountMapper.saveTransaction(supportTx);
      balanceBefore -= firstSupport;
    }

    if (deposit > 0) {
      AccountTransaction lockTx = AccountTransaction.builder()
          .id(UUID.randomUUID().toString())
          .accountId(account.getId())
          .type(TransactionType.LOCK)
          .amount(-deposit)
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

    String memberId = isRejoin ? existingMemberId : UUID.randomUUID().toString();
    ChallengeMember member = ChallengeMember.builder()
        .id(memberId)
        .challengeId(challengeId)
        .userId(userId)
        .role(ChallengeRole.FOLLOWER)
        .depositStatus(DepositStatus.LOCKED)
        .privilegeStatus(PrivilegeStatus.ACTIVE)
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

    int incResult = challengeMapper.incrementCurrentMembers(challengeId);
    if (incResult == 0) {
      throw new IllegalStateException("CHALLENGE_005"); // current_members 증가 실패
    }

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
        throw new RuntimeException("CHALLENGE_003:챌린지 정보 업데이트 충돌이 발생했습니다. 다시 시도해주세요");
      }

      if (entryFee > 0) {
        LedgerEntry ledger = LedgerEntry.builder()
            .id(UUID.randomUUID().toString())
            .challengeId(challengeId)
            .type(com.woorido.challenge.domain.LedgerEntryType.ENTRY_FEE)
            .amount(entryFee)
            .description("Challenge entry fee")
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
            .type(com.woorido.challenge.domain.LedgerEntryType.SUPPORT)
            .amount(firstSupport)
            .description("Challenge first support")
            .balanceBefore(chBalanceBefore)
            .balanceAfter(chBalanceBefore + firstSupport)
            .relatedUserId(userId)
            .createdAt(LocalDateTime.now())
            .build();
        ledgerMapper.insert(ledger);
      }
    }

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
        .message("챌린지 가입이 완료되었습니다")
        .build();
  }

  /**
   * 챌린지 탈퇴를 처리한다.
   * 리더는 탈퇴할 수 없으며, 보증금은 환급 처리한다.
   */
  @Transactional
  // [학습] 챌린지 탈퇴 및 보증금 환급을 처리한다.
  public LeaveChallengeResponse leaveChallenge(String challengeId, String accessToken) {

    String userId = resolveUserId(accessToken);

    int isLeader = challengeMapper.isLeader(challengeId, userId);
    if (isLeader > 0) {
      throw new RuntimeException("MEMBER_002:리더는 챌린지를 탈퇴할 수 없습니다");
    }

    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId);
    if (isMember == 0) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }

    Long deposit = challenge.getDepositAmount() != null ? challenge.getDepositAmount() : 0L;
    Long netRefund = deposit;

    Account account = accountMapper.findByUserId(userId);
    if (account == null) {
      throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
    }

    if (deposit > 0) {
      Long balanceBefore = account.getBalance();
      Long lockedBefore = account.getLockedBalance();

      account.setBalance(balanceBefore + netRefund);
      account.setLockedBalance(lockedBefore - deposit);

      int updateResult = accountMapper.update(account);
      if (updateResult == 0) {
        throw new RuntimeException("ACCOUNT_003:잔액 업데이트에 실패했습니다. 다시 시도해주세요");
      }

      AccountTransaction refundTx = AccountTransaction.builder()
          .id(UUID.randomUUID().toString())
          .accountId(account.getId())
          .type(TransactionType.REFUND)
          .amount(netRefund)
          .balanceBefore(balanceBefore)
          .balanceAfter(account.getBalance())
          .lockedBefore(lockedBefore)
          .lockedAfter(account.getLockedBalance())
          // .relatedChallengeId(challengeId) // If field exists
          .description("챌린지 탈퇴 보증금 환급")
          .createdAt(LocalDateTime.now())
          .build();
      accountMapper.saveTransaction(refundTx);
    }

    challengeMapper.decrementCurrentMembers(challengeId);

    challengeMemberMapper.leaveChallenge(challengeId, userId);

    LeaveChallengeResponse.Refund refund = LeaveChallengeResponse.Refund.builder()
        .deposit(deposit)
        .deducted(0L)
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
   * 챌린지 멤버 목록과 요약 통계를 조회한다.
   */
  // [학습] 챌린지 멤버 목록과 요약 통계를 조회한다.
  public ChallengeMemberListResponse getChallengeMembers(String challengeId, String accessToken, String filterStatus) {

    String requestUserId = resolveUserId(accessToken);

    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, requestUserId);
    if (isMember == 0) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }

    List<Map<String, Object>> membersData = challengeMemberMapper.findMembersWithUserInfo(challengeId, filterStatus);

    List<ChallengeMemberListResponse.MemberInfo> memberList = new ArrayList<>();
    int activeCount = 0;
    int overdueCount = 0;
    int graceCount = 0;
    int completedMeetings = meetingMapper.countCompletedMeetingsByChallengeId(challengeId);

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
          .brix(data.get("BRIX") != null ? Double.parseDouble(data.get("BRIX").toString()) : 12.0)
          .build();

      // Calculate real support status
      String thisMonthStatus = "UNPAID";
      int consecutivePaid = 0;

      Object lastPaidObj = data.get("LAST_SUPPORT_PAID_AT");
      if (lastPaidObj != null) {
        LocalDateTime lastPaidAt = null;
        if (lastPaidObj instanceof java.sql.Timestamp) {
          lastPaidAt = ((java.sql.Timestamp) lastPaidObj).toLocalDateTime();
        } else if (lastPaidObj instanceof LocalDateTime) {
          lastPaidAt = (LocalDateTime) lastPaidObj;
        }

        if (lastPaidAt != null) {
          String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
          String paidMonth = lastPaidAt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
          if (currentMonth.equals(paidMonth)) {
            thisMonthStatus = "PAID";
            consecutivePaid = 1;
          }
        }
      }

      ChallengeMemberListResponse.SupportStatus supportStatus = ChallengeMemberListResponse.SupportStatus.builder()
          .thisMonth(thisMonthStatus)
          .consecutivePaid(consecutivePaid)
          .overdueCount(0)
          .build();

      int attendedMeetings = meetingMapper.countAttendedCompletedMeetingsByChallengeIdAndUserId(
          challengeId,
          getString(data, "USER_ID"));
      double attendanceRate = completedMeetings > 0
          ? (double) attendedMeetings / completedMeetings * 100.0
          : 0.0;

      memberList.add(ChallengeMemberListResponse.MemberInfo.builder()
          .memberId((String) data.get("MEMBER_ID"))
          .user(userInfo)
          .role((String) data.get("ROLE"))
          .status(status)
          .supportStatus(supportStatus)
          .attendanceRate(attendanceRate)
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

  /**
   * 모집 중인 챌린지를 삭제(soft delete) 상태로 전환한다.
   */
  @Transactional
  // [학습] 챌린지를 삭제 상태로 전환한다.
  public ChallengeDeleteResponse deleteChallenge(String accessToken,
      String challengeId) {
    String userId = resolveUserId(accessToken);

    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    int isLeader = challengeMapper.isLeader(challengeId, userId);
    if (isLeader == 0) {
      throw new RuntimeException("CHALLENGE_004:리더만 접근할 수 있습니다");
    }

    if (ChallengeStatus.RECRUITING != challenge.getStatus()) {
      throw new RuntimeException("CHALLENGE_010:모집 중 상태의 챌린지만 삭제할 수 있습니다");
    }

    challenge.setStatus(ChallengeStatus.COMPLETED);
    challenge.setDeletedAt(LocalDateTime.now());

    challengeMapper.updateStatusAndDeletedAt(challenge);

    return ChallengeDeleteResponse.builder()
        .challengeId(challengeId)
        .deleted(true)
        .build();
  }

  /**
   * 챌린지 멤버의 자동 납입 설정을 변경한다.
   */
  @Transactional
  // [학습] 자동 납입 설정을 변경한다.
  public UpdateSupportSettingsResponse updateSupportSettings(String challengeId,
      String accessToken, UpdateSupportSettingsRequest request) {
    String userId = resolveUserId(accessToken);

    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    Map<String, Object> membership = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (membership == null || !"ACTIVE".equals(getString(membership, "STATUS"))) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }

    String autoPayValue = request.getAutoPayEnabled() ? "Y" : "N";
    int result = challengeMemberMapper.updateAutoPayEnabled(userId, challengeId, autoPayValue);
    if (result == 0) {
      throw new RuntimeException("CHALLENGE_014:자동 납입 설정 업데이트에 실패했습니다");
    }

    LocalDate nextDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);

    return UpdateSupportSettingsResponse.builder()
        .challengeId(challengeId)
        .autoPayEnabled(request.getAutoPayEnabled())
        .nextPaymentDate(nextDate.toString())
        .amount(challenge.getMonthlyFee())
        .build();
  }

  /**
   * 특정 멤버의 상세 통계/후원 이력을 조회한다.
   */
  // [학습] 특정 멤버의 상세 통계 정보를 조회한다.
  public com.woorido.challenge.dto.response.ChallengeMemberDetailResponse getMemberDetail(String challengeId,
      String memberId, String accessToken) {
    String requestUserId = resolveUserId(accessToken);

    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, requestUserId);
    if (isMember == 0) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }

    Map<String, Object> memberData = challengeMemberMapper.findMemberDetail(challengeId, memberId);

    if (memberData == null) {
      throw new RuntimeException("MEMBER_001:멤버 정보를 찾을 수 없습니다");
    }

    String targetUserId = (String) memberData.get("USER_ID");
    Long totalSupportPaid = memberData.get("TOTAL_SUPPORT_PAID") != null
        ? Long.parseLong(memberData.get("TOTAL_SUPPORT_PAID").toString())
        : 0L;

    int meetingsTotal = meetingMapper.countCompletedMeetingsByChallengeId(challengeId);
    int meetingsAttended = meetingMapper.countAttendedCompletedMeetingsByChallengeIdAndUserId(challengeId, targetUserId);
    Double attendanceRate = meetingsTotal > 0 ? (double) meetingsAttended / meetingsTotal * 100.0 : 0.0;

    Double supportRate = totalSupportPaid > 0 ? 100.0 : 0.0;

    com.woorido.challenge.dto.response.ChallengeMemberDetailResponse.Stats stats = com.woorido.challenge.dto.response.ChallengeMemberDetailResponse.Stats
        .builder()
        .totalSupport(totalSupportPaid)
        .supportRate(supportRate)
        .attendanceRate(attendanceRate)
        .meetingsAttended(meetingsAttended)
        .meetingsTotal(meetingsTotal)
        .build();

    List<LedgerEntry> ledgerEntries = ledgerMapper.findSupportHistory(challengeId, targetUserId);

    List<com.woorido.challenge.dto.response.ChallengeMemberDetailResponse.SupportHistory> supportHistory = new ArrayList<>();

    for (LedgerEntry entry : ledgerEntries) {
      String paidAt = entry.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      String month = entry.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM"));

      supportHistory.add(com.woorido.challenge.dto.response.ChallengeMemberDetailResponse.SupportHistory.builder()
          .month(month)
          .amount(entry.getAmount())
          .paidAt(paidAt)
          .build());
    }

    com.woorido.challenge.dto.response.ChallengeMemberDetailResponse.UserInfo userInfo = com.woorido.challenge.dto.response.ChallengeMemberDetailResponse.UserInfo
        .builder()
        .userId(targetUserId)
        .nickname((String) memberData.get("NICKNAME"))
        .profileImage((String) memberData.get("PROFILE_IMAGE"))
        .brix(memberData.get("BRIX") != null ? Double.parseDouble(memberData.get("BRIX").toString()) : 12.0)
        .build();

    return com.woorido.challenge.dto.response.ChallengeMemberDetailResponse.builder()
        .memberId(memberId)
        .user(userInfo)
        .role((String) memberData.get("ROLE"))
        .status((String) memberData.get("STATUS"))
        .stats(stats)
        .supportHistory(supportHistory)
        .joinedAt(memberData.get("JOINED_AT") != null ? memberData.get("JOINED_AT").toString() : null)
        .build();
  }

  // ------------------------------------------------------------------------------------------------
  // ------------------------------------------------------------------------------------------------
  @Transactional
  // [학습] 토큰 기반으로 리더 위임을 수행한다.
  public DelegateLeaderResponse delegateLeaderWithToken(String challengeId, String token, String targetUserId) {
    String userId = resolveUserId(token);
    return delegateLeader(challengeId, userId, targetUserId);
  }

  @Transactional
  // [학습] 현재 리더를 다른 멤버에게 위임한다.
  public DelegateLeaderResponse delegateLeader(String challengeId, String userId,
      String targetUserId) {
    Map<String, Object> myMemberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);


    if (myMemberInfo == null ||
        (!"LEADER".equals(myMemberInfo.get("ROLE")) && !"LEADER".equals(myMemberInfo.get("role")))) {
      throw new RuntimeException("CHALLENGE_004:리더만 리더 위임을 수행할 수 있습니다");
    }
    String myMemberId = (String) myMemberInfo.get("MEMBER_ID");
    if (myMemberId == null)
      myMemberId = (String) myMemberInfo.get("member_id"); // Fallback

    if (userId.equals(targetUserId)) {
      throw new RuntimeException("VALIDATION_001:본인에게는 리더를 위임할 수 없습니다");
    }

    Map<String, Object> targetMemberInfo = challengeMemberMapper.findByUserIdAndChallengeId(targetUserId, challengeId);
    if (targetMemberInfo == null) {
      throw new RuntimeException("MEMBER_001:대상 멤버 정보를 찾을 수 없습니다");
    }
    String targetMemberId = (String) targetMemberInfo.get("MEMBER_ID");
    if (targetMemberId == null)
      targetMemberId = (String) targetMemberInfo.get("member_id");

    com.woorido.challenge.domain.ChallengeMember targetMember = challengeMemberMapper.findById(targetMemberId);
    if (targetMember == null) {
      throw new RuntimeException("MEMBER_001:대상 멤버 상세 정보를 찾을 수 없습니다");
    }
    if (PrivilegeStatus.ACTIVE != targetMember.getPrivilegeStatus()) {
      throw new RuntimeException("MEMBER_001:ACTIVE 상태의 멤버에게만 리더를 위임할 수 있습니다");
    }

    int count1 = challengeMemberMapper.updateRole("FOLLOWER", myMemberId, challengeId);
    if (count1 == 0) {
      throw new RuntimeException("CHALLENGE_014:리더 권한 이관 처리에 실패했습니다");
    }

    int count2 = challengeMemberMapper.updateRole("LEADER", targetMemberId, challengeId);
    if (count2 == 0) {
      throw new RuntimeException("CHALLENGE_014:리더 권한 이관 처리에 실패했습니다");
    }

    Map<String, Object> myDetail = challengeMemberMapper.findMemberDetail(challengeId, myMemberId);
    Map<String, Object> targetDetail = challengeMemberMapper.findMemberDetail(challengeId, targetMemberId);

    DelegateLeaderResponse.MemberInfo prevLeaderInfo = DelegateLeaderResponse.MemberInfo
        .builder()
        .memberId(myMemberId)
        .userId(userId)
        .nickname((String) myDetail.get("NICKNAME"))
        .newRole("FOLLOWER")
        .build();

    DelegateLeaderResponse.MemberInfo newLeaderInfo = DelegateLeaderResponse.MemberInfo
        .builder()
        .memberId(targetMemberId)
        .userId(targetMember.getUserId())
        .nickname((String) targetDetail.get("NICKNAME"))
        .newRole("LEADER")
        .build();

    return DelegateLeaderResponse.builder()
        .challengeId(challengeId)
        .previousLeader(prevLeaderInfo)
        .newLeader(newLeaderInfo)
        .delegatedAt(java.time.LocalDateTime.now().toString())
        .build();
  }

  /**
   * 챌린지를 해산 처리한다.
   * - 잔액이 있으면 원장에 EXPENSE로 기록 후 0으로 정리
   * - 챌린지 상태를 COMPLETED로 변경
   * - 활성 멤버를 탈퇴 처리
   */
  @org.springframework.transaction.annotation.Transactional
  // [학습] 챌린지를 해산하고 멤버 상태를 정리한다.
  public void dissolveChallenge(String challengeId) {
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null)
      return;

    Long balance = challenge.getBalance() != null ? challenge.getBalance() : 0L;

    if (balance > 0) {
      LedgerEntry ledgerEntry = LedgerEntry.builder()
          .id(java.util.UUID.randomUUID().toString())
          .challengeId(challengeId)
          .type(com.woorido.challenge.domain.LedgerEntryType.EXPENSE)
          .amount(-balance)
          // 해산 시 남은 잔액을 지출로 기록해 원장 합계가 맞도록 맞춘다.
          .balanceBefore(balance)
          .balanceAfter(0L)
          .description("Challenge dissolved - remaining balance")
          .createdAt(LocalDateTime.now())
          .build();
      ledgerMapper.insert(ledgerEntry);

      challenge.setBalance(0L);
    }

    challenge.setStatus(ChallengeStatus.COMPLETED);
    challenge.setDeletedAt(LocalDateTime.now());
    challengeMapper.updateStatusAndDeletedAt(challenge);

    // Update balance if changed
    if (balance > 0) {
      challengeMapper.updateBalance(challenge);
    }

    List<Map<String, Object>> members = challengeMemberMapper.findAllActiveMembers(challengeId);
    for (Map<String, Object> member : members) {
      String userId = (String) member.get("USER_ID");
      challengeMemberMapper.leaveChallenge(challengeId, userId);
    }
  }

  /**
   * 연체 발생 시 보증금에서 월 후원금을 자동 차감한다.
   *
   * @return 실제 차감이 발생하면 true, 조건 미충족이면 false
   */
  @org.springframework.transaction.annotation.Transactional
  // [학습] 연체된 후원금을 보증금에서 자동 차감한다.
  public boolean autoDeductFromDeposit(String challengeId, String userId) {
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null)
      return false;

    Account account = accountMapper.findByUserId(userId);
    if (account == null)
      return false;

    Long monthlyFee = challenge.getMonthlyFee() != null ? challenge.getMonthlyFee() : 0L;
    if (monthlyFee <= 0)
      return false;

    if (account.getBalance() >= monthlyFee) {
      return false;
    }

    Long lockedBalance = account.getLockedBalance();
    if (lockedBalance < monthlyFee) {
      return false;
    }

    long balanceBefore = account.getBalance();
    long lockedBefore = lockedBalance;

    account.setLockedBalance(lockedBalance - monthlyFee);
    accountMapper.update(account);

    AccountTransaction tx = AccountTransaction.builder()
        .id(java.util.UUID.randomUUID().toString())
        .accountId(account.getId())
        .type(TransactionType.SUPPORT)
        .amount(-monthlyFee)
        .balanceBefore(balanceBefore)
        .balanceAfter(balanceBefore)
        .lockedBefore(lockedBefore)
        .lockedAfter(lockedBefore - monthlyFee)
        .relatedChallengeId(challengeId)
          .description("챌린지 월 후원금 납부")
        .createdAt(LocalDateTime.now())
        .build();
    accountMapper.saveTransaction(tx);

    Long chBalance = challenge.getBalance() != null ? challenge.getBalance() : 0L;
    challenge.setBalance(chBalance + monthlyFee);
    challengeMapper.updateBalance(challenge);

    challengeMemberMapper.updateDepositStatus(challengeId, userId, "USED");
    challengeMemberMapper.updatePrivilegeStatus(challengeId, userId, "REVOKED");

    return true;
  }
}
