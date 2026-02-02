package com.woorido.meeting.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woorido.challenge.repository.ChallengeMapper;
import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.common.util.JwtUtil;
import com.woorido.meeting.dto.request.MeetingListRequest;
import com.woorido.meeting.dto.response.MeetingListResponse;
import com.woorido.meeting.repository.MeetingMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingService {

  private final MeetingMapper meetingMapper;
  private final ChallengeMapper challengeMapper;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final com.woorido.account.repository.AccountMapper accountMapper;
  private final com.woorido.challenge.repository.LedgerMapper ledgerMapper;
  private final com.woorido.meeting.repository.MeetingVoteMapper meetingVoteMapper;
  private final JwtUtil jwtUtil;

  // ... getMeetingList ...

  /* ... existing methods ... */
  // It is hard to replace strictly without seeing the file mapping.
  // I will append completeMeeting at the end.

  /**
   * API 035: 모임 목록 조회
   */
  @Transactional(readOnly = true)
  public MeetingListResponse getMeetingList(String challengeId, String accessToken, MeetingListRequest request) {

    // 1. 토큰 검증 및 사용자 ID 추출
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001: 유효하지 않은 토큰입니다");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. 챌린지 존재 여부 확인
    if (challengeMapper.findById(challengeId) == null) {
      throw new RuntimeException("CHALLENGE_001: 챌린지를 찾을 수 없습니다");
    }

    // 3. 멤버 권한 확인
    if (challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId) == 0) {
      throw new RuntimeException("CHALLENGE_003: 챌린지 멤버가 아닙니다");
    }

    // 4. 모임 목록 조회
    int offset = request.getPage() * request.getSize();
    String statusFilter = "N".equals(request.getStatus()) ? null : request.getStatus();

    List<Map<String, Object>> meetings = meetingMapper.findAllByChallengeIdWithFilter(
        challengeId, statusFilter, offset, request.getSize());

    long totalElements = meetingMapper.countAllByChallengeIdWithFilter(challengeId, statusFilter);

    // 5. 응답 매핑
    List<MeetingListResponse.MeetingItem> content = new ArrayList<>();

    for (Map<String, Object> row : meetings) {

      MeetingListResponse.AttendanceInfo attendance = MeetingListResponse.AttendanceInfo.builder()
          .confirmed(Integer.parseInt(row.get("CONFIRMED_COUNT").toString()))
          .total(Integer.parseInt(row.get("TOTAL_MEMBERS").toString()))
          .build();

      MeetingListResponse.BeneficiaryInfo beneficiary = null;
      if (row.get("BENEFICIARY_ID") != null) {
        beneficiary = MeetingListResponse.BeneficiaryInfo.builder()
            .userId(row.get("BENEFICIARY_ID").toString())
            .nickname((String) row.get("BENEFICIARY_NICKNAME"))
            .build();
      }

      content.add(MeetingListResponse.MeetingItem.builder()
          .meetingId(row.get("MEETING_ID").toString())
          .title((String) row.get("TITLE"))
          .description((String) row.get("DESCRIPTION"))
          .status((String) row.get("STATUS"))
          .scheduledAt(formatTimestamp(row.get("SCHEDULED_AT")))
          .location((String) row.get("LOCATION"))
          .attendance(attendance)
          .beneficiary(beneficiary)
          .createdAt(formatTimestamp(row.get("CREATED_AT")))
          .build());
    }

    int totalPages = (int) Math.ceil((double) totalElements / request.getSize());

    return MeetingListResponse.builder()
        .content(content)
        .page(MeetingListResponse.PageInfo.builder()
            .number(request.getPage())
            .size(request.getSize())
            .totalElements(totalElements)
            .totalPages(totalPages)
            .build())
        .build();
  }

  public com.woorido.meeting.dto.response.MeetingDetailResponse getMeetingDetail(String meetingId, String accessToken) {
    // 1. 토큰 검증 및 사용자 ID 추출
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001: 유효하지 않은 토큰입니다");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. 모임 조회
    Map<String, Object> meetingMap = meetingMapper.findById(meetingId);
    if (meetingMap == null) {
      throw new RuntimeException("MEETING_001: 모임을 찾을 수 없습니다");
    }
    // 3. 내 참석 정보 조회 (MeetingVote & Record)
    com.woorido.meeting.domain.MeetingVote vote = meetingVoteMapper.findByMeetingId(meetingId).orElse(null);
    com.woorido.meeting.dto.response.MeetingDetailResponse.MyAttendance myAttendance = null;

    if (vote != null) {
      com.woorido.meeting.domain.MeetingVoteRecord record = meetingVoteMapper.findRecord(vote.getId(), userId)
          .orElse(null);
      if (record != null) {
        myAttendance = com.woorido.meeting.dto.response.MeetingDetailResponse.MyAttendance.builder()
            .status(record.getChoice())
            .respondedAt(formatTimestamp(record.getAttendanceConfirmedAt()))
            .build();
      }
    }
    // 4. Beneficiary Info
    com.woorido.meeting.dto.response.MeetingDetailResponse.BeneficiaryInfo beneficiary = null;
    if (meetingMap.get("BENEFICIARY_ID") != null) {
      beneficiary = com.woorido.meeting.dto.response.MeetingDetailResponse.BeneficiaryInfo.builder()
          .userId((String) meetingMap.get("BENEFICIARY_ID"))
          .nickname((String) meetingMap.get("BENEFICIARY_NICKNAME"))
          .build();
    }

    // 5. Creator Info
    com.woorido.meeting.dto.response.MeetingDetailResponse.CreatorInfo creator = null;
    if (meetingMap.get("CREATOR_ID") != null) {
      creator = com.woorido.meeting.dto.response.MeetingDetailResponse.CreatorInfo.builder()
          .userId((String) meetingMap.get("CREATOR_ID"))
          .nickname((String) meetingMap.get("CREATOR_NICKNAME"))
          .build();
    }

    // 6. Attendance Summary
    com.woorido.meeting.dto.response.MeetingDetailResponse.AttendanceSummary attendance = com.woorido.meeting.dto.response.MeetingDetailResponse.AttendanceSummary
        .builder()
        .confirmed(Integer.parseInt(String.valueOf(meetingMap.get("CONFIRMED_COUNT"))))
        .declind(Integer.parseInt(String.valueOf(meetingMap.get("DECLINED_COUNT"))))
        .pending(Integer.parseInt(String.valueOf(meetingMap.get("PENDING_COUNT"))))
        .total(Integer.parseInt(String.valueOf(meetingMap.get("TOTAL_MEMBERS"))))
        .build();

    return com.woorido.meeting.dto.response.MeetingDetailResponse.builder()
        .meetingId((String) meetingMap.get("MEETING_ID"))
        .challengeId((String) meetingMap.get("CHALLENGE_ID"))
        .title((String) meetingMap.get("TITLE"))
        .description((String) meetingMap.get("DESCRIPTION"))
        .status((String) meetingMap.get("STATUS"))
        .scheduledAt(formatTimestamp(meetingMap.get("SCHEDULED_AT")))
        .location((String) meetingMap.get("LOCATION"))
        .locationDetail((String) meetingMap.get("LOCATION_DETAIL"))
        .agenda((String) meetingMap.get("AGENDA"))
        .benefitAmount(
            meetingMap.get("BENEFIT_AMOUNT") != null ? Long.parseLong(String.valueOf(meetingMap.get("BENEFIT_AMOUNT")))
                : null)
        .createdAt(formatTimestamp(meetingMap.get("CREATED_AT")))
        .attendance(attendance)
        .myAttendance(myAttendance)
        .beneficiary(beneficiary)
        .createdBy(creator)
        .build();
  }

  /**
   * API 037: 모임 생성
   */
  @Transactional
  public com.woorido.meeting.dto.response.CreateMeetingResponse createMeeting(
      String challengeId, String accessToken, com.woorido.meeting.dto.request.CreateMeetingRequest request) {

    // 1. 토큰 검증 및 사용자 ID 추출
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001: 유효하지 않은 토큰입니다");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. 챌린지 및 멤버십 확인 (리더 권한 체크) - ChallengeMemberMapper 사용
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null) {
      throw new RuntimeException("CHALLENGE_003: 챌린지 멤버가 아닙니다");
    }
    String role = (String) memberInfo.get("ROLE");
    if (!"LEADER".equals(role)) {
      throw new RuntimeException("CHALLENGE_004: 리더만 모임을 생성할 수 있습니다");
    }
    String memberId = (String) memberInfo.get("MEMBER_ID");

    // 3. 예정 일시 검증 (현재 시간보다 24시간 이후인지)
    java.time.LocalDateTime scheduledAt = java.time.LocalDateTime.parse(request.getScheduledAt(),
        DateTimeFormatter.ISO_DATE_TIME);
    if (scheduledAt.isBefore(java.time.LocalDateTime.now().plusHours(24))) {
      throw new RuntimeException("MEETING_004: 예정 일시는 최소 24시간 이후여야 합니다");
    }

    // 4. 베네핏 수령자 지정 (Round-Robin) - ChallengeMemberMapper 사용
    List<Map<String, Object>> activeMembers = challengeMemberMapper.findAllActiveMembers(challengeId);
    if (activeMembers.isEmpty()) {
      throw new RuntimeException("CHALLENGE_005: 활성 멤버가 없습니다");
    }

    long meetingCount = meetingMapper.countAllByChallengeIdWithFilter(challengeId, null);

    int beneficiaryIndex = (int) (meetingCount % activeMembers.size());
    Map<String, Object> beneficiaryMap = activeMembers.get(beneficiaryIndex);
    String beneficiaryId = (String) beneficiaryMap.get("USER_ID");
    String beneficiaryNickname = (String) beneficiaryMap.get("NICKNAME");

    // 5. 모임 생성
    String meetingId = java.util.UUID.randomUUID().toString();
    java.time.LocalDateTime now = java.time.LocalDateTime.now();

    com.woorido.meeting.domain.Meeting meeting = com.woorido.meeting.domain.Meeting.builder()
        .id(meetingId)
        .challengeId(challengeId)
        .title(request.getTitle())
        .description(request.getDescription())
        .scheduledAt(scheduledAt)
        .location(request.getLocation())
        .locationDetail(request.getLocationDetail())
        .agenda(request.getAgenda())
        .status("SCHEDULED")
        .beneficiaryId(beneficiaryId)
        .createdBy(userId)
        .createdAt(now)
        .updatedAt(now)
        .build();

    meetingMapper.insert(meeting);

    // 6. 투표/참석 데이터 생성 (MeetingVote & Records)
    com.woorido.meeting.domain.MeetingVote vote = com.woorido.meeting.domain.MeetingVote.builder()
        .id(java.util.UUID.randomUUID().toString())
        .meetingId(meetingId)
        .requiredCount(activeMembers.size())
        .attendCount(0)
        .absentCount(0)
        .status("OPEN")
        .createdAt(now)
        .expiresAt(scheduledAt)
        .build();

    meetingVoteMapper.insertVote(vote);

    for (Map<String, Object> m : activeMembers) {
      String mUserId = (String) m.get("USER_ID");
      com.woorido.meeting.domain.MeetingVoteRecord record = new com.woorido.meeting.domain.MeetingVoteRecord();
      record.setId(java.util.UUID.randomUUID().toString());
      record.setMeetingVoteId(vote.getId());
      record.setUserId(mUserId);
      record.setChoice("PENDING"); // Default choice before member responds
      record.setActualAttendance("PENDING");
      record.setCreatedAt(now);

      meetingVoteMapper.insertRecord(record);
    }

    // 7. 응답 생성
    return com.woorido.meeting.dto.response.CreateMeetingResponse.builder()
        .meetingId(meetingId)
        .title(meeting.getTitle())
        .status(meeting.getStatus())
        .scheduledAt(formatTimestamp(meeting.getScheduledAt()))
        .beneficiary(com.woorido.meeting.dto.response.MeetingDetailResponse.BeneficiaryInfo.builder()
            .userId(beneficiaryId)
            .nickname(beneficiaryNickname)
            .order(beneficiaryIndex + 1)
            .build())
        .createdAt(formatTimestamp(now))
        .message("모임이 생성되었습니다")
        .build();
  }

  /**
   * API 038: 모임 수정
   */
  @Transactional
  public com.woorido.meeting.dto.response.UpdateMeetingResponse updateMeeting(
      String meetingId, String accessToken, com.woorido.meeting.dto.request.UpdateMeetingRequest request) {

    // 1. 토큰 검증
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001: 유효하지 않은 토큰입니다");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. 모임 조회 및 존재 여부 확인
    Map<String, Object> meetingMap = meetingMapper.findById(meetingId);
    if (meetingMap == null) {
      throw new RuntimeException("MEETING_001: 모임을 찾을 수 없습니다");
    }

    // 3. 권한 체크 (생성자만 수정 가능)
    // 주의: meetingMap의 키는 대문자이므로 CREATOR_ID로 가져와야 함.
    // 하지만 findById 쿼리에서 CREATOR_ID로 가져오고 있음.
    // created_by 컬럼은 CREATOR_ID로 알칭되어 있음.
    // 서비스 로직에서 확인: m.created_by as CREATOR_ID
    String creatorId = (String) meetingMap.get("CREATOR_ID");
    if (!userId.equals(creatorId)) {
      // 리더인지 추가 확인이 필요한가?
      // API 명세에는 "리더만"이라고 되어 있음. 생성자가 리더일 것이므로 생성자 체크로 충분할 수 있으나,
      // 리더가 변경되었을 경우를 대비해 현재 챌린지의 리더인지 체크하는 것이 더 정확할 수 있음.
      // 하지만 일단 "작성자(생성자)" 기준으로 구현하고, 필요시 리더 권한 체크로 변경.
      // 명세서: "인증: 리더(본인)" -> 리더만 가능.
      // ChallengeMemberMapper를 통해 현재 리더인지 확인하는 것이 더 안전함.
      String challengeId = (String) meetingMap.get("CHALLENGE_ID");
      Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
      if (memberInfo == null || !"LEADER".equals(memberInfo.get("ROLE"))) {
        throw new RuntimeException("CHALLENGE_004: 리더만 모임을 수정할 수 있습니다");
      }
    }

    // 4. 예정 일시 검증 (과거 모임은 수정 불가)
    // DB의 SCHEDULED_AT은 Timestamp 타입
    java.time.LocalDateTime originalScheduledAt = null;
    Object scheduledAtObj = meetingMap.get("SCHEDULED_AT");
    if (scheduledAtObj instanceof java.sql.Timestamp) {
      originalScheduledAt = ((java.sql.Timestamp) scheduledAtObj).toLocalDateTime();
    }

    // 이미 지난 모임은 수정 불가 (정책에 따라 다를 수 있음, 여기서는 일단 허용하되 경고? 아니면 에러?)
    // 명세서 에러 코드: MEETING_002: 이미 지난 모임은 수정할 수 없습니다
    if (originalScheduledAt != null && originalScheduledAt.isBefore(java.time.LocalDateTime.now())) {
      throw new RuntimeException("MEETING_002: 이미 지난 모임은 수정할 수 없습니다");
    }

    // 5. 모임 정보 업데이트
    java.time.LocalDateTime newScheduledAt = java.time.LocalDateTime.parse(request.getScheduledAt(),
        DateTimeFormatter.ISO_DATE_TIME);
    java.time.LocalDateTime now = java.time.LocalDateTime.now();

    com.woorido.meeting.domain.Meeting meeting = com.woorido.meeting.domain.Meeting.builder()
        .id(meetingId)
        .title(request.getTitle())
        .description(request.getDescription())
        .location(request.getLocation())
        .locationDetail(request.getLocationDetail())
        .agenda(request.getAgenda())
        .scheduledAt(newScheduledAt)
        .updatedAt(now)
        .build();

    meetingMapper.update(meeting);

    // 6. 응답 생성
    return com.woorido.meeting.dto.response.UpdateMeetingResponse.builder()
        .meetingId(meetingId)
        .title(meeting.getTitle())
        .scheduledAt(formatTimestamp(meeting.getScheduledAt()))
        .updatedAt(formatTimestamp(now))
        .message("모임 정보가 수정되었습니다")
        .build();
  }

  /**
   * API 039: 참석 의사 표시
   */
  @Transactional
  public com.woorido.meeting.dto.response.AttendanceResponseResponse respondAttendance(
      String meetingId, String accessToken, com.woorido.meeting.dto.request.AttendanceResponseRequest request) {

    // 1. 토큰 검증
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001: 유효하지 않은 토큰입니다");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. 모임 조회
    Map<String, Object> meetingMap = meetingMapper.findById(meetingId);
    if (meetingMap == null) {
      throw new RuntimeException("MEETING_001: 모임을 찾을 수 없습니다");
    }

    // 3. 참석 정보 조회 (MeetingVote & Record)
    com.woorido.meeting.domain.MeetingVote vote = meetingVoteMapper.findByMeetingId(meetingId)
        .orElseThrow(() -> new RuntimeException("MEETING_001: 모임을 찾을 수 없습니다 (Vote Missing)"));

    com.woorido.meeting.domain.MeetingVoteRecord record = meetingVoteMapper.findRecord(vote.getId(), userId)
        .orElseThrow(() -> new RuntimeException("CHALLENGE_003: 챌린지 멤버가 아닙니다"));

    // 이미 응답했는지 확인 (Choice가 있으면 응답한 것)
    if (record.getChoice() != null) {
      throw new RuntimeException("MEETING_003: 이미 참석 의사를 표시했습니다");
    }

    // 4. 예정 일시 검증 (과거 모임은 응답 불가)
    Object scheduledAtObj = meetingMap.get("SCHEDULED_AT");
    java.time.LocalDateTime scheduledAt = null;
    if (scheduledAtObj instanceof java.sql.Timestamp) {
      scheduledAt = ((java.sql.Timestamp) scheduledAtObj).toLocalDateTime();
    }
    if (scheduledAt != null && scheduledAt.isBefore(java.time.LocalDateTime.now())) {
      throw new RuntimeException("MEETING_002: 이미 지난 모임입니다");
    }

    // 5. 업데이트
    record.setChoice(request.getStatus()); // AGREE / DISAGREE
    record.setAttendanceConfirmedAt(java.time.LocalDateTime.now());
    meetingVoteMapper.updateRecord(record);

    // 6. 응답 생성 (통계 다시 조회)
    Map<String, Object> updatedMeeting = meetingMapper.findById(meetingId);
    int confirmed = ((Number) updatedMeeting.get("CONFIRMED_COUNT")).intValue();
    int declined = ((Number) updatedMeeting.get("DECLINED_COUNT")).intValue();
    int pending = ((Number) updatedMeeting.get("PENDING_COUNT")).intValue();
    int total = ((Number) updatedMeeting.get("TOTAL_MEMBERS")).intValue();

    return com.woorido.meeting.dto.response.AttendanceResponseResponse.builder()
        .meetingId(meetingId)
        .myAttendance(com.woorido.meeting.dto.response.AttendanceResponseResponse.MyAttendanceInfo.builder()
            .status(request.getStatus())
            .respondedAt(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .build())
        .attendance(com.woorido.meeting.dto.response.AttendanceResponseResponse.AttendanceStats.builder()
            .confirmed(confirmed)
            .declined(declined)
            .pending(pending)
            .total(total)
            .build())
        .build();
  }

  /**
   * API 040: 모임 완료 처리
   */
  @Transactional
  public com.woorido.meeting.dto.response.CompleteMeetingResponse completeMeeting(
      String meetingId, String accessToken, com.woorido.meeting.dto.request.CompleteMeetingRequest request) {

    // 1. 토큰 검증
    String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001: 유효하지 않은 토큰입니다");
    }
    String userId = jwtUtil.getUserIdFromToken(token);

    // 2. 모임 조회
    Map<String, Object> meetingMap = meetingMapper.findById(meetingId);
    if (meetingMap == null) {
      throw new RuntimeException("MEETING_001: 모임을 찾을 수 없습니다");
    }

    // 3. 권한 체크 (리더만 가능)
    String challengeId = (String) meetingMap.get("CHALLENGE_ID");
    if (challengeMapper.isLeader(challengeId, userId) == 0) {
      throw new RuntimeException("CHALLENGE_004: 리더만 완료 처리할 수 있습니다");
    }

    // 4. 상태 체크 (이미 완료되었는지)
    if ("COMPLETED".equals(meetingMap.get("STATUS"))) {
      throw new RuntimeException("MEETING_005: 이미 완료된 모임입니다");
    }

    // 5. 실제 참석자 처리 (Vote Records 업데이트)
    com.woorido.meeting.domain.MeetingVote vote = meetingVoteMapper.findByMeetingId(meetingId)
        .orElseThrow(() -> new RuntimeException("VOTE_001: 투표 정보를 찾을 수 없습니다"));

    List<String> actualAttendees = request.getActualAttendees();
    int actualAttendCount = 0;

    if (actualAttendees != null) {
      for (String attendeeId : actualAttendees) {
        com.woorido.meeting.domain.MeetingVoteRecord record = meetingVoteMapper.findRecord(vote.getId(), attendeeId)
            .orElse(null);
        if (record == null) {
          record = new com.woorido.meeting.domain.MeetingVoteRecord();
          record.setId(java.util.UUID.randomUUID().toString());
          record.setMeetingVoteId(vote.getId());
          record.setUserId(attendeeId);
          record.setChoice("AGREE");
          record.setActualAttendance("ATTENDED");
          record.setAttendanceConfirmedAt(java.time.LocalDateTime.now());
          record.setCreatedAt(java.time.LocalDateTime.now());
          meetingVoteMapper.insertRecord(record);
        } else {
          record.setActualAttendance("ATTENDED");
          record.setAttendanceConfirmedAt(java.time.LocalDateTime.now());
          meetingVoteMapper.updateRecord(record);
        }
        actualAttendCount++;
      }
    }

    // 6. 베네핏 정산
    Long benefitAmount = meetingMap.get("BENEFIT_AMOUNT") != null
        ? Long.parseLong(String.valueOf(meetingMap.get("BENEFIT_AMOUNT")))
        : 0L;
    String beneficiaryId = (String) meetingMap.get("BENEFICIARY_ID");

    if (benefitAmount > 0 && beneficiaryId != null) {
      com.woorido.challenge.domain.Challenge challenge = challengeMapper.findById(challengeId);
      if (challenge.getBalance() < benefitAmount) {
        throw new RuntimeException("ACCOUNT_004: 챌린지 잔액이 부족합니다");
      }
      challenge.setBalance(challenge.getBalance() - benefitAmount);
      challengeMapper.updateBalance(challenge);

      com.woorido.challenge.domain.LedgerEntry ledger = new com.woorido.challenge.domain.LedgerEntry();
      ledger.setId(java.util.UUID.randomUUID().toString());
      ledger.setChallengeId(challengeId);
      ledger.setType("EXPENSE");
      ledger.setAmount(benefitAmount);
      ledger.setDescription("모임 베네핏 지급: " + meetingMap.get("TITLE"));
      ledger.setBalanceBefore(challenge.getBalance());
      ledger.setBalanceAfter(challenge.getBalance() - benefitAmount);
      ledger.setRelatedMeetingId(meetingId);
      ledger.setRelatedUserId(beneficiaryId);
      ledger.setCreatedAt(java.time.LocalDateTime.now());
      ledgerMapper.insert(ledger);

      com.woorido.account.domain.Account beneficiaryAccount = accountMapper.findByUserId(beneficiaryId);
      if (beneficiaryAccount != null) {
        beneficiaryAccount.setBalance(beneficiaryAccount.getBalance() + benefitAmount);
        accountMapper.update(beneficiaryAccount);

        com.woorido.account.domain.AccountTransaction tx = new com.woorido.account.domain.AccountTransaction();
        tx.setId(java.util.UUID.randomUUID().toString());
        tx.setAccountId(beneficiaryAccount.getId());
        tx.setType(com.woorido.account.domain.TransactionType.BENEFIT);
        tx.setAmount(benefitAmount);
        tx.setBalanceBefore(beneficiaryAccount.getBalance());
        tx.setBalanceAfter(beneficiaryAccount.getBalance() + benefitAmount);
        tx.setRelatedChallengeId(challengeId);
        tx.setDescription("모임 베네핏 입금");
        tx.setCreatedAt(java.time.LocalDateTime.now());
        accountMapper.saveTransaction(tx);
      }
    }

    // 7. 모임 완료 처리
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    com.woorido.meeting.domain.Meeting meetingUpdate = new com.woorido.meeting.domain.Meeting();
    meetingUpdate.setId(meetingId);
    meetingUpdate.setNotes(request.getNotes());
    meetingUpdate.setCompletedAt(now);
    meetingUpdate.setUpdatedAt(now);

    meetingMapper.complete(meetingUpdate);

    // 8. 응답 생성
    return com.woorido.meeting.dto.response.CompleteMeetingResponse.builder()
        .meetingId(meetingId)
        .status("COMPLETED")
        .attendance(com.woorido.meeting.dto.response.CompleteMeetingResponse.AttendanceStats.builder()
            .actual(actualAttendCount)
            .total(meetingMap.get("TOTAL_MEMBERS") != null
                ? Integer.parseInt(String.valueOf(meetingMap.get("TOTAL_MEMBERS")))
                : 1)
            .rate(meetingMap.get("TOTAL_MEMBERS") != null
                ? (double) actualAttendCount / Integer.parseInt(String.valueOf(meetingMap.get("TOTAL_MEMBERS"))) * 100
                : 100.0)
            .build())
        .benefit(com.woorido.meeting.dto.response.CompleteMeetingResponse.BenefitInfo.builder()
            .amount(benefitAmount)
            .beneficiary(com.woorido.meeting.dto.response.CompleteMeetingResponse.BeneficiaryInfo.builder()
                .userId(beneficiaryId)
                .nickname((String) meetingMap.get("BENEFICIARY_NICKNAME"))
                .build())
            .transferredAt(
                benefitAmount > 0 ? java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
            .build())
        .completedAt(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .message("모임이 완료 처리되었습니다")
        .build();
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
}
