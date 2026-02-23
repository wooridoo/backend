package com.woorido.meeting.service;

import com.woorido.challenge.repository.ChallengeMapper;
import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.common.util.JwtUtil;
import com.woorido.meeting.domain.Meeting;
import com.woorido.meeting.domain.MeetingVote;
import com.woorido.meeting.domain.MeetingVoteRecord;
import com.woorido.meeting.dto.request.AttendanceResponseRequest;
import com.woorido.meeting.dto.request.CompleteMeetingRequest;
import com.woorido.meeting.dto.request.CreateMeetingRequest;
import com.woorido.meeting.dto.request.MeetingListRequest;
import com.woorido.meeting.dto.request.UpdateMeetingRequest;
import com.woorido.meeting.dto.response.AttendanceResponseResponse;
import com.woorido.meeting.dto.response.CompleteMeetingResponse;
import com.woorido.meeting.dto.response.CreateMeetingResponse;
import com.woorido.meeting.dto.response.MeetingDetailResponse;
import com.woorido.meeting.dto.response.MeetingListResponse;
import com.woorido.meeting.dto.response.UpdateMeetingResponse;
import com.woorido.meeting.repository.MeetingMapper;
import com.woorido.meeting.repository.MeetingVoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {
  // 학습 포인트:
  // - 인증/권한 검증을 private helper로 모아 중복을 줄인다.
  // - 응답 DTO 조립 시 DB 타입 차이(Number/Timestamp)를 흡수한다.

  private final MeetingMapper meetingMapper;
  private final ChallengeMapper challengeMapper;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final MeetingVoteMapper meetingVoteMapper;
  private final JwtUtil jwtUtil;

  /**
   * 모임 목록 조회.
   * 흐름: 토큰 해석 -> 챌린지/멤버 검증 -> 필터 조회 -> 페이지 응답 구성
   */
  @Transactional(readOnly = true)
  public MeetingListResponse getMeetingList(String challengeId, String accessToken, MeetingListRequest request) {
    String userId = resolveUserId(accessToken);

    if (challengeMapper.findById(challengeId) == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }
    requireMemberAny(challengeId, userId);

    int offset = request.getPage() * request.getSize();
    // 클라이언트가 "N"(No filter)을 보내면 전체 상태를 조회한다.
    String statusFilter = "N".equals(request.getStatus()) ? null : request.getStatus();

    List<Map<String, Object>> meetings = meetingMapper.findAllByChallengeIdWithFilter(
        challengeId,
        statusFilter,
        offset,
        request.getSize());

    long totalElements = meetingMapper.countAllByChallengeIdWithFilter(challengeId, statusFilter);

    List<MeetingListResponse.MeetingItem> content = new ArrayList<>();
    for (Map<String, Object> row : meetings) {
      MeetingListResponse.AttendanceInfo attendance = MeetingListResponse.AttendanceInfo.builder()
          .confirmed(toInt(row.get("CONFIRMED_COUNT")))
          .total(toInt(row.get("TOTAL_MEMBERS")))
          .build();

      content.add(MeetingListResponse.MeetingItem.builder()
          .meetingId(asString(row.get("MEETING_ID")))
          .title(asString(row.get("TITLE")))
          .description(asString(row.get("DESCRIPTION")))
          .status(asString(row.get("STATUS")))
          .meetingDate(formatTimestamp(row.get("MEETING_DATE")))
          .location(asString(row.get("LOCATION")))
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

  /**
   * 모임 상세 조회.
   * 흐름: 모임 조회 -> 멤버 검증 -> 참석/내 응답 정보 조립 -> 상세 응답 반환
   */
  @Transactional(readOnly = true)
  public MeetingDetailResponse getMeetingDetail(String meetingId, String accessToken) {
    String userId = resolveUserId(accessToken);

    Map<String, Object> meetingMap = meetingMapper.findById(meetingId);
    if (meetingMap == null) {
      throw new RuntimeException("MEETING_001:모임을 찾을 수 없습니다");
    }

    String challengeId = asString(meetingMap.get("CHALLENGE_ID"));
    requireMemberAny(challengeId, userId);

    MeetingVote vote = meetingVoteMapper.findByMeetingId(meetingId).orElse(null);
    MeetingDetailResponse.MyAttendance myAttendance = null;

    if (vote != null) {
      MeetingVoteRecord record = meetingVoteMapper.findRecord(vote.getId(), userId).orElse(null);
      if (record != null) {
        myAttendance = MeetingDetailResponse.MyAttendance.builder()
            .status(record.getChoice())
            .respondedAt(formatTimestamp(record.getAttendanceConfirmedAt()))
            .build();
      }
    }

    MeetingDetailResponse.CreatorInfo creator = null;
    if (meetingMap.get("CREATOR_ID") != null) {
      creator = MeetingDetailResponse.CreatorInfo.builder()
          .userId(asString(meetingMap.get("CREATOR_ID")))
          .nickname(asString(meetingMap.get("CREATOR_NICKNAME")))
          .build();
    }

    MeetingDetailResponse.AttendanceSummary attendance = MeetingDetailResponse.AttendanceSummary.builder()
        .confirmed(toInt(meetingMap.get("CONFIRMED_COUNT")))
        .declined(toInt(meetingMap.get("DECLINED_COUNT")))
        .pending(toInt(meetingMap.get("PENDING_COUNT")))
        .total(toInt(meetingMap.get("TOTAL_MEMBERS")))
        .build();

    List<Map<String, Object>> attendeeRows = meetingMapper.findAttendeesByMeetingId(meetingId);
    List<MeetingDetailResponse.MemberInfo> members = new ArrayList<>();
    if (attendeeRows != null) {
      for (Map<String, Object> row : attendeeRows) {
        members.add(MeetingDetailResponse.MemberInfo.builder()
            .userId(asString(row.get("USER_ID")))
            .nickname(asString(row.get("NICKNAME")))
            .profileImage(asString(row.get("PROFILE_IMAGE")))
            .build());
      }
    }

    return MeetingDetailResponse.builder()
        .meetingId(asString(meetingMap.get("MEETING_ID")))
        .challengeId(challengeId)
        .title(asString(meetingMap.get("TITLE")))
        .description(asString(meetingMap.get("DESCRIPTION")))
        .status(asString(meetingMap.get("STATUS")))
        .meetingDate(formatTimestamp(meetingMap.get("MEETING_DATE")))
        .location(asString(meetingMap.get("LOCATION")))
        .locationDetail(asString(meetingMap.get("LOCATION_DETAIL")))
        .createdAt(formatTimestamp(meetingMap.get("CREATED_AT")))
        .attendance(attendance)
        .myAttendance(myAttendance)
        .members(members)
        .createdBy(creator)
        .build();
  }

  /**
   * 모임 생성.
   * 흐름: 리더 권한 검증 -> 모임 저장 -> 참석 투표(기록 포함) 초기화 -> 생성 응답 반환
   */
  @Transactional
  public CreateMeetingResponse createMeeting(String challengeId, String accessToken, CreateMeetingRequest request) {
    String userId = resolveUserId(accessToken);

    if (challengeMapper.findById(challengeId) == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }
    requireLeader(challengeId, userId);

    LocalDateTime meetingDate = LocalDateTime.parse(request.getMeetingDate(), DateTimeFormatter.ISO_DATE_TIME);
    if (meetingDate.isBefore(LocalDateTime.now().plusHours(24))) {
      throw new RuntimeException("MEETING_004:모임 일시는 최소 24시간 이후여야 합니다");
    }

    String meetingId = UUID.randomUUID().toString();
    LocalDateTime now = LocalDateTime.now();

    Meeting meeting = Meeting.builder()
        .id(meetingId)
        .challengeId(challengeId)
        .title(request.getTitle())
        .description(request.getDescription())
        .meetingDate(meetingDate)
        .location(request.getLocation())
        .locationDetail(request.getLocationDetail())
        .status("SCHEDULED")
        .createdBy(userId)
        .createdAt(now)
        .updatedAt(now)
        .build();
    meetingMapper.insert(meeting);

    // ACTIVE 멤버를 기준으로 참석 투표 레코드를 사전 생성한다.
    List<Map<String, Object>> activeMembers = challengeMemberMapper.findAllActiveMembers(challengeId);

    MeetingVote vote = MeetingVote.builder()
        .id(UUID.randomUUID().toString())
        .meetingId(meetingId)
        .requiredCount(activeMembers.size())
        .attendCount(0)
        .absentCount(0)
        .status("OPEN")
        .createdAt(now)
        .expiresAt(meetingDate)
        .build();
    meetingVoteMapper.insertVote(vote);

    for (Map<String, Object> member : activeMembers) {
      MeetingVoteRecord record = new MeetingVoteRecord();
      record.setId(UUID.randomUUID().toString());
      record.setMeetingVoteId(vote.getId());
      record.setUserId(asString(member.get("USER_ID")));
      record.setChoice("PENDING");
      record.setActualAttendance("PENDING");
      record.setCreatedAt(now);
      meetingVoteMapper.insertRecord(record);
    }

    return CreateMeetingResponse.builder()
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
   * 모임 수정.
   * 리더만 가능하며, 이미 지난 모임은 수정할 수 없다.
   */
  @Transactional
  public UpdateMeetingResponse updateMeeting(String meetingId, String accessToken, UpdateMeetingRequest request) {
    String userId = resolveUserId(accessToken);

    Map<String, Object> meetingMap = meetingMapper.findById(meetingId);
    if (meetingMap == null) {
      throw new RuntimeException("MEETING_001:모임을 찾을 수 없습니다");
    }

    String challengeId = asString(meetingMap.get("CHALLENGE_ID"));
    requireLeader(challengeId, userId);

    LocalDateTime originalMeetingDate = toLocalDateTime(meetingMap.get("MEETING_DATE"));
    if (originalMeetingDate != null && originalMeetingDate.isBefore(LocalDateTime.now())) {
      throw new RuntimeException("MEETING_002:이미 지난 모임은 수정할 수 없습니다");
    }

    LocalDateTime now = LocalDateTime.now();

    // null이 아닌 필드만 부분 업데이트하도록 빌더에 반영한다.
    Meeting.MeetingBuilder meetingBuilder = Meeting.builder()
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
    if (request.getMeetingDate() != null) {
      meetingBuilder.meetingDate(LocalDateTime.parse(request.getMeetingDate(), DateTimeFormatter.ISO_DATE_TIME));
    }

    meetingMapper.update(meetingBuilder.build());

    String responseTitle = request.getTitle() != null ? request.getTitle() : asString(meetingMap.get("TITLE"));
    String responseDate = request.getMeetingDate() != null ? request.getMeetingDate() : formatTimestamp(meetingMap.get("MEETING_DATE"));
    String responseLocation = request.getLocation() != null ? request.getLocation() : asString(meetingMap.get("LOCATION"));
    String responseLocationDetail = request.getLocationDetail() != null ? request.getLocationDetail() : asString(meetingMap.get("LOCATION_DETAIL"));

    return UpdateMeetingResponse.builder()
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
   * 모임 참석 응답 처리.
   * 사용자별 레코드를 생성/갱신하며, 집계값은 조회 시점 쿼리 결과를 사용한다.
   */
  @Transactional
  public AttendanceResponseResponse respondAttendance(String meetingId, String accessToken, AttendanceResponseRequest request) {
    String userId = resolveUserId(accessToken);

    Map<String, Object> meetingMap = meetingMapper.findById(meetingId);
    if (meetingMap == null) {
      throw new RuntimeException("MEETING_001:모임을 찾을 수 없습니다");
    }

    String challengeId = asString(meetingMap.get("CHALLENGE_ID"));
    requireMemberActive(challengeId, userId);

    MeetingVote vote = meetingVoteMapper.findByMeetingId(meetingId)
        .orElseThrow(() -> new RuntimeException("MEETING_001:모임 투표를 찾을 수 없습니다"));

    LocalDateTime now = LocalDateTime.now();
    if (!isAttendanceVoteCastable(vote.getStatus())) {
      throw new RuntimeException("MEETING_006:모임 참석 투표가 종료되었습니다");
    }
    if (vote.getExpiresAt() != null && now.isAfter(vote.getExpiresAt())) {
      meetingVoteMapper.updateVoteStatus(vote.getId(), "EXPIRED");
      throw new RuntimeException("MEETING_006:모임 참석 투표가 종료되었습니다");
    }

    // 기존 응답 레코드가 없으면 PENDING 기본 레코드를 생성한다.
    MeetingVoteRecord record = meetingVoteMapper.findRecord(vote.getId(), userId).orElse(null);
    if (record == null) {
      record = new MeetingVoteRecord();
      record.setId(UUID.randomUUID().toString());
      record.setMeetingVoteId(vote.getId());
      record.setUserId(userId);
      record.setChoice("PENDING");
      record.setActualAttendance("PENDING");
      record.setCreatedAt(now);
      meetingVoteMapper.insertRecord(record);
    }

    LocalDateTime meetingDate = toLocalDateTime(meetingMap.get("MEETING_DATE"));
    if (meetingDate != null && meetingDate.isBefore(now)) {
      throw new RuntimeException("MEETING_002:이미 지난 모임입니다");
    }

    String choice = normalizeAttendanceChoice(request);
    record.setChoice(choice);
    record.setActualAttendance("PENDING");
    record.setAttendanceConfirmedAt(now);
    meetingVoteMapper.updateRecord(record);

    Map<String, Object> updatedMeeting = meetingMapper.findById(meetingId);

    return AttendanceResponseResponse.builder()
        .meetingId(meetingId)
        .myAttendance(AttendanceResponseResponse.MyAttendanceInfo.builder()
            .status(choice)
            .respondedAt(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .build())
        .attendance(AttendanceResponseResponse.AttendanceStats.builder()
            .confirmed(toInt(updatedMeeting.get("CONFIRMED_COUNT")))
            .declined(toInt(updatedMeeting.get("DECLINED_COUNT")))
            .pending(toInt(updatedMeeting.get("PENDING_COUNT")))
            .total(toInt(updatedMeeting.get("TOTAL_MEMBERS")))
            .build())
        .build();
  }

  /**
   * 모임 완료 처리.
   * 리더 권한 검증 후 실제 참석자를 반영하고 모임 상태를 COMPLETED로 전환한다.
   */
  @Transactional
  public CompleteMeetingResponse completeMeeting(String meetingId, String accessToken, CompleteMeetingRequest request) {
    String userId = resolveUserId(accessToken);

    Map<String, Object> meetingMap = meetingMapper.findById(meetingId);
    if (meetingMap == null) {
      throw new RuntimeException("MEETING_001:모임을 찾을 수 없습니다");
    }

    String challengeId = asString(meetingMap.get("CHALLENGE_ID"));
    if (challengeMapper.isLeader(challengeId, userId) == 0) {
      throw new RuntimeException("CHALLENGE_004:리더만 모임 완료 처리가 가능합니다");
    }

    if ("COMPLETED".equals(asString(meetingMap.get("STATUS")))) {
      throw new RuntimeException("MEETING_005:이미 완료된 모임입니다");
    }

    MeetingVote vote = meetingVoteMapper.findByMeetingId(meetingId)
        .orElseThrow(() -> new RuntimeException("MEETING_001:모임 투표를 찾을 수 없습니다"));

    List<String> actualAttendees = request != null ? request.getActualAttendees() : null;
    if (actualAttendees == null || actualAttendees.isEmpty()) {
      throw new RuntimeException("MEETING_003:실제 참석자를 1명 이상 선택해야 합니다");
    }

    int actualAttendCount = 0;
    Set<String> uniqueAttendees = new HashSet<>();
    LocalDateTime now = LocalDateTime.now();

    for (String attendeeIdRaw : actualAttendees) {
      if (!hasText(attendeeIdRaw)) {
        throw new RuntimeException("MEETING_003:참석자 ID가 비어 있습니다");
      }

      String attendeeId = attendeeIdRaw.trim();
      if (!uniqueAttendees.add(attendeeId)) {
        throw new RuntimeException("MEETING_003:실제 참석자 목록에 중복 ID가 포함되어 있습니다");
      }

      Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(attendeeId, challengeId);
      if (memberInfo == null || !"ACTIVE".equals(asString(memberInfo.get("STATUS")))) {
        throw new RuntimeException("MEETING_003:활성 멤버만 실제 참석자로 처리할 수 있습니다");
      }

      if (meetingMapper.isAttendee(meetingId, attendeeId) == 0) {
        throw new RuntimeException("MEETING_003:출석 응답이 AGREE인 멤버만 실제 참석자로 처리할 수 있습니다");
      }

      MeetingVoteRecord record = meetingVoteMapper.findRecord(vote.getId(), attendeeId).orElse(null);
      if (record == null) {
        record = new MeetingVoteRecord();
        record.setId(UUID.randomUUID().toString());
        record.setMeetingVoteId(vote.getId());
        record.setUserId(attendeeId);
        record.setChoice("AGREE");
        record.setActualAttendance("ATTENDED");
        record.setAttendanceConfirmedAt(now);
        record.setCreatedAt(now);
        meetingVoteMapper.insertRecord(record);
      } else {
        record.setActualAttendance("ATTENDED");
        record.setAttendanceConfirmedAt(now);
        meetingVoteMapper.updateRecord(record);
      }
      actualAttendCount++;
    }

    Meeting meetingUpdate = new Meeting();
    meetingUpdate.setId(meetingId);
    meetingUpdate.setCompletedAt(now);
    meetingUpdate.setUpdatedAt(now);
    meetingMapper.complete(meetingUpdate);
    challengeMapper.touchLeaderLastActiveAt(challengeId, userId);

    int totalMembers = toInt(meetingMap.get("TOTAL_MEMBERS"));

    return CompleteMeetingResponse.builder()
        .meetingId(meetingId)
        .status("COMPLETED")
        .attendance(CompleteMeetingResponse.AttendanceStats.builder()
            .actual(actualAttendCount)
            .total(Math.max(totalMembers, 0))
            .rate(totalMembers > 0 ? (double) actualAttendCount / totalMembers * 100 : 0.0)
            .build())
        .benefit(null)
        .completedAt(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .message("모임 완료 처리되었습니다")
        .build();
  }

  /**
   * 모임 삭제.
   * 리더 권한 검증 후 모임/참석 투표 데이터를 정리한다.
   */
  @Transactional
  public Map<String, Object> deleteMeeting(String meetingId, String accessToken) {
    String userId = resolveUserId(accessToken);

    Map<String, Object> meetingMap = meetingMapper.findById(meetingId);
    if (meetingMap == null) {
      throw new RuntimeException("MEETING_001:모임을 찾을 수 없습니다");
    }

    String challengeId = asString(meetingMap.get("CHALLENGE_ID"));
    requireLeader(challengeId, userId);

    if ("COMPLETED".equals(asString(meetingMap.get("STATUS")))) {
      throw new RuntimeException("MEETING_005:이미 완료된 모임입니다");
    }

    MeetingVote vote = meetingVoteMapper.findByMeetingId(meetingId).orElse(null);
    if (vote != null) {
      meetingVoteMapper.deleteRecordsByMeetingVoteId(vote.getId());
      meetingVoteMapper.deleteVoteById(vote.getId());
    }

    meetingMapper.deleteById(meetingId);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("meetingId", meetingId);
    response.put("deletedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    return response;
  }

  /**
   * 모임 참석 의사 취소.
   */
  @Transactional
  public Map<String, Object> cancelAttendance(String meetingId, String accessToken) {
    String userId = resolveUserId(accessToken);

    Map<String, Object> meetingMap = meetingMapper.findById(meetingId);
    if (meetingMap == null) {
      throw new RuntimeException("MEETING_001:모임을 찾을 수 없습니다");
    }

    String challengeId = asString(meetingMap.get("CHALLENGE_ID"));
    requireMemberActive(challengeId, userId);

    LocalDateTime meetingDate = toLocalDateTime(meetingMap.get("MEETING_DATE"));
    if (meetingDate != null && meetingDate.isBefore(LocalDateTime.now())) {
      throw new RuntimeException("MEETING_002:이미 지난 모임입니다");
    }

    MeetingVote vote = meetingVoteMapper.findByMeetingId(meetingId)
        .orElseThrow(() -> new RuntimeException("MEETING_001:모임 투표를 찾을 수 없습니다"));

    MeetingVoteRecord record = meetingVoteMapper.findRecord(vote.getId(), userId).orElse(null);
    if (record == null || "PENDING".equalsIgnoreCase(asString(record.getChoice()))) {
      throw new RuntimeException("MEETING_006:참석 의사를 표시하지 않았습니다");
    }

    record.setChoice("PENDING");
    record.setActualAttendance("PENDING");
    record.setAttendanceConfirmedAt(null);
    meetingVoteMapper.updateRecord(record);

    Map<String, Object> updatedMeeting = meetingMapper.findById(meetingId);
    Map<String, Object> attendance = new LinkedHashMap<>();
    attendance.put("confirmed", toInt(updatedMeeting.get("CONFIRMED_COUNT")));
    attendance.put("declined", toInt(updatedMeeting.get("DECLINED_COUNT")));
    attendance.put("pending", toInt(updatedMeeting.get("PENDING_COUNT")));
    attendance.put("total", toInt(updatedMeeting.get("TOTAL_MEMBERS")));

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("meetingId", meetingId);
    response.put("myAttendance", null);
    response.put("attendance", attendance);
    response.put("cancelledAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    return response;
  }

  /**
   * Authorization 헤더/토큰 문자열에서 userId를 추출한다.
   */
  private String resolveUserId(String accessToken) {
    String token = accessToken != null && accessToken.startsWith("Bearer ")
        ? accessToken.substring(7)
        : accessToken;

    if (token == null || !jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001:유효하지 않은 토큰입니다");
    }
    return jwtUtil.getUserIdFromToken(token);
  }

  /**
   * 챌린지 리더 권한 검증 공통 메서드.
   */
  private void requireLeader(String challengeId, String userId) {
    Map<String, Object> memberInfo = requireMemberActive(challengeId, userId);
    if (!"LEADER".equals(asString(memberInfo.get("ROLE")))) {
      throw new RuntimeException("CHALLENGE_004:리더만 접근 가능합니다");
    }
  }

  /**
   * 챌린지 멤버 검증(LEFT 제외).
   */
  private Map<String, Object> requireMemberAny(String challengeId, String userId) {
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null || "LEFT".equals(asString(memberInfo.get("STATUS")))) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }
    return memberInfo;
  }

  /**
   * ACTIVE 멤버 검증(쓰기/응답 계열 API).
   */
  private Map<String, Object> requireMemberActive(String challengeId, String userId) {
    Map<String, Object> memberInfo = requireMemberAny(challengeId, userId);
    if (!"ACTIVE".equals(asString(memberInfo.get("STATUS")))) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }
    return memberInfo;
  }

  /**
   * DB/JDBC 타입 값을 LocalDateTime으로 변환한다.
   */
  private LocalDateTime toLocalDateTime(Object value) {
    if (value instanceof java.sql.Timestamp ts) {
      return ts.toLocalDateTime();
    }
    if (value instanceof LocalDateTime dt) {
      return dt;
    }
    return null;
  }

  /**
   * LocalDateTime을 API 응답용 ISO 문자열로 변환한다.
   */
  private String formatTimestamp(Object timestamp) {
    LocalDateTime dt = toLocalDateTime(timestamp);
    if (dt != null) {
      return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    return timestamp != null ? timestamp.toString() : null;
  }

  /**
   * 숫자 타입/문자열을 int로 안전하게 변환한다.
   */
  private int toInt(Object value) {
    if (value instanceof Number n) {
      return n.intValue();
    }
    if (value == null) {
      return 0;
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * 참석 투표 가능 상태인지 확인한다.
   */
  private boolean isAttendanceVoteCastable(String status) {
    if (status == null) {
      return true;
    }
    String normalized = status.trim().toUpperCase();
    return "PENDING".equals(normalized)
        || "OPEN".equals(normalized)
        || "IN_PROGRESS".equals(normalized);
  }

  private String normalizeAttendanceChoice(AttendanceResponseRequest request) {
    String rawChoice = request != null ? request.getChoice() : null;
    if (!hasText(rawChoice) && request != null) {
      rawChoice = request.getStatus();
    }
    if (!hasText(rawChoice)) {
      throw new RuntimeException("MEETING_003:출석 응답값이 필요합니다");
    }

    String normalized = rawChoice.trim().toUpperCase();
    if (!"AGREE".equals(normalized) && !"DISAGREE".equals(normalized) && !"PENDING".equals(normalized)) {
      throw new RuntimeException("MEETING_003:지원하지 않는 출석 응답값입니다");
    }
    return normalized;
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private String asString(Object value) {
    return value == null ? null : value.toString();
  }
}
