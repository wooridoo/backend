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
  private final ChallengeMapper challengeMapper;
  private final UserMapper userMapper;
  private final com.woorido.meeting.repository.MeetingMapper meetingMapper; // 주입 추가
  private final com.woorido.common.util.JwtUtil jwtUtil;

  // ... (existing getVoteList, getVoteDetail) ...

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
    if (request.getDeadline().isBefore(LocalDateTime.now().plusHours(24).minusMinutes(1))) { // 약간의 오차 허용
      throw new RuntimeException("VOTE_002: 마감 시간은 최소 24시간 이후여야 합니다");
    }

    // 3. Meeting 생성 (투표 정보 저장용 Workaround)
    String meetingId = java.util.UUID.randomUUID().toString();
    com.woorido.meeting.domain.Meeting meeting = com.woorido.meeting.domain.Meeting.builder()
        .id(meetingId)
        .challengeId(challengeId)
        .title(request.getTitle())
        .description(request.getDescription())
        .location(request.getType().name()) // Type 저장
        .agenda(request.getTargetId()) // TargetId 저장
        .status("VOTE") // 투표용 Meeting임을 표시
        .scheduledAt(request.getDeadline()) // 마감 시간을 예정일로 (정렬용)
        .createdBy(userId)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    meetingMapper.insert(meeting);

    // 4. Vote 생성 (requiredCount 계산)
    Challenge challenge = challengeMapper.findById(challengeId);
    int currentMembers = challenge != null ? challenge.getCurrentMembers() : 0;
    int requiredCount = (int) Math.ceil(currentMembers * 0.7); // 70% 이상 동의 필요
    if (requiredCount < 1)
      requiredCount = 1;

    String voteId = java.util.UUID.randomUUID().toString();
    com.woorido.vote.domain.Vote vote = com.woorido.vote.domain.Vote.builder()
        .id(voteId)
        .challengeId(challengeId)
        .meetingId(meetingId)
        .type(request.getType())
        .status(VoteStatus.IN_PROGRESS)
        .createdBy(userId)
        .deadline(request.getDeadline())
        .createdAt(LocalDateTime.now())
        .requiredCount(requiredCount)
        .build();
    voteMapper.insert(vote);

    // 5. 응답 DTO 생성 (Map 변환보다는 직접 빌드)
    // 작성자 닉네임 조회
    com.woorido.common.entity.User creator = userMapper.findById(userId);
    String nickname = creator != null ? creator.getNickname() : "Unknown";

    return VoteDto.builder()
        .voteId(voteId)
        .type(vote.getType())
        .title(meeting.getTitle())
        .status(vote.getStatus())
        .createdBy(VoteDto.CreatorDto.builder().userId(userId).nickname(nickname).build())
        .voteCount(VoteDto.VoteCountDto.builder().agree(0).disagree(0).total(0).build())
        .deadline(vote.getDeadline())
        .createdAt(vote.getCreatedAt())
        .build();
  }

  /**
   * API 041: 투표 목록 조회
   */
  @Transactional(readOnly = true)
  public VoteListResponse getVoteList(String challengeId, String userId, String status, String type, int page,
      int size) {
    // 1. 챌린지 멤버 권한 체크
    if (challengeMapper.countMemberByChallengeIdAndUserId(challengeId, userId) == 0) {
      throw new RuntimeException("CHALLENGE_003: 챌린지 멤버가 아닙니다");
    }

    // 2. 데이터 조회
    int offset = page * size;
    List<Map<String, Object>> resultList = voteMapper.findAllByChallengeIdWithFilter(challengeId, status, type, offset,
        size);
    long totalElements = voteMapper.countAllByChallengeIdWithFilter(challengeId, status, type);

    // 3. DTO 변환
    List<VoteDto> content = resultList.stream().map(this::mapToVoteDto).collect(Collectors.toList());

    // 4. 페이지 정보 생성
    int totalPages = (int) Math.ceil((double) totalElements / size);
    PageInfo pageInfo = PageInfo.builder()
        .number(page)
        .size(size)
        .totalElements(totalElements)
        .totalPages(totalPages)
        .build();

    return VoteListResponse.builder()
        .content(content)
        .page(pageInfo)
        .build();
  }

  /**
   * API 042: 투표 상세 조회
   */
  @Transactional(readOnly = true)
  public VoteDetailResponse getVoteDetail(String voteId, String userId) {
    // 1. 투표 조회
    Vote vote = voteMapper.findById(voteId);
    if (vote == null) {
      throw new RuntimeException("VOTE_001: 투표를 찾을 수 없습니다");
    }

    // 2. 권한 체크 (챌린지 멤버)
    if (challengeMapper.countMemberByChallengeIdAndUserId(vote.getChallengeId(), userId) == 0) {
      throw new RuntimeException("VOTE_003: 투표 조회 권한이 없습니다");
    }

    // 3. 생성자 정보 조회
    User creator = userMapper.findById(vote.getCreatedBy());
    VoteDto.CreatorDto creatorDto = VoteDto.CreatorDto.builder()
        .userId(vote.getCreatedBy())
        .nickname(creator != null ? creator.getNickname() : "Unknown")
        .build();

    // 4. 내 투표 여부
    String myVote = voteMapper.findMyVote(voteId, userId);

    // 5. 투표 현황 집계
    Map<String, Object> counts = voteMapper.findVoteCounts(voteId);
    VoteDto.VoteCountDto voteCount = VoteDto.VoteCountDto.builder()
        .agree(((Number) counts.get("AGREE")).intValue())
        .disagree(((Number) counts.get("DISAGREE")).intValue())
        .total(((Number) counts.get("TOTAL")).intValue())
        .build();

    // 6. 챌린지 정보 (유권자 수)
    Challenge challenge = challengeMapper.findById(vote.getChallengeId());
    int eligibleVoters = challenge != null ? challenge.getCurrentMembers() : 0;

    // 7. Target Info 구성
    Map<String, Object> targetInfo = new HashMap<>();
    if (VoteType.MEETING_ATTENDANCE.equals(vote.getType())) {
      targetInfo.put("meetingId", vote.getMeetingId());
      // 필요하다면 Meeting 정보 추가 (MeetingMapper 이용)
    } else {
      targetInfo.put("targetId", vote.getTargetId());
    }

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
        .requiredApproval((int) Math.ceil(eligibleVoters * 0.7)) // 예시: 70%
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
    // 1. 투표 조회
    Vote vote = voteMapper.findById(voteId);
    if (vote == null) {
      throw new RuntimeException("VOTE_001: 투표를 찾을 수 없습니다");
    }

    // 2. 권한 체크 (챌린지 멤버)
    if (challengeMapper.countMemberByChallengeIdAndUserId(vote.getChallengeId(), userId) == 0) {
      throw new RuntimeException("VOTE_003: 투표 권한이 없습니다 (챌린지 멤버 아님)");
    }

    // 3. 마감 체크
    if (vote.getDeadline() != null && LocalDateTime.now().isAfter(vote.getDeadline())) {
      throw new RuntimeException("VOTE_005: 투표가 이미 마감되었습니다");
    }

    // 4. 중복 투표 체크
    if (voteMapper.checkVoteRecordExisting(voteId, userId) > 0) {
      throw new RuntimeException("VOTE_006: 이미 투표하셨습니다");
    }

    // 5. 투표 기록 저장
    String recordId = java.util.UUID.randomUUID().toString();
    voteMapper.insertVoteRecord(recordId, voteId, userId, request.getChoice());

    // 6. 최신 집계 조회
    Map<String, Object> counts = voteMapper.findVoteCounts(voteId);
    VoteDto.VoteCountDto voteCount = VoteDto.VoteCountDto.builder()
        .agree(((Number) counts.get("AGREE")).intValue())
        .disagree(((Number) counts.get("DISAGREE")).intValue())
        .total(((Number) counts.get("TOTAL")).intValue())
        .build();

    return com.woorido.vote.dto.response.CastVoteResponse.builder()
        .voteId(voteId)
        .myVote(request.getChoice())
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
