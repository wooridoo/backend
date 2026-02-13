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
  private final com.woorido.meeting.repository.MeetingVoteMapper meetingVoteMapper;
  private final JwtUtil jwtUtil;

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

      content.add(MeetingListResponse.MeetingItem.builder()
          .meetingId(row.get("MEETING_ID").toString())
          .title((String) row.get("TITLE"))
          .description((String) row.get("DESCRIPTION"))
          .status((String) row.get("STATUS"))
          .meetingDate(formatTimestamp(row.get("MEETING_DATE"))) // Changed from scheduledAt
          .location((String) row.get("LOCATION"))
          .attendance(attendance)
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

    // Beneficiary Logic Removed

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
        .declined(Integer.parseInt(String.valueOf(meetingMap.get("DECLINED_COUNT"))))
        .pending(Integer.parseInt(String.valueOf(meetingMap.get("PENDING_COUNT"))))
        .total(Integer.parseInt(String.valueOf(meetingMap.get("TOTAL_MEMBERS"))))
        .build();

    // 7. 참석자 목록 조회
    java.util.List<java.util.Map<String, Object>> attendeeRows = meetingMapper.findAttendeesByMeetingId(meetingId);
    java.util.List<com.woorido.meeting.dto.response.MeetingDetailResponse.MemberInfo> members = new java.util.ArrayList<>();
    if (attendeeRows != null) {
      for (java.util.Map<String, Object> row : attendeeRows) {
        members.add(com.woorido.meeting.dto.response.MeetingDetailResponse.MemberInfo.builder()
            .userId((String) row.get("USER_ID"))
            .nickname((String) row.get("NICKNAME"))
            .profileImage((String) row.get("PROFILE_IMAGE"))
            .build());
      }
    }

    return com.woorido.meeting.dto.response.MeetingDetailResponse.builder()
        .meetingId((String) meetingMap.get("MEETING_ID"))
        .challengeId((String) meetingMap.get("CHALLENGE_ID"))
        .title((String) meetingMap.get("TITLE"))
        .description((String) meetingMap.get("DESCRIPTION"))
        .status((String) meetingMap.get("STATUS"))
        .meetingDate(formatTimestamp(meetingMap.get("MEETING_DATE")))
        .location((String) meetingMap.get("LOCATION"))
        .locationDetail((String) meetingMap.get("LOCATION_DETAIL"))
        .createdAt(formatTimestamp(meetingMap.get("CREATED_AT")))
        .attendance(attendance)
        .myAttendance(myAttendance)
        .members(members)
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

    // 2. 챌린지 및 멤버십 확인 (리더 권한 체크)
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null) {
      throw new RuntimeException("CHALLENGE_003: 챌린지 멤버가 아닙니다");
    }
    String role = (String) memberInfo.get("ROLE");
    if (!"LEADER".equals(role)) {
      throw new RuntimeException("CHALLENGE_004: 리더만 모임을 생성할 수 있습니다");
    }

    // 3. 예정 일시 검증 (현재 시간보다 24시간 이후인지)
    java.time.LocalDateTime meetingDate = java.time.LocalDateTime.parse(request.getMeetingDate(),
        DateTimeFormatter.ISO_DATE_TIME);
    if (meetingDate.isBefore(java.time.LocalDateTime.now().plusHours(24))) {
      throw new RuntimeException("MEETING_004: 예정 일시는 최소 24시간 이후여야 합니다");
    }

    // 4. Beneficiary Logic Removed

    // 5. 모임 생성
    String meetingId = java.util.UUID.randomUUID().toString();
    java.time.LocalDateTime now = java.time.LocalDateTime.now();

    com.woorido.meeting.domain.Meeting meeting = com.woorido.meeting.domain.Meeting.builder()
        .id(meetingId)
        .challengeId(challengeId)
        .title(request.getTitle())
        .description(request.getDescription())
        .meetingDate(meetingDate) // Changed
        .location(request.getLocation())
        .locationDetail(request.getLocationDetail())
        // .agenda() removed
        .status("SCHEDULED")
        // .beneficiaryId() removed
        .createdBy(userId)
        .createdAt(now)
        .updatedAt(now)
        .build();

    meetingMapper.insert(meeting);

    // 6. 투표/참석 데이터 생성 (MeetingVote & Records)
    List<Map<String, Object>> activeMembers = challengeMemberMapper.findAllActiveMembers(challengeId);

    com.woorido.meeting.domain.MeetingVote vote = com.woorido.meeting.domain.MeetingVote.builder()
        .id(java.util.UUID.randomUUID().toString())
        .meetingId(meetingId)
        .requiredCount(activeMembers.size())
        .attendCount(0)
        .absentCount(0)
        .status("OPEN")
        .createdAt(now)
        .expiresAt(meetingDate)
        .build();

    meetingVoteMapper.insertVote(vote);

    for (Map<String, Object> m : activeMembers) {
      String mUserId = (String) m.get("USER_ID");
      com.woorido.meeting.domain.MeetingVoteRecord record = new com.woorido.meeting.domain.MeetingVoteRecord();
      record.setId(java.util.UUID.randomUUID().toString());
      record.setMeetingVoteId(vote.getId());
      record.setUserId(mUserId);
      record.setChoice("PENDING");
      record.setActualAttendance("PENDING");
      record.setCreatedAt(now);

      meetingVoteMapper.insertRecord(record);
    }

    // 7. 응답 생성
    return com.woorido.meeting.dto.response.CreateMeetingResponse.builder()
        .meetingId(meetingId)
        .title(meeting.getTitle())
        .status(meeting.getStatus())
        .meetingDate(formatTimestamp(meeting.getMeetingDate()))
        .location(meeting.getLocation())
        .locationDetail(meeting.getLocationDetail())
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

    // 3. 권한 체크 (생성자만 수정 가능 -> 리더 체크로 강화)
    String challengeId = (String) meetingMap.get("CHALLENGE_ID");
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null || !"LEADER".equals(memberInfo.get("ROLE"))) {
      throw new RuntimeException("CHALLENGE_004: 리더만 모임을 수정할 수 있습니다");
    }

    // 4. 예정 일시 검증 (과거 모임은 수정 불가)
    java.time.LocalDateTime originalMeetingDate = null;
    Object meetingDateObj = meetingMap.get("MEETING_DATE"); // Changed column name
    if (meetingDateObj instanceof java.sql.Timestamp) {
      originalMeetingDate = ((java.sql.Timestamp) meetingDateObj).toLocalDateTime();
    }

    if (originalMeetingDate != null && originalMeetingDate.isBefore(java.time.LocalDateTime.now())) {
      throw new RuntimeException("MEETING_002: 이미 지난 모임은 수정할 수 없습니다");
    }

    // 5. 모임 정보 업데이트
    java.time.LocalDateTime now = java.time.LocalDateTime.now();

    // Partial Update Logic
    com.woorido.meeting.domain.Meeting.MeetingBuilder meetingBuilder = com.woorido.meeting.domain.Meeting.builder()
        .id(meetingId)
        .updatedAt(now);

    if (request.getTitle() != null) {
      meetingBuilder.title(request.getTitle());
    }
    if (request.getDescription() != null) {
      meetingBuilder.description(request.getDescription());
    }
    if (request.getLocation() != null) {
      meetingBuilder.location(request.getLocation());
    }
    if (request.getLocationDetail() != null) {
      meetingBuilder.locationDetail(request.getLocationDetail());
    }
    // meetingDate Handling
    if (request.getMeetingDate() != null) {
      meetingBuilder
          .meetingDate(java.time.LocalDateTime.parse(request.getMeetingDate(), DateTimeFormatter.ISO_DATE_TIME));
    }

    com.woorido.meeting.domain.Meeting meetingUpdate = meetingBuilder.build();

    meetingMapper.update(meetingUpdate);

    // Refetch or construct response based on request + existing
    // Let's construct based on what we have.
    // If request.meetingDate was null, we use original.
    // However, the response object usually returns the current state.
    // We can either refetch or merge. Refetching is safer but costlier.
    // Merging for response:
    String responseTitle = request.getTitle() != null ? request.getTitle() : (String) meetingMap.get("TITLE");
    String responseDate = request.getMeetingDate() != null ? request.getMeetingDate()
        : formatTimestamp(meetingMap.get("MEETING_DATE"));
    String responseLocation = request.getLocation() != null ? request.getLocation()
        : (String) meetingMap.get("LOCATION");
    String responseLocationDetail = request.getLocationDetail() != null ? request.getLocationDetail()
        : (String) meetingMap.get("LOCATION_DETAIL");

    // 6. 응답 생성
    return com.woorido.meeting.dto.response.UpdateMeetingResponse.builder()
        .meetingId(meetingId)
        .title(responseTitle)
        .meetingDate(responseDate)
        .location(responseLocation)
        .locationDetail(responseLocationDetail)
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

    // 3. 참석 정보 조회
    com.woorido.meeting.domain.MeetingVote vote = meetingVoteMapper.findByMeetingId(meetingId)
        .orElseThrow(() -> new RuntimeException("MEETING_001: 모임을 찾을 수 없습니다 (Vote Missing)"));

    com.woorido.meeting.domain.MeetingVoteRecord record = meetingVoteMapper.findRecord(vote.getId(), userId)
        .orElse(null);

    // Record가 없으면 멤버십 확인 후 생성 (Late Joiner 처리)
    if (record == null) {
      String challengeId = (String) meetingMap.get("CHALLENGE_ID");
      if (challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId) == 0) {
        throw new RuntimeException("CHALLENGE_003: 챌린지의 팔로워만 참여가 가능합니다");
      }

      record = new com.woorido.meeting.domain.MeetingVoteRecord();
      record.setId(java.util.UUID.randomUUID().toString());
      record.setMeetingVoteId(vote.getId());
      record.setUserId(userId);
      record.setChoice("PENDING");
      record.setActualAttendance("PENDING");
      record.setCreatedAt(java.time.LocalDateTime.now());

      meetingVoteMapper.insertRecord(record);
    }

    // if (record.getChoice() != null && !"PENDING".equals(record.getChoice())) {
    // throw new RuntimeException("MEETING_003: 이미 참석 의사를 표시했습니다"); // Updating
    // allowed
    // }

    // 4. 예정 일시 검증 (unchanged)
    Object meetingDateObj = meetingMap.get("MEETING_DATE");
    java.time.LocalDateTime meetingDate = null;
    if (meetingDateObj instanceof java.sql.Timestamp) {
      meetingDate = ((java.sql.Timestamp) meetingDateObj).toLocalDateTime();
    }
    if (meetingDate != null && meetingDate.isBefore(java.time.LocalDateTime.now())) {
      throw new RuntimeException("MEETING_002: 이미 지난 모임입니다");
    }

    // 5. 업데이트
    String choice = request.getChoice() != null ? request.getChoice() : request.getStatus();
    record.setChoice(choice);
    record.setAttendanceConfirmedAt(java.time.LocalDateTime.now());
    meetingVoteMapper.updateRecord(record);

    // 6. 응답 생성
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

    // 3. 권한 체크
    String challengeId = (String) meetingMap.get("CHALLENGE_ID");
    if (challengeMapper.isLeader(challengeId, userId) == 0) {
      throw new RuntimeException("CHALLENGE_004: 리더만 완료 처리할 수 있습니다");
    }

    // 4. 상태 체크
    if ("COMPLETED".equals(meetingMap.get("STATUS"))) {
      throw new RuntimeException("MEETING_005: 이미 완료된 모임입니다");
    }

    // 5. 실제 참석자 처리
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

    // 6. Beneficiary & Ledger Logic Reduced as fields might be missing in DB/Map
    // But since we removed beneficiary from create/update, we likely shouldn't
    // process it here either
    // unless the DB table actually has the columns and data exists from somewhere
    // else.
    // Given the user instruction "Remove beneficiary", I will skip the
    // ledger/benefit transaction part
    // to match the "Removal" request.

    // 7. 모임 완료 처리
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    com.woorido.meeting.domain.Meeting meetingUpdate = new com.woorido.meeting.domain.Meeting();
    meetingUpdate.setId(meetingId);
    // meetingUpdate.setNotes(request.getNotes()); // Notes removed
    meetingUpdate.setCompletedAt(now);
    meetingUpdate.setUpdatedAt(now);

    meetingMapper.complete(meetingUpdate);

    // 8. 응답 생성
    // BenefitInfo will strictly carry empty/null values as requested to remove the
    // feature.
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
        .benefit(null) // Removing benefit info
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
