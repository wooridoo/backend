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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeService {
  // Learning note:
  // - Read flow as: validate auth/role -> execute domain logic -> persist via Mapper.

  private static final int MAX_LEADER_CHALLENGES = 3;

  private final ChallengeMapper challengeMapper;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final AccountMapper accountMapper;
  private final AccountTransactionFactory accountTransactionFactory;
  private final JwtUtil jwtUtil;
  private final LedgerMapper ledgerMapper;

  /**
   * ???????? ???꾩룆???(API 022)
   */
  @Transactional
  // [학습] 챌린지를 생성하고 리더 멤버를 등록한다.
  public CreateChallengeResponse createChallenge(String accessToken, CreateChallengeRequest request) {

    // 1. ????ｋ???嚥▲굧???????????ID ???ㅻ쿋驪??
    String token = extractToken(accessToken);
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001:Invalid access token");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. ??잙갭큔筌?????????? ??嶺뚮㉡????癲ル슢캉????(?꿔꺂????쭍? 3??
    int leaderCount = challengeMapper.countLeaderChallenges(userId);
    if (leaderCount >= MAX_LEADER_CHALLENGES) {
      throw new RuntimeException("CHALLENGE_007:리더는 동시에 최대 3개의 챌린지만 생성할 수 있습니다");
    }

    // 3. ????ъ군????嚥▲굧????    validateRequest(request);

    // 4. ??????????嚥싳쉶瑗??꾧틚???癲ル슢캉????
    String normalizedName = request.getName().trim();
    if (challengeMapper.countByName(normalizedName) > 0) {
      throw new RuntimeException("CHALLENGE_011:이미 동일한 이름의 챌린지가 존재합니다");
    }

    // 5. ???????? ???꾩룆???
    LocalDateTime now = LocalDateTime.now();
    String challengeId = UUID.randomUUID().toString();
    Challenge challenge = Challenge.builder()
        .id(challengeId)
        .name(normalizedName)
        .description(request.getDescription())
        .category(ChallengeCategory.valueOf(request.getCategory()))
        .creatorId(userId)
        .currentMembers(1) // ??잙갭큔筌??????
        .minMembers(3)
        .maxMembers(request.getMaxMembers())
        .balance(0L)
        .monthlyFee(request.getSupportAmount())
        .depositAmount(request.getDepositAmount())
        .status(ChallengeStatus.RECRUITING)
        .thumbnailUrl(request.getThumbnailImage())
        .build();

    challengeMapper.insert(challenge);

    // 6. ???????? ?꿔꺂????蹂λ?????꾩룆???(??잙갭큔筌??
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

    // 7. ??⑤슢???節띾짆??룸쮤?????ャ렑???꿔꺂??節뉖き??
    if (request.getDepositAmount() > 0) {
      lockDeposit(userId, challengeId, request.getDepositAmount());
    }

    // 8. ??????????꾩룆???
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
   * Authorization ????諛몄??????Bearer ????ｋ?????ㅻ쿋驪??
   */
  // [학습] Authorization 헤더에서 Bearer 토큰을 추출한다.
  private String extractToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001:Authorization header is required");
    }
    return authorization.substring(7);
  }

  /**
   * ???됰Ŋ???????ъ군????嚥▲굧????
   */
  // [학습] 챌린지 생성 요청값의 정책을 검증한다.
  private void validateRequest(CreateChallengeRequest request) {
    // supportAmount??10,000??????숈춹??????
    if (request.getSupportAmount() % 10000 != 0) {
      throw new RuntimeException("VALIDATION_001:Support amount must be in units of 10000");
    }

    // depositAmount??supportAmount?? ??醫딆┻??貫?????
    if (!request.getDepositAmount().equals(request.getSupportAmount())) {
      throw new RuntimeException("VALIDATION_001:Deposit amount must match support amount");
    }

    // startDate??7?????????壤?
    LocalDate startDate = LocalDate.parse(request.getStartDate());
    LocalDate minStartDate = LocalDate.now().plusDays(7);
    if (startDate.isBefore(minStartDate)) {
      throw new RuntimeException("VALIDATION_001:Start date must be at least 7 days later");
    }
  }

  /**
   * ??⑤슢???節띾짆??룸쮤?????ャ렑???꿔꺂??節뉖き??
   */
  // [학습] 가입 보증금을 계좌에서 잠금 처리한다.
  private void lockDeposit(String userId, String challengeId, Long depositAmount) {
    Account account = accountMapper.findByUserId(userId);
    if (account == null) {
      throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
    }

    // ???됰Ŋ?좂춯??癲ル슢캉????
    if (account.getBalance() < depositAmount) {
      throw new RuntimeException("ACCOUNT_002:잔액이 부족합니다");
    }

    // ????⑥쥓猷??????
    Long balanceBefore = account.getBalance();
    Long lockedBefore = account.getLockedBalance();

    // ???됰Ŋ?좂춯???⑤슢堉?????????ャ렑??
    account.setBalance(balanceBefore - depositAmount);
    account.setLockedBalance(lockedBefore + depositAmount);

    // ?????????????????욍걛???ш끽維??
    int updated = accountMapper.update(account);
    if (updated == 0) {
      throw new RuntimeException("ACCOUNT_003:잔액 업데이트에 실패했습니다. 다시 시도해주세요");
    }

    // ?癲ル슢??????????뚯????덈춣?
    // ?癲ル슢??????????뚯????덈춣?
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
   * ???????? ?꿔꺂??袁ㅻ븶筌믠뫀萸???됰슦????(API 023)
   */
  @Transactional(readOnly = true)
  // [학습] 챌린지 목록을 필터/정렬 조건으로 조회한다.
  public ChallengeListResponse getChallengeList(ChallengeListRequest request) {

    // 1. ?꿔꺂??袁ㅻ븶筌믠뫀萸???됰슦????
    List<Map<String, Object>> challenges = challengeMapper.findAllWithFilter(
        request.getStatus(),
        request.getCategory(),
        request.getSortField(),
        request.getSortDirection(),
        request.getOffset(),
        request.getSize());

    // 2. ????醫딆┻?????됰슦????
    long totalElements = challengeMapper.countAllWithFilter(
        request.getStatus(),
        request.getCategory());

    // 3. ?嚥▲굧??????⑤슢堉???
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
          .isVerified("Y".equals(getString(row, "IS_VERIFIED")))
          .leader(ChallengeListResponse.Leader.builder()
              .userId(getString(row, "LEADER_USER_ID"))
              .nickname(getString(row, "LEADER_NICKNAME"))
              .build())
          .createdAt(formatTimestamp(row.get("CREATED_AT")))
          .build();
      content.add(item);
    }

    // 4. ????볥궙?袁р뵾???? ?癲ル슢???ъ쒜???影??낟??
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
   * ???????? ????노듋????됰슦????(API 024)
   */
  @Transactional(readOnly = true)
  // [학습] 챌린지 상세 정보를 조회한다.
  public ChallengeDetailResponse getChallengeDetail(String challengeId, String accessToken) {

    // 1. ???????? ????노듋????됰슦????
    Map<String, Object> challenge = challengeMapper.findDetailById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    // 2. ????ｋ???????????ID ???ㅻ쿋驪??(????ｋ???
    String userId = null;
    Boolean isMember = false;
    ChallengeDetailResponse.MyMembership myMembership = null;

    if (accessToken != null && accessToken.startsWith("Bearer ")) {
      try {
        String token = accessToken.substring(7);
        if (jwtUtil.validateToken(token)) {
          userId = jwtUtil.getUserIdFromToken(token);

          // 3. ??????꿔꺂????蹂λ?????됰슦????
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
        // ????ｋ???嚥▲굧?????????곌숯?????????嶺뚮ㅏ諭???꿔꺂??節뉖き??
        // ????ｋ???嚥▲굧?????????곌숯?????????嶺뚮ㅏ諭???꿔꺂??節뉖き??
      }
    }

    // 4. ??????????꾩룆???
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
   * ???????? ????볥궚??(API 025)
   */
  @Transactional
  // [학습] 리더 권한으로 챌린지 정보를 수정한다.
  public UpdateChallengeResponse updateChallenge(String challengeId, String accessToken,
      UpdateChallengeRequest request) {

    // 1. ????ｋ???嚥▲굧???????????ID ???ㅻ쿋驪??
    if (accessToken == null || !accessToken.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001:Authorization header is required");
    }
    String token = accessToken.substring(7);
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001:Invalid access token");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. ???????? ??됰슦????
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    // 3. ??잙갭큔筌??????????癲ル슢캉????
    int isLeader = challengeMapper.isLeader(challengeId, userId);
    if (isLeader == 0) {
      throw new RuntimeException("CHALLENGE_004:리더만 접근할 수 있습니다");
    }

    // 4. ??????????嚥싳쉶瑗??꾧틚???癲ル슢캉????(???????⑤슢堉?????
    if (request.getName() != null) {
      String normalizedName = request.getName().trim();
      if (challengeMapper.countByNameExcludingId(normalizedName, challengeId) > 0) {
        throw new RuntimeException("CHALLENGE_011:이미 동일한 이름의 챌린지가 존재합니다");
      }
    }

    // 5. maxMembers ?嚥▲굧????(????썹땟???癲ル슢?????????壤? ?꿔꺂?ｉ뜮?뚮쑏?????醫딆쓧???
    if (request.getMaxMembers() != null) {
      if (request.getMaxMembers() < challenge.getCurrentMembers()) {
        throw new RuntimeException("VALIDATION_001:Max members must be greater than or equal to current members(" + challenge.getCurrentMembers() + ")");
      }
      if (request.getMaxMembers() < challenge.getMaxMembers()) {
        throw new RuntimeException("VALIDATION_001:최대 인원은 기존 설정 값보다 작게 변경할 수 없습니다");
      }
    }

    // 6. ????볥궚???????썹땟??????繹먮냱??(null??????썹땟?????醫딆┫?傭??????욍걛???ш끽維??
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

    // 7. ?????욍걛???ш끽維???????덊떀
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
   * API 027: ?????????? ?꿔꺂??袁ㅻ븶筌믠뫀萸???됰슦????
   */
  // [학습] 내가 속한 챌린지 목록을 조회한다.
  public MyChallengesResponse getMyChallenges(String accessToken, MyChallengesRequest request) {

    // 1. ????ｋ???嚥▲굧???????????ID ???ㅻ쿋驪??
    String token = accessToken.replace("Bearer ", "");
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. ?????????? ?꿔꺂??袁ㅻ븶筌믠뫀萸???됰슦????
    List<Map<String, Object>> myChallenges = challengeMapper.findMyChallenges(
        userId, request.getRole(), request.getStatus());

    // 3. ??????????????????⑤슢堉???
    List<MyChallengesResponse.MyChallengeItem> challengeItems = new ArrayList<>();
    int leaderCount = 0;
    int followerCount = 0;
    long totalMonthlySupport = 0;

    for (Map<String, Object> row : myChallenges) {
      String role = row.get("MY_ROLE") != null ? row.get("MY_ROLE").toString() : null;

      // Summary ??影??낟??
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
          .build();

      challengeItems.add(item);
    }

    // 4. Summary ???꾩룆???
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
   * API 028: ???????? ????ㅿ폎????ш끽維????됰슦????
   */
  // [학습] 챌린지 계정(잔액/원장) 정보를 조회한다.
  public ChallengeAccountResponse getChallengeAccount(String challengeId, String accessToken) {

    // 1. ????ｋ???嚥▲굧???????????ID ???ㅻ쿋驪??
    String token = accessToken.replace("Bearer ", "");
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. ???????? ??됰슦?????癲ル슢캉????
    Map<String, Object> accountData = challengeMapper.findChallengeAccount(challengeId);
    if (accountData == null) {
      throw new IllegalArgumentException("CHALLENGE_001");
    }

    // 3. ?꿔꺂????蹂λ????? ?癲ル슢캉????
    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId);
    if (isMember == 0) {
      throw new SecurityException("CHALLENGE_003");
    }

    // 4. ?꿔꺂????쭍???꿸쑨?????????ㅿ폎????됰슦????
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

    // 5. ???됰Ŋ?좂춯??癲ル슢???ъ쒜????ㅻ쿋驪??
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

    // 6. Stats ??影??낟??
    ChallengeAccountResponse.Stats stats = ChallengeAccountResponse.Stats.builder()
        .totalSupport(totalIncome)
        .totalExpense(totalExpense)
        .totalFee(0L) // ??嶺뚮슣?쒒뜮??猷몄굣?????⑤슢???????影??낟??????썹땟??
        .monthlyAverage(monthlyFee * currentMembers)
        .build();

    // 7. SupportStatus (???繹먮냱議???? ????꾣뤃????影??낟??
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
        .availableBalance(balance) // ??????醫딆쓧??????됰Ŋ?좂춯?= ???됰Ŋ?좂춯?
        .stats(stats)
        .recentTransactions(transactions)
        .supportStatus(supportStatus)
        .build();
  }

  /**
   * API 030: ???????? ??醫딆쓧???
   */
  @Transactional
  // [학습] 챌린지 가입 및 가입금/보증금/첫 후원을 처리한다.
  public JoinChallengeResponse joinChallenge(String challengeId, String accessToken) {

    // 1. ????ｋ???嚥▲굧???????????ID ???ㅻ쿋驪??
    String token = accessToken.replace("Bearer ", "");
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. ???????? ??됰슦?????癲ル슢캉????
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new IllegalArgumentException("CHALLENGE_001");
    }

    // 3. ?꿔꺂??袁ㅻ븶?ⓥ뫚留?嚥싳쉶瑗??꾧틡???????????癲? ?癲ル슢캉????
    if (ChallengeStatus.RECRUITING != challenge.getStatus()) {
      throw new IllegalStateException("CHALLENGE_006");
    }

    // 4. ?꿔꺂????蹂λ???????븐뻤???癲ル슢캉????(????ャ렑?????????????醫딆쓧??????곗뒩泳??
    Map<String, Object> existingMember = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    boolean isRejoin = false;
    String existingMemberId = null;

    if (existingMember != null) {
      String status = (String) existingMember.get("STATUS");
      if ("ACTIVE".equals(status)) {
        throw new IllegalStateException("CHALLENGE_002");
      }
      // ?????낅뻘 ????븐뻤??쒖뱽????????꿔꺂????紐꾩뗄?
      isRejoin = true;
      existingMemberId = (String) existingMember.get("MEMBER_ID");
    }

    // 5. ?癲ル슢캉????潁????癲ル슢캉????
    if (challenge.getCurrentMembers() >= challenge.getMaxMembers()) {
      throw new IllegalStateException("CHALLENGE_005");
    }

    // 6. ???????影??낟渦???됰슦????
    Account account = accountMapper.findByUserId(userId);
    if (account == null) {
      throw new IllegalArgumentException("ACCOUNT_001");
    }

    // 7. ???????影??낟??
    Long deposit = challenge.getDepositAmount() != null ? challenge.getDepositAmount() : 0L;

    // ?????レ???= ???????? ???됰Ŋ?좂춯?/ (?꿔꺂????蹂λ???- 1) = ???됰Ŧ?뤻툣??????????낇뀘????繹먮굝鍮?
    // ??잙갭큔筌????筌??節꾪렭癰?鍮???蹂κ텥???熬곣뫖利?猷몃뢾??????살깓???????????????잙갭큔筌????嶺뚮????
    int followerCount = challenge.getCurrentMembers() - 1; // ??잙갭큔筌????嶺뚮????
    if (followerCount < 1)
      followerCount = 1; // 0 ?熬곣뫖?삥납? (????醫딆쓧?????⑤㈇猿?
    Long entryFee = (challenge.getBalance() != null && challenge.getBalance() > 0)
        ? challenge.getBalance() / followerCount
        : 0L;
    Long firstSupport = 0L; // ????????7?????????寃??욱맪????????猷??????썹땟??(??嶺뚮ㅎ???
    Long totalCost = deposit + entryFee + firstSupport;

    // 8. ???됰Ŋ?좂춯??癲ル슢캉????
    if (account.getBalance() < totalCost) {
      throw new IllegalStateException("ACCOUNT_004");
    }

    // 9. ???됰Ŋ?좂춯??꿔꺂?볟젆怨ㅼ춻??뮻?????⑤슢???節띾짆??룸쮤?????ャ렑??
    Long balanceBefore = account.getBalance();
    Long lockedBefore = account.getLockedBalance();

    // ??????꿔꺂?볟젆怨ㅼ춻??뮻?(??醫딆쓧??????됰Ŋ?좂춯??????꿔꺂?볟젆怨ㅼ춻??뮻?
    if (entryFee > 0) {
      account.setBalance(account.getBalance() - entryFee);
    }
    if (firstSupport > 0) {
      account.setBalance(account.getBalance() - firstSupport);
    }

    // ??⑤슢???節띾짆??룸쮤?????ャ렑??(??醫딆쓧??????됰Ŋ?좂춯??꿔꺂?볟젆怨ㅼ춻??뮻???????ャ렑????꿔꺂?ｉ뜮?뚮쑏?)
    if (deposit > 0) {
      account.setBalance(account.getBalance() - deposit);
      account.setLockedBalance(account.getLockedBalance() + deposit);
    }

    int updateResult = accountMapper.update(account);
    if (updateResult == 0) {
      throw new RuntimeException("Failed to update account balance");
    }

    // Transaction ???뚯????덈춣?
    // 9-1. ????⑤㈇???(ENTRY_FEE)
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
      balanceBefore -= entryFee; // ???繹먮굞???癲ル슢????????????꾣뤃????醫딆┣???
    }

    // 9-2. ???????猷??(SUPPORT)
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

    // 9-3. ??⑤슢???節띾짆??룸쮤?????ャ렑??(LOCK)
    if (deposit > 0) {
      AccountTransaction lockTx = AccountTransaction.builder()
          .id(UUID.randomUUID().toString())
          .accountId(account.getId())
          .type(TransactionType.LOCK)
          .amount(-deposit) // ??醫딆쓧???????????醫딆┫???
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

    // 10. ???????? ?꿔꺂????蹂λ???嚥싲갭큔?댁쉩??
    // 10. ???????? ?꿔꺂????蹂λ???嚥싲갭큔?댁쉩??
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

    // 11. ???????? ?꿔꺂????蹂λ?????꿔꺂?ｉ뜮?뚮쑏?
    int incResult = challengeMapper.incrementCurrentMembers(challengeId);
    if (incResult == 0) {
      throw new IllegalStateException("CHALLENGE_005"); // current_members 증가 실패
    }

    // [NEW] 12. ???????? ?熬곣뫖利당춯??????逆?(Ledger) ?????욍걛???ш끽維??
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

    // 13. ??????????꾩룆???
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
   * API 031: ???????? ?????낅뻘
   */
  @Transactional
  // [학습] 챌린지 탈퇴 및 보증금 환급을 처리한다.
  public LeaveChallengeResponse leaveChallenge(String challengeId, String accessToken) {

    // 1. ????ｋ???嚥▲굧???????????ID ???ㅻ쿋驪??(Bearer ???곌퇈?뗦틦?
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. ??잙갭큔筌??????????癲ル슢캉????
    int isLeader = challengeMapper.isLeader(challengeId, userId);
    if (isLeader > 0) {
      throw new RuntimeException("MEMBER_002:리더는 챌린지를 탈퇴할 수 없습니다");
    }

    // 3. ???????? ??됰슦??????? ?癲ル슢캉????
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    // 4. ?꿔꺂????蹂λ????? ?癲ル슢캉????
    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId);
    if (isMember == 0) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }

    // 5. ???β넄??????궰????덈????影??낟??
    Long deposit = challenge.getDepositAmount() != null ? challenge.getDepositAmount() : 0L;
    Long netRefund = deposit; // ?꿔꺂?볟젆怨ㅼ춻??뮻?????ㅼ굡????醫딆쓧???

    // 6. ???????影??낟渦????β넄????꿔꺂??節뉖き??
    Account account = accountMapper.findByUserId(userId);
    if (account == null) {
      throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
    }

    if (deposit > 0) {
      Long balanceBefore = account.getBalance();
      Long lockedBefore = account.getLockedBalance();

      // ???됰Ŋ?좂춯??꿔꺂?ｉ뜮?뚮쑏?, ????ャ렑?????醫딆┫???
      account.setBalance(balanceBefore + netRefund);
      account.setLockedBalance(lockedBefore - deposit);

      int updateResult = accountMapper.update(account);
      if (updateResult == 0) {
        throw new RuntimeException("ACCOUNT_003:잔액 업데이트에 실패했습니다. 다시 시도해주세요");
      }

      // 7. Transaction ???뚯????덈춣?(REFUND)
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

    // 8. ???????? ?꿔꺂????蹂λ??????醫딆┫???(Optional, trigger might handle it)
    challengeMapper.decrementCurrentMembers(challengeId);

    // 9. ?????낅뻘 ?꿔꺂??節뉖き??(Soft Delete)
    challengeMemberMapper.leaveChallenge(challengeId, userId);

    // 10. ??????????꾩룆???
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
   * API 032: ???????? ?꿔꺂????蹂λ???꿔꺂??袁ㅻ븶筌믠뫀萸???됰슦????
   */
  // [학습] 챌린지 멤버 목록과 요약 통계를 조회한다.
  public ChallengeMemberListResponse getChallengeMembers(String challengeId, String accessToken, String filterStatus) {

    // 1. ????ｋ???嚥▲굧???????????ID ???ㅻ쿋驪??
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    String requestUserId = jwtUtil.getUserIdFromToken(token);

    // 2. ???????? ??됰슦??????? ?癲ル슢캉????
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    // 3. ???됰Ŋ????? ?꿔꺂????蹂λ??癲? ?癲ル슢캉????(?꿔꺂????蹂λ??鶯???됰슦??????醫딆쓧???
    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, requestUserId);
    if (isMember == 0) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }

    // 4. ?꿔꺂????蹂λ???꿔꺂??袁ㅻ븶筌믠뫀萸???됰슦????(User Join)
    List<Map<String, Object>> membersData = challengeMemberMapper.findMembersWithUserInfo(challengeId, filterStatus);

    // 5. Response ?꿔꺂?????몃??
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

  /**
   * API 026: ???????? ????
   * - ??잙갭큔筌??????????醫딆쓧???
   * - ?꿔꺂??袁ㅻ븶?ⓥ뫚留?嚥?RECRUITING) ????븐뻤?????影?쀫븸???????醫딆쓧???
   * - Soft Delete (status -> DISSOLVED, deleted_at ???繹먮냱??
   */
  @Transactional
  // [학습] 챌린지를 삭제 상태로 전환한다.
  public ChallengeDeleteResponse deleteChallenge(String accessToken,
      String challengeId) {
    // 1. ????ｋ???嚥▲굧???????????ID ???ㅻ쿋驪??
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. ???????? ??됰슦????
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    // 3. ??잙갭큔筌??????????癲ル슢캉????(creatorId??醫딆쓧? ????썹땟???????썹땟????잙갭큔筌??????????癲ル슢캉????
    int isLeader = challengeMapper.isLeader(challengeId, userId);
    if (isLeader == 0) {
      throw new RuntimeException("CHALLENGE_004:리더만 접근할 수 있습니다");
    }

    // 4. ????븐뻤???癲ル슢캉????(RECRUITING ????븐뻤??쒖뱽????????醫딆쓧???
    if (ChallengeStatus.RECRUITING != challenge.getStatus()) {
      throw new RuntimeException("CHALLENGE_010:모집 중 상태의 챌린지만 삭제할 수 있습니다");
    }

    // 5. Soft Delete ?꿔꺂??節뉖き??
    challenge.setStatus(ChallengeStatus.COMPLETED);
    challenge.setDeletedAt(LocalDateTime.now());

    challengeMapper.updateStatusAndDeletedAt(challenge);

    return ChallengeDeleteResponse.builder()
        .challengeId(challengeId)
        .deleted(true)
        .build();
  }

  /**
   * API 029: ???嶺???????????繹먮냱??
   */
  @Transactional
  // [학습] 자동 납입 설정을 변경한다.
  public UpdateSupportSettingsResponse updateSupportSettings(String challengeId,
      String accessToken, UpdateSupportSettingsRequest request) {
    // 1. ????ｋ???嚥▲굧???????????ID ???ㅻ쿋驪??
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. ???????? ??됰슦????
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    // 3. ?꿔꺂????蹂λ????癲ル슢캉????
    Map<String, Object> membership = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (membership == null || !"ACTIVE".equals(getString(membership, "STATUS"))) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }

    // 4. ???嶺???????????繹먮냱???????욍걛???ш끽維??
    String autoPayValue = request.getAutoPayEnabled() ? "Y" : "N";
    int result = challengeMemberMapper.updateAutoPayEnabled(userId, challengeId, autoPayValue);
    if (result == 0) {
      throw new RuntimeException("ERROR:자동 납입 설정 업데이트에 실패했습니다");
    }

    // 5. ???繹먮굞????????????影??낟??(???類ㅺ퉻??嚥??????繹먮굞????1??
    LocalDate nextDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);

    return UpdateSupportSettingsResponse.builder()
        .challengeId(challengeId)
        .autoPayEnabled(request.getAutoPayEnabled())
        .nextPaymentDate(nextDate.toString())
        .amount(challenge.getMonthlyFee())
        .build();
  }

  /**
   * API 033: ???????? ?꿔꺂????蹂λ??????노듋????됰슦????
   */
  // [학습] 특정 멤버의 상세 통계 정보를 조회한다.
  public com.woorido.challenge.dto.response.ChallengeMemberDetailResponse getMemberDetail(String challengeId,
      String memberId, String accessToken) {
    // 1. ????ｋ???嚥▲굧????
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    String requestUserId = jwtUtil.getUserIdFromToken(token);

    // 2. ???????? ??됰슦??????? ?癲ル슢캉????
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }

    // 3. ???됰Ŋ????? ???????? ?꿔꺂????蹂λ??癲? ?癲ル슢캉????
    int isMember = challengeMapper.countMemberByChallengeIdAndUserId(challengeId, requestUserId);
    if (isMember == 0) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }

    // 4. ??됰슦?????????꿔꺂????蹂λ??????노듋???癲ル슢???ъ쒜???됰슦????
    Map<String, Object> memberData = challengeMemberMapper.findMemberDetail(challengeId, memberId);

    if (memberData == null) {
      throw new RuntimeException("MEMBER_001:멤버 정보를 찾을 수 없습니다");
    }

    // ????????????ㅻ쿋驪??
    String targetUserId = (String) memberData.get("USER_ID");
    Long totalSupportPaid = memberData.get("TOTAL_SUPPORT_PAID") != null
        ? Long.parseLong(memberData.get("TOTAL_SUPPORT_PAID").toString())
        : 0L;

    // 5. ???????影??낟??
    // 5-1. ?癲????꿔꺂??袁ㅻ븶?????Β?ы닎??얜Ŋ逾η춯?(?????????壤굿?怨룻뱺??癲ル슢?뤸뤃?????붺몭?겹럷???- 0????Β?????쒙쭫??
    int meetingsTotal = 0; // meetingMapper.countTotalMeetings(challengeId);
    int meetingsAttended = 0; // meetingMapper.countAttendedMeetings(challengeId, targetUserId);
    Double attendanceRate = 0.0; // meetingsTotal > 0 ? (double) meetingsAttended / meetingsTotal * 100 : 0.0;

    // 5-2. ?????猷??????嶺?(????썹땟戮녹춿??汝??吏?癒곕㎦? 100.0 ???쒙쭫??or ????????뚯???維◈?
    Double supportRate = totalSupportPaid > 0 ? 100.0 : 0.0;

    com.woorido.challenge.dto.response.ChallengeMemberDetailResponse.Stats stats = com.woorido.challenge.dto.response.ChallengeMemberDetailResponse.Stats
        .builder()
        .totalSupport(totalSupportPaid)
        .supportRate(supportRate)
        .attendanceRate(attendanceRate)
        .meetingsAttended(meetingsAttended)
        .meetingsTotal(meetingsTotal)
        .build();

    // 6. ?????猷?????????됰슦????
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

    // 7. Response ???꾩룆???
    com.woorido.challenge.dto.response.ChallengeMemberDetailResponse.UserInfo userInfo = com.woorido.challenge.dto.response.ChallengeMemberDetailResponse.UserInfo
        .builder()
        .userId(targetUserId)
        .nickname((String) memberData.get("NICKNAME"))
        .profileImage((String) memberData.get("PROFILE_IMAGE"))
        .brix(memberData.get("BRIX") != null ? Double.parseDouble(memberData.get("BRIX").toString()) : 0.0)
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
  // [NEW] API 034: ??잙갭큔筌??????썹땟?④덩?(Transaction Required)
  // ------------------------------------------------------------------------------------------------
  @Transactional
  // [학습] 토큰 기반으로 리더 위임을 수행한다.
  public DelegateLeaderResponse delegateLeaderWithToken(String challengeId, String token, String targetUserId) {
    String userId = jwtUtil.getUserIdFromToken(token);
    return delegateLeader(challengeId, userId, targetUserId);
  }

  @Transactional
  // [학습] 현재 리더를 다른 멤버에게 위임한다.
  public DelegateLeaderResponse delegateLeader(String challengeId, String userId,
      String targetUserId) {
    // 1. ????썹땟????잙갭큔筌???? ?嚥▲굧????
    Map<String, Object> myMemberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);

    // ???됰Ŧ???븍툖異??????汝??吏??(????볥굜???????

    if (myMemberInfo == null ||
        (!"LEADER".equals(myMemberInfo.get("ROLE")) && !"LEADER".equals(myMemberInfo.get("role")))) {
      throw new RuntimeException("리더만 리더 위임을 수행할 수 있습니다.");
    }
    String myMemberId = (String) myMemberInfo.get("MEMBER_ID");
    if (myMemberId == null)
      myMemberId = (String) myMemberInfo.get("member_id"); // Fallback

    // 2. ?????꿔꺂????蹂λ???嚥▲굧????(UserId????됰슦????
    if (userId.equals(targetUserId)) {
      throw new RuntimeException("본인에게는 리더를 위임할 수 없습니다.");
    }

    Map<String, Object> targetMemberInfo = challengeMemberMapper.findByUserIdAndChallengeId(targetUserId, challengeId);
    if (targetMemberInfo == null) {
      throw new RuntimeException("대상 멤버 정보를 찾을 수 없습니다. (ID: " + targetUserId + ")");
    }
    String targetMemberId = (String) targetMemberInfo.get("MEMBER_ID");
    if (targetMemberId == null)
      targetMemberId = (String) targetMemberInfo.get("member_id");

    com.woorido.challenge.domain.ChallengeMember targetMember = challengeMemberMapper.findById(targetMemberId);
    if (targetMember == null) {
      throw new RuntimeException("대상 멤버 상세 정보를 찾을 수 없습니다.");
    }
    if (PrivilegeStatus.ACTIVE != targetMember.getPrivilegeStatus()) {
      throw new RuntimeException("ACTIVE 상태의 멤버에게만 리더를 위임할 수 있습니다. (현재 상태: " + targetMember.getPrivilegeStatus() + ")");
    }

    // 3. ??????????(Atomic Update)
    int count1 = challengeMemberMapper.updateRole("FOLLOWER", myMemberId, challengeId);
    if (count1 == 0) {
      throw new RuntimeException("ERROR: Failed to update current leader role. ID mismatch? " + myMemberId);
    }

    int count2 = challengeMemberMapper.updateRole("LEADER", targetMemberId, challengeId);
    if (count2 == 0) {
      throw new RuntimeException("ERROR: Failed to update new leader role. ID mismatch? " + targetMemberId);
    }

    // 4. ??????????꾩룆???(????ㅼ뒭筌????됰슦????????꾣뤃??findMemberDetail ??嶺뚮??↑짆?
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
   * ???????? ????ㅻ샑??(??癲??嚥▲굧????100% ???????癲ル슢????
   */
  @org.springframework.transaction.annotation.Transactional
  // [학습] 챌린지를 해산하고 멤버 상태를 정리한다.
  public void dissolveChallenge(String challengeId) {
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null)
      return;

    // 1. ???됰Ŋ?좂춯??꿔꺂??節뉖き??(??嶺뚮쮳?놂폇????????
    Long balance = challenge.getBalance();

    if (balance > 0) {
      // ?逆? ???뚯????덈춣?(?꿔꺂?????- ??嶺뚮쮳?놂폇????????
      LedgerEntry ledgerEntry = LedgerEntry.builder()
          .id(java.util.UUID.randomUUID().toString())
          .challengeId(challengeId)
          .type(com.woorido.challenge.domain.LedgerEntryType.EXPENSE)
          .amount(-balance) // ?꿔꺂??????Β?ы닎? ??????용뮋? ???뚯?????癲ル슢캉????????썹땟????????⑤슢?????꿔꺂??????Β?ы닎? amount < 0 or logic handles it.
                            // LedgerMapper logic usually sums based on type or sign.
                            // Existing ledger logic uses negative for expense?
                            // Let's look at `ChallengeService.updateChallenge` logic for reference or
                            // adjust.
                            // Creating `EXPENSE` usually means spending money.
                            // If I set balance to 0, I should record where it went.
          .balanceBefore(balance)
          .balanceAfter(0L)
          .description("Challenge dissolved - remaining balance")
          .createdAt(LocalDateTime.now())
          .build();
      ledgerMapper.insert(ledgerEntry);

      challenge.setBalance(0L);
      // Update challenge balance in DB is handled by updateStatusAndDeletedAt? No,
      // that updates status.
      // Need to update balance separately or add it to update query.
      // challengeMapper.updateBalance(challenge); // This method exists.
    }

    // 2. ???????? ????븐뻤????⑤슢堉???
    challenge.setStatus(ChallengeStatus.COMPLETED);
    challenge.setDeletedAt(LocalDateTime.now());
    challengeMapper.updateStatusAndDeletedAt(challenge);

    // Update balance if changed
    if (balance > 0) {
      challengeMapper.updateBalance(challenge);
    }

    // 3. ?꿔꺂????蹂λ??????븐뻤????⑤슢堉???
    List<Map<String, Object>> members = challengeMemberMapper.findAllActiveMembers(challengeId);
    for (Map<String, Object> member : members) {
      String userId = (String) member.get("USER_ID");
      challengeMemberMapper.leaveChallenge(challengeId, userId);
    }
  }

  /**
   * ??⑤슢???節띾짆??룸쮤????嶺???롪퍓梨띄댚??(???嚥싳쉶瑗ч뇡癒?낄???????癲ル슢????
   * - ?????猷?????붺몭?겹럷??댁뮏?????⑤슢???節띾짆??룸쮤???⑥쥓援?????嶺??꿔꺂?볟젆怨ㅼ춻??뮻?
   * - ?꿔꺂?볟젆怨ㅼ춻??뮻???????????熬곣뫖利당뵓寃밸???(REVOKED)
   * 
   * @return true if deduction occurred, false otherwise
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

    // ??醫딆쓧??????됰Ŋ?좂춯??癲ル슢캉????
    if (account.getBalance() >= monthlyFee) {
      // ??롪퍓梨띄댚????????癲ル슢캉??낆춹?????????꿔꺂??節뉖き??(???꿔꺂???熬곊삳튉??嶺뚮㉡??㎘????붺몭?겹럷??댁뮏???嶺뚮Ĳ????癲ル슢?????嶺뚮슣??땻????
      return false;
    }

    // ??⑤슢???節띾짆??룸쮤????됰Ŋ?좂춯??癲ル슢캉????
    Long lockedBalance = account.getLockedBalance();
    if (lockedBalance < monthlyFee) {
      // ??⑤슢???節띾짆??룸쮤????ㅻ덫 ???낇뀘???- ???ㅻ쿋?? ??됰슦????????썹땟??(60???????嶺??????낅뻘 ??
      return false;
    }

    // ??⑤슢???節띾짆??룸쮤???⑥쥓援???꿔꺂?볟젆怨ㅼ춻??뮻?
    long balanceBefore = account.getBalance();
    long lockedBefore = lockedBalance;

    account.setLockedBalance(lockedBalance - monthlyFee);
    accountMapper.update(account);

    // ?癲ル슢??????????뚯????덈춣?
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

    // ???????? ??影??낟渦??????⒱닪??
    Long chBalance = challenge.getBalance() != null ? challenge.getBalance() : 0L;
    challenge.setBalance(chBalance + monthlyFee);
    challengeMapper.updateBalance(challenge);

    // ?꿔꺂????蹂λ??????븐뻤???????욍걛???ш끽維?? ??⑤슢???節띾짆??룸쮤??????+ ????????熬곣뫖利당뵓寃밸???
    challengeMemberMapper.updateDepositStatus(challengeId, userId, "USED");
    challengeMemberMapper.updatePrivilegeStatus(challengeId, userId, "REVOKED");

    return true;
  }
}
