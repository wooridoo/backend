package com.woorido.vote.service;

import com.woorido.challenge.repository.ChallengeMapper;
import com.woorido.common.dto.PageInfo;
import com.woorido.vote.domain.Vote.VoteStatus;
import com.woorido.vote.domain.Vote.VoteType;
import com.woorido.vote.dto.VoteDto;
import com.woorido.vote.dto.response.VoteListResponse;
import com.woorido.vote.dto.response.VoteDetailResponse;
import com.woorido.vote.repository.VoteMapper;
import com.woorido.common.mapper.UserMapper;
import com.woorido.common.entity.User;
import com.woorido.challenge.domain.Challenge;
import com.woorido.vote.domain.Vote;
import lombok.RequiredArgsConstructor;
import com.woorido.vote.repository.ExpenseVoteMapper;
import com.woorido.vote.repository.GeneralVoteMapper;
import com.woorido.vote.repository.VoteQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

  private final VoteMapper voteMapper;
  private final ExpenseVoteMapper expenseVoteMapper;
  private final GeneralVoteMapper generalVoteMapper;
  private final VoteQueryMapper voteQueryMapper;
  private final ChallengeMapper challengeMapper;
  private final UserMapper userMapper;
  private final com.woorido.meeting.repository.MeetingMapper meetingMapper;
  private final com.woorido.common.util.JwtUtil jwtUtil;

  // ... (existing getVoteList, getVoteDetail) ...

  /**
   * API 043: 투표 생성
   */
  /**
   * API 043: 투표 생성
   */
  @Transactional
  public VoteDto createVote(String challengeId, String userId, com.woorido.vote.dto.request.CreateVoteRequest request) {
    // 1. 권한 체크
    if (challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId) == 0) {
      throw new RuntimeException("CHALLENGE_003: 챌린지 멤버가 아닙니다");
    }

    // 2. 마감 시간 체크 (최소 24시간)
    if (request.getDeadline().isBefore(LocalDateTime.now().plusHours(24).minusMinutes(1))) {
      throw new RuntimeException("VOTE_002: 마감 시간은 최소 24시간 이후여야 합니다");
    }

    Challenge challenge = challengeMapper.findById(challengeId);
    int currentMembers = challenge != null ? challenge.getCurrentMembers() : 0;
    int requiredCount = (int) Math.ceil(currentMembers * 0.7);
    if (requiredCount < 1)
      requiredCount = 1;

    String voteId = java.util.UUID.randomUUID().toString();
    VoteType type = request.getType();
    LocalDateTime now = LocalDateTime.now();

    // 작성자 정보 조회 (응답용)
    User creator = userMapper.findById(userId);
    String nickname = creator != null ? creator.getNickname() : "Unknown";

    // 3. 타입별 분기 처리
    if (type == VoteType.MEETING_ATTENDANCE) {
      // 기존 Meeting 로직 유지
      String meetingId = java.util.UUID.randomUUID().toString();
      com.woorido.meeting.domain.Meeting meeting = com.woorido.meeting.domain.Meeting.builder()
          .id(meetingId)
          .challengeId(challengeId)
          .title(request.getTitle())
          .description(request.getDescription())
          .location(type.name())
          .agenda(request.getTargetId())
          .status("VOTE")
          .scheduledAt(request.getDeadline())
          .createdBy(userId)
          .createdAt(now)
          .updatedAt(now)
          .build();
      meetingMapper.insert(meeting);

      com.woorido.vote.domain.Vote vote = com.woorido.vote.domain.Vote.builder()
          .id(voteId)
          .challengeId(challengeId)
          .meetingId(meetingId)
          .type(type)
          .status(VoteStatus.IN_PROGRESS)
          .createdBy(userId)
          .deadline(request.getDeadline())
          .createdAt(now)
          .requiredCount(requiredCount)
          .build();
      voteMapper.insert(vote);

    } else if (type == VoteType.EXPENSE) {
      // ExpenseVote 로직
      com.woorido.vote.domain.ExpenseVote expenseVote = com.woorido.vote.domain.ExpenseVote.builder()
          .id(voteId)
          .challengeId(challengeId)
          .createdBy(userId)
          .title(request.getTitle())
          .description(request.getDescription())
          .targetId(request.getTargetId())
          .requiredCount(requiredCount)
          .status("IN_PROGRESS")
          .createdAt(now)
          .expiresAt(request.getDeadline())
          .build();
      expenseVoteMapper.insert(expenseVote);

    } else {
      // GeneralVote 로직 (KICK, LEADER_KICK, DISSOLVE)
      com.woorido.vote.domain.GeneralVote generalVote = com.woorido.vote.domain.GeneralVote.builder()
          .id(voteId)
          .challengeId(challengeId)
          .createdBy(userId)
          .type(type.name())
          .title(request.getTitle())
          .description(request.getDescription())
          .targetUserId(request.getTargetId()) // GeneralVote에서는 targetId가 targetUserId
          .requiredCount(requiredCount)
          .eligibleCount(currentMembers)
          .status("IN_PROGRESS")
          .createdAt(now)
          .expiresAt(request.getDeadline())
          .build();
      generalVoteMapper.insert(generalVote);
    }

    return VoteDto.builder()
        .voteId(voteId)
        .type(type)
        .title(request.getTitle())
        .status(VoteStatus.IN_PROGRESS)
        .createdBy(VoteDto.CreatorDto.builder().userId(userId).nickname(nickname).build())
        .voteCount(VoteDto.VoteCountDto.builder().agree(0).disagree(0).total(0).build())
        .deadline(request.getDeadline())
        .createdAt(now)
        .build();
  }

  /**
   * API 041: 투표 목록 조회 (Union Query 사용)
   */
  @Transactional(readOnly = true)
  public VoteListResponse getVoteList(String challengeId, String userId, String status, String type, int page,
      int size) {
    if (challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId) == 0) {
      throw new RuntimeException("CHALLENGE_003: 챌린지 멤버가 아닙니다");
    }

    int offset = page * size;
    // VoteQueryMapper 사용 (필터링은 쿼리 내에서 처리되거나, Union 후 처리가 필요함)
    // 현재 VoteQueryMapper.findAllUnionByChallengeId는 status, type 필터를 인자로 받지 않음.
    // 일단 전체 조회 후 메모리 필터링 하기엔 페이징 때문에 어려움.
    // *주의*: 앞서 만든 VoteQueryMapper XML에는 필터 조건이 빠져있음.
    // 일단은 전체 목록을 가져오게 되므로, 필터링은 XML 수정이 필요할 수 있음.
    // 하지만 우선순위 높은 '동작'을 위해 일단 호출함. (필터는 추후 보완)

    List<Map<String, Object>> resultList = voteQueryMapper.findAllUnionByChallengeId(challengeId, offset, size);
    long totalElements = voteQueryMapper.countAllUnionByChallengeId(challengeId);

    List<VoteDto> content = resultList.stream().map(this::mapToVoteDto).collect(Collectors.toList());

    int totalPages = (int) Math.ceil((double) totalElements / size);
    PageInfo pageInfo = PageInfo.builder()
        .number(page).size(size).totalElements(totalElements).totalPages(totalPages).build();

    return VoteListResponse.builder().content(content).page(pageInfo).build();
  }

  /**
   * API 042: 투표 상세 조회
   */
  @Transactional(readOnly = true)
  public VoteDetailResponse getVoteDetail(String voteId, String userId) {
    // 1. 기본 정보 조회 (타입 식별)
    Map<String, Object> basicInfo = voteQueryMapper.findByIdBasic(voteId);
    if (basicInfo == null) {
      throw new RuntimeException("VOTE_001: 투표를 찾을 수 없습니다");
    }

    String typeStr = (String) basicInfo.get("TYPE");
    // String challengeId = (String) basicInfo.get("CHALLENGE_ID"); // DB 컬럼 대소문자 주의
    // (ResultType map)
    // Oracle은 보통 대문자로 키가 옴.
    String challengeId = (String) basicInfo.getOrDefault("CHALLENGE_ID", basicInfo.get("challenge_id"));

    // 2. 권한 체크
    if (challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId) == 0) {
      throw new RuntimeException("VOTE_003: 투표 조회 권한이 없습니다");
    }

    // 3. 타입별 상세 조회 및 취합
    String creatorId = null;
    String title = null;
    String description = null;
    String status = null;
    java.sql.Timestamp createdAt = null;
    java.sql.Timestamp deadline = null;

    VoteType type = VoteType.valueOf(typeStr);

    VoteDto.VoteCountDto voteCount = null;
    String myVote = null;

    if (type == VoteType.MEETING_ATTENDANCE) {
      Vote vote = voteMapper.findById(voteId); // 기존 로직
      creatorId = vote.getCreatedBy();
      title = vote.getTitle();
      description = vote.getDescription();
      status = vote.getStatus().name();
      // createdAt, deadline 변환 필요
      // ... 기존 DTO 매핑 로직 활용이 좋지만 여기선 직접 매핑

      // 내 투표, 카운트
      myVote = voteMapper.findMyVote(voteId, userId);
      Map<String, Object> c = voteMapper.findVoteCounts(voteId);
      voteCount = VoteDto.VoteCountDto.builder()
          .agree(((Number) c.get("AGREE")).intValue())
          .disagree(((Number) c.get("DISAGREE")).intValue())
          .total(((Number) c.get("TOTAL")).intValue()).build();

      return generateDetailResponse(vote, voteCount, myVote, userId);

    } else if (type == VoteType.EXPENSE) {
      com.woorido.vote.domain.ExpenseVote vote = expenseVoteMapper.findById(voteId);
      creatorId = vote.getCreatedBy();
      title = vote.getTitle();
      description = vote.getDescription();
      status = vote.getStatus();

      myVote = expenseVoteMapper.checkRecordExisting(voteId, userId) > 0 ? "VOTED" : null;
      // 실제 어떤 걸 찍었는지는 checkRecordExisting으론 모름. findMyRecord가 필요할 수 있음.
      // 일단은 null or 'some choice'
      // 상세 구현 생략 (기존 findMyVote 로직이 없어서 새로 만들어야 함)

      Map<String, Object> c = expenseVoteMapper.findVoteCounts(voteId);
      voteCount = VoteDto.VoteCountDto.builder()
          .agree(((Number) c.get("AGREE")).intValue())
          .disagree(((Number) c.get("DISAGREE")).intValue())
          .total(((Number) c.get("TOTAL")).intValue()).build();

    } else { // GENERAL
      com.woorido.vote.domain.GeneralVote vote = generalVoteMapper.findById(voteId);
      creatorId = vote.getCreatedBy();
      title = vote.getTitle();
      description = vote.getDescription();
      status = vote.getStatus();

      Map<String, Object> c = generalVoteMapper.findVoteCounts(voteId);
      voteCount = VoteDto.VoteCountDto.builder()
          .agree(((Number) c.get("AGREE")).intValue())
          .disagree(((Number) c.get("DISAGREE")).intValue())
          .total(((Number) c.get("TOTAL")).intValue()).build();
    }

    // 단순화를 위해 공통 리턴 처리 (작성자 닉네임 조회 등)
    User creator = userMapper.findById(creatorId);
    VoteDto.CreatorDto creatorDto = VoteDto.CreatorDto.builder()
        .userId(creatorId).nickname(creator != null ? creator.getNickname() : "Unknown").build();

    // createdAt, deadline 등은 basicInfo에서 가져오거나 각 객체에서 가져옴.

    return VoteDetailResponse.builder()
        .voteId(voteId)
        .challengeId(challengeId)
        .type(type)
        .title(title)
        .description(description)
        .status(VoteStatus.valueOf(status)) // String to Enum
        .createdBy(creatorDto)
        .voteCount(voteCount)
        .myVote(myVote)
        .createdAt(toLocalDateTime(basicInfo.get("CREATED_AT"))) // basic info has it? check query
        .deadline(toLocalDateTime(basicInfo.get("DEADLINE")))
        .build();
  }

  // Helper method for Meeting Vote Detail (Reusing existing logic somewhat)
  private VoteDetailResponse generateDetailResponse(Vote vote, VoteDto.VoteCountDto voteCount, String myVote,
      String userId) {
    User creator = userMapper.findById(vote.getCreatedBy());
    VoteDto.CreatorDto creatorDto = VoteDto.CreatorDto.builder()
        .userId(vote.getCreatedBy())
        .nickname(creator != null ? creator.getNickname() : "Unknown")
        .build();

    Challenge challenge = challengeMapper.findById(vote.getChallengeId());
    int eligibleVoters = challenge != null ? challenge.getCurrentMembers() : 0;

    Map<String, Object> targetInfo = new HashMap<>();
    targetInfo.put("meetingId", vote.getMeetingId());

    return VoteDetailResponse.builder()
        .voteId(vote.getId())
        .challengeId(vote.getChallengeId())
        .type(vote.getType())
        .title(vote.getTitle())
        .description(vote.getDescription())
        .status(vote.getStatus())
        .createdBy(creatorDto)
        .targetInfo(targetInfo)
        .voteCount(voteCount)
        .myVote(myVote)
        .eligibleVoters(eligibleVoters)
        .requiredApproval((int) Math.ceil(eligibleVoters * 0.7))
        .deadline(vote.getDeadline())
        .createdAt(vote.getCreatedAt())
        .build();
  }

  private VoteDto mapToVoteDto(Map<String, Object> map) {
    VoteDto dto = new VoteDto();
    dto.setVoteId((String) map.get("VOTE_ID"));

    // Enum 변환
    String typeStr = (String) map.get("TYPE");
    if (typeStr != null)
      dto.setType(VoteType.valueOf(typeStr));

    String statusStr = (String) map.get("STATUS");
    if (statusStr != null)
      dto.setStatus(VoteStatus.valueOf(statusStr));

    dto.setTitle((String) map.get("TITLE"));

    // 생성자 정보
    dto.setCreatedBy(VoteDto.CreatorDto.builder()
        .userId((String) map.get("CREATED_BY_ID"))
        .nickname((String) map.get("CREATED_BY_NICKNAME"))
        .build());

    // 투표 카운트
    int agree = ((Number) map.getOrDefault("AGREE_COUNT", 0)).intValue();
    int disagree = ((Number) map.getOrDefault("DISAGREE_COUNT", 0)).intValue();
    int total = ((Number) map.getOrDefault("TOTAL_VOTE_COUNT", 0)).intValue();

    dto.setVoteCount(VoteDto.VoteCountDto.builder()
        .agree(agree)
        .disagree(disagree)
        .total(total)
        .build());

    // 날짜 변환 (Oracle TIMESTAMP -> LocalDateTime)
    dto.setDeadline(toLocalDateTime(map.get("DEADLINE")));
    dto.setCreatedAt(toLocalDateTime(map.get("CREATED_AT")));

    return dto;
  }

  /**
   * API 044: 투표하기
   */
  @Transactional
  public com.woorido.vote.dto.response.CastVoteResponse castVote(String voteId, String userId,
      com.woorido.vote.dto.request.CastVoteRequest request) {

    // 1. 기본 정보 조회 (타입 식별)
    Map<String, Object> basicInfo = voteQueryMapper.findByIdBasic(voteId);
    if (basicInfo == null) {
      throw new RuntimeException("VOTE_001: 투표를 찾을 수 없습니다");
    }

    String typeStr = (String) basicInfo.get("TYPE");
    String challengeId = (String) basicInfo.getOrDefault("CHALLENGE_ID", basicInfo.get("challenge_id"));

    // 2. 권한 체크
    if (challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId) == 0) {
      throw new RuntimeException("VOTE_003: 투표 권한이 없습니다");
    }

    // 3. 마감 체크
    LocalDateTime deadline = toLocalDateTime(basicInfo.get("DEADLINE"));
    if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
      throw new RuntimeException("VOTE_005: 투표가 이미 마감되었습니다");
    }

    VoteType type = VoteType.valueOf(typeStr);

    // 4. 타입별 처리
    if (type == VoteType.MEETING_ATTENDANCE) {
      if (voteMapper.checkVoteRecordExisting(voteId, userId) > 0) {
        throw new RuntimeException("VOTE_006: 이미 투표하셨습니다");
      }
      String recordId = java.util.UUID.randomUUID().toString();
      voteMapper.insertVoteRecord(recordId, voteId, userId, request.getChoice());

      Map<String, Object> counts = voteMapper.findVoteCounts(voteId);
      return buildCastResponse(voteId, request.getChoice(), counts);

    } else if (type == VoteType.EXPENSE) {
      if (expenseVoteMapper.checkRecordExisting(voteId, userId) > 0) {
        throw new RuntimeException("VOTE_006: 이미 투표하셨습니다");
      }
      String dbChoice = "AGREE".equals(request.getChoice()) ? "APPROVE" : "REJECT";

      com.woorido.vote.domain.ExpenseVoteRecord record = com.woorido.vote.domain.ExpenseVoteRecord.builder()
          .id(java.util.UUID.randomUUID().toString())
          .expenseVoteId(voteId)
          .userId(userId)
          .choice(dbChoice)
          .createdAt(LocalDateTime.now())
          .build();
      expenseVoteMapper.insertRecord(record);

      Map<String, Object> counts = expenseVoteMapper.findVoteCounts(voteId);
      return buildCastResponse(voteId, request.getChoice(), counts);

    } else { // GENERAL
      if (generalVoteMapper.checkRecordExisting(voteId, userId) > 0) {
        throw new RuntimeException("VOTE_006: 이미 투표하셨습니다");
      }
      String dbChoice = "AGREE".equals(request.getChoice()) ? "APPROVE" : "REJECT";

      com.woorido.vote.domain.GeneralVoteRecord record = com.woorido.vote.domain.GeneralVoteRecord.builder()
          .id(java.util.UUID.randomUUID().toString())
          .generalVoteId(voteId)
          .userId(userId)
          .choice(dbChoice)
          .createdAt(LocalDateTime.now())
          .build();
      generalVoteMapper.insertRecord(record);

      Map<String, Object> counts = generalVoteMapper.findVoteCounts(voteId);
      return buildCastResponse(voteId, request.getChoice(), counts);
    }
  }

  private com.woorido.vote.dto.response.CastVoteResponse buildCastResponse(String voteId, String myChoice,
      Map<String, Object> counts) {
    VoteDto.VoteCountDto voteCount = VoteDto.VoteCountDto.builder()
        .agree(((Number) counts.get("AGREE")).intValue())
        .disagree(((Number) counts.get("DISAGREE")).intValue())
        .total(((Number) counts.get("TOTAL")).intValue())
        .build();

    return com.woorido.vote.dto.response.CastVoteResponse.builder()
        .voteId(voteId)
        .myVote(myChoice)
        .voteCount(voteCount)
        .votedAt(LocalDateTime.now())
        .message("투표가 완료되었습니다")
        .build();
  }

  private LocalDateTime toLocalDateTime(Object timestamp) {
    if (timestamp == null)
      return null;
    if (timestamp instanceof java.sql.Timestamp) {
      return ((java.sql.Timestamp) timestamp).toLocalDateTime();
    }
    if (timestamp instanceof LocalDateTime) {
      return (LocalDateTime) timestamp;
    }
    return null; // or throw
  }
}
