package com.woorido.vote.service;

import com.woorido.challenge.domain.Challenge;
import com.woorido.challenge.domain.LeaveReason;
import com.woorido.challenge.repository.ChallengeMapper;
import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.challenge.service.ChallengeService;
import com.woorido.common.dto.PageInfo;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.expense.domain.ExpenseRequest;
import com.woorido.expense.domain.PaymentBarcode;
import com.woorido.expense.repository.ExpenseRequestMapper;
import com.woorido.expense.repository.PaymentBarcodeMapper;
import com.woorido.meeting.domain.Meeting;
import com.woorido.meeting.repository.MeetingMapper;
import com.woorido.vote.domain.ExpenseVote;
import com.woorido.vote.domain.ExpenseVoteRecord;
import com.woorido.vote.domain.GeneralVote;
import com.woorido.vote.domain.GeneralVoteRecord;
import com.woorido.vote.domain.GeneralVoteType;
import com.woorido.vote.domain.Vote;
import com.woorido.vote.domain.Vote.VoteStatus;
import com.woorido.vote.domain.Vote.VoteType;
import com.woorido.vote.dto.VoteDto;
import com.woorido.vote.dto.request.CastVoteRequest;
import com.woorido.vote.dto.request.CreateVoteRequest;
import com.woorido.vote.dto.response.CastVoteResponse;
import com.woorido.vote.dto.response.VoteDetailResponse;
import com.woorido.vote.dto.response.VoteListResponse;
import com.woorido.vote.dto.response.VoteResultResponse;
import com.woorido.vote.repository.ExpenseVoteMapper;
import com.woorido.vote.repository.GeneralVoteMapper;
import com.woorido.vote.repository.VoteMapper;
import com.woorido.vote.repository.VoteQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {
  // Learning note:
  // - Read flow as: validate auth/role -> execute domain logic -> persist via Mapper.

  private final VoteMapper voteMapper;
  private final ExpenseVoteMapper expenseVoteMapper;
  private final GeneralVoteMapper generalVoteMapper;
  private final VoteQueryMapper voteQueryMapper;
  private final ChallengeMapper challengeMapper;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final UserMapper userMapper;
  private final MeetingMapper meetingMapper;
  private final ChallengeService challengeService;
  private final ExpenseRequestMapper expenseRequestMapper;
  private final PaymentBarcodeMapper paymentBarcodeMapper;

  @Transactional
  // [학습] 투표를 생성하고 정족수를 계산한다.
  public VoteDto createVote(String challengeId, String userId, CreateVoteRequest request) {
    requireChallengeExists(challengeId);
    Map<String, Object> memberInfo = requireMemberAny(challengeId, userId);

    if (request == null || request.getType() == null) {
      throw new RuntimeException("VOTE_004:투표 유형이 필요합니다");
    }
    if (request.getDeadline() == null) {
      throw new RuntimeException("VOTE_002:마감 일시가 필요합니다");
    }
    if (request.getDeadline().isBefore(LocalDateTime.now().plusHours(24))) {
      throw new RuntimeException("VOTE_002:마감 일시는 최소 24시간 이후여야 합니다");
    }

    VoteType type = request.getType();
    String role = asString(memberInfo.get("ROLE"));
    String privilegeStatus = asString(memberInfo.get("STATUS"));
    String targetUserId = resolveGeneralVoteTargetId(type, challengeId, request.getTargetId());

    validateSupportedVoteType(type);
    validateProposerPermission(type, role, userId, targetUserId);
    validateRevokedPolicy(type, request.getMeetingId(), privilegeStatus);
    validateKickTarget(type, challengeId, targetUserId);
    validateLeaderKickPolicy(type, challengeId, targetUserId);

    int activeMemberCount = challengeMemberMapper.findAllActiveMembers(challengeId).size();
    if (activeMemberCount < 1) {
      throw new RuntimeException("VOTE_009:투표 가능 인원이 없습니다");
    }

    String voteId = UUID.randomUUID().toString();
    LocalDateTime now = LocalDateTime.now();

    User creator = userMapper.findById(userId);
    VoteDto.CreatorDto creatorDto = VoteDto.CreatorDto.builder()
        .userId(userId)
        .nickname(creator != null ? creator.getNickname() : "Unknown")
        .build();

    int eligibleCount;
    int requiredCount;

    if (type == VoteType.MEETING_ATTENDANCE) {
      eligibleCount = activeMemberCount;
      requiredCount = calculateRequiredCount(type, eligibleCount);

      String meetingId = UUID.randomUUID().toString();
      Meeting meeting = Meeting.builder()
          .id(meetingId)
          .challengeId(challengeId)
          .title(request.getTitle())
          .description(request.getDescription())
          .location(type.name())
          .status("VOTE")
          .meetingDate(request.getDeadline())
          .createdBy(userId)
          .createdAt(now)
          .updatedAt(now)
          .build();
      meetingMapper.insert(meeting);

      Vote vote = Vote.builder()
          .id(voteId)
          .challengeId(challengeId)
          .meetingId(meetingId)
          .type(type)
          .status(VoteStatus.PENDING)
          .createdBy(userId)
          .deadline(request.getDeadline())
          .createdAt(now)
          .requiredCount(requiredCount)
          .build();
      voteMapper.insert(vote);

    } else if (type == VoteType.EXPENSE) {
      eligibleCount = calculateExpenseEligibleCount(challengeId, request.getMeetingId());
      requiredCount = calculateRequiredCount(type, eligibleCount);

      String expenseRequestId = UUID.randomUUID().toString();
      ExpenseRequest expenseRequest = ExpenseRequest.builder()
          .id(expenseRequestId)
          .meetingId(request.getMeetingId())
          .createdBy(userId)
          .title(request.getTitle())
          .amount(request.getAmount() != null ? request.getAmount() : 0L)
          .description(request.getDescription())
          .receiptUrl(request.getReceiptUrl())
          .status("VOTING")
          .createdAt(now)
          .build();
      expenseRequestMapper.insert(expenseRequest);

      ExpenseVote expenseVote = ExpenseVote.builder()
          .id(voteId)
          .expenseRequestId(expenseRequestId)
          .eligibleCount(eligibleCount)
          .requiredCount(requiredCount)
          .status(VoteStatus.PENDING)
          .createdAt(now)
          .expiresAt(request.getDeadline())
          .build();
      expenseVoteMapper.insert(expenseVote);

    } else {
      eligibleCount = calculateGeneralEligibleCount(type, challengeId, activeMemberCount, targetUserId);
      requiredCount = calculateRequiredCount(type, eligibleCount);

      GeneralVote generalVote = GeneralVote.builder()
          .id(voteId)
          .challengeId(challengeId)
          .createdBy(userId)
          .type(toGeneralVoteType(type))
          .title(request.getTitle())
          .description(request.getDescription())
          .targetUserId(targetUserId)
          .requiredCount(requiredCount)
          .eligibleCount(eligibleCount)
          .status(VoteStatus.PENDING)
          .createdAt(now)
          .expiresAt(request.getDeadline())
          .build();
      generalVoteMapper.insert(generalVote);
    }

    return VoteDto.builder()
        .voteId(voteId)
        .type(type)
        .title(request.getTitle())
        .status(VoteStatus.PENDING)
        .createdBy(creatorDto)
        .voteCount(VoteDto.VoteCountDto.builder().agree(0).disagree(0).total(0).build())
        .deadline(request.getDeadline())
        .createdAt(now)
        .build();
  }

  @Transactional(readOnly = true)
  // [학습] 투표 목록/카운트를 필터 조건으로 조회한다.
  public VoteListResponse getVoteList(String challengeId, String userId, String status, String type, int page, int size) {
    requireChallengeExists(challengeId);
    requireMemberAny(challengeId, userId);

    int offset = page * size;
    String normalizedStatus = normalizeStatusFilter(status);
    String normalizedType = normalizeTypeFilter(type);

    List<Map<String, Object>> resultList = voteQueryMapper.findAllUnionByChallengeId(
        challengeId,
        normalizedStatus,
        normalizedType,
        offset,
        size);
    long totalElements = voteQueryMapper.countAllUnionByChallengeId(challengeId, normalizedStatus, normalizedType);

    List<VoteDto> content = resultList.stream()
        .map(this::mapToVoteDto)
        .collect(Collectors.toList());

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

  @Transactional(readOnly = true)
  // [학습] 투표 상세와 내 투표 여부를 조회한다.
  public VoteDetailResponse getVoteDetail(String voteId, String userId) {
    Map<String, Object> basicInfo = voteQueryMapper.findByIdBasic(voteId);
    if (basicInfo == null) {
      throw new RuntimeException("VOTE_001:투표를 찾을 수 없습니다");
    }

    String challengeId = asString(basicInfo.get("CHALLENGE_ID"));
    requireMemberAny(challengeId, userId);

    VoteType type = parseVoteType(asString(basicInfo.get("TYPE")));
    VoteStatus status = parseVoteStatus(asString(basicInfo.get("STATUS")));

    int eligibleVoters = toInt(basicInfo.get("ELIGIBLE_COUNT"));
    int requiredApproval = toInt(basicInfo.get("REQUIRED_COUNT"));
    LocalDateTime createdAt = toLocalDateTime(basicInfo.get("CREATED_AT"));
    LocalDateTime deadline = toLocalDateTime(basicInfo.get("DEADLINE"));

    String creatorId;
    String title;
    String description;
    String myVote;
    VoteDto.VoteCountDto voteCount;
    Map<String, Object> targetInfo = new HashMap<>();

    if (type == VoteType.MEETING_ATTENDANCE) {
      Vote vote = voteMapper.findById(voteId);
      if (vote == null) {
        throw new RuntimeException("VOTE_001:투표를 찾을 수 없습니다");
      }
      creatorId = vote.getCreatedBy();
      title = vote.getTitle();
      description = vote.getDescription();
      targetInfo.put("meetingId", vote.getMeetingId());
      myVote = normalizeMeetingChoice(voteMapper.findMyVote(voteId, userId));

      Map<String, Object> counts = voteMapper.findVoteCounts(voteId);
      voteCount = toVoteCount(counts);

    } else if (type == VoteType.EXPENSE) {
      ExpenseVote vote = expenseVoteMapper.findById(voteId);
      if (vote == null) {
        throw new RuntimeException("VOTE_001:투표를 찾을 수 없습니다");
      }
      ExpenseRequest expenseRequest = expenseRequestMapper.findById(vote.getExpenseRequestId());

      creatorId = expenseRequest != null ? expenseRequest.getCreatedBy() : null;
      title = expenseRequest != null ? expenseRequest.getTitle() : null;
      description = expenseRequest != null ? expenseRequest.getDescription() : null;
      if (expenseRequest != null) {
        targetInfo.put("meetingId", expenseRequest.getMeetingId());
        targetInfo.put("amount", expenseRequest.getAmount());
        targetInfo.put("receiptUrl", expenseRequest.getReceiptUrl());
        targetInfo.put("expenseRequestStatus", expenseRequest.getStatus());

        PaymentBarcode barcode = paymentBarcodeMapper.findByExpenseRequestId(expenseRequest.getId());
        if (barcode != null) {
          targetInfo.put("barcodeId", barcode.getId());
          targetInfo.put("barcodeNumber", barcode.getBarcodeNumber());
          targetInfo.put("barcodeStatus", barcode.getStatus());
          targetInfo.put("barcodeExpiresAt", barcode.getExpiresAt());
        }
      }
      myVote = normalizeExpenseChoice(expenseVoteMapper.findMyVote(voteId, userId));

      Map<String, Object> counts = expenseVoteMapper.findVoteCounts(voteId);
      voteCount = toVoteCount(counts);

      if (eligibleVoters == 0 && vote.getEligibleCount() != null) {
        eligibleVoters = vote.getEligibleCount();
      }
      if (requiredApproval == 0 && vote.getRequiredCount() != null) {
        requiredApproval = vote.getRequiredCount();
      }

    } else {
      GeneralVote vote = generalVoteMapper.findById(voteId);
      if (vote == null) {
        throw new RuntimeException("VOTE_001:투표를 찾을 수 없습니다");
      }
      creatorId = vote.getCreatedBy();
      title = vote.getTitle();
      description = vote.getDescription();
      targetInfo.put("targetId", vote.getTargetUserId());
      myVote = normalizeGeneralChoice(generalVoteMapper.findMyVote(voteId, userId));

      Map<String, Object> counts = generalVoteMapper.findVoteCounts(voteId);
      voteCount = toVoteCount(counts);

      if (eligibleVoters == 0 && vote.getEligibleCount() != null) {
        eligibleVoters = vote.getEligibleCount();
      }
      if (requiredApproval == 0 && vote.getRequiredCount() != null) {
        requiredApproval = vote.getRequiredCount();
      }
    }

    User creator = creatorId != null ? userMapper.findById(creatorId) : null;
    VoteDto.CreatorDto creatorDto = VoteDto.CreatorDto.builder()
        .userId(creatorId)
        .nickname(creator != null ? creator.getNickname() : "Unknown")
        .build();

    return VoteDetailResponse.builder()
        .voteId(voteId)
        .challengeId(challengeId)
        .type(type)
        .title(title)
        .description(description)
        .status(status)
        .createdBy(creatorDto)
        .targetInfo(targetInfo)
        .voteCount(voteCount)
        .myVote(myVote)
        .eligibleVoters(eligibleVoters)
        .requiredApproval(requiredApproval)
        .deadline(deadline)
        .createdAt(createdAt)
        .build();
  }

  @Transactional(readOnly = true)
  public VoteResultResponse getVoteResult(String voteId, String userId) {
    VoteDetailResponse detail = getVoteDetail(voteId, userId);
    if (detail.getStatus() == VoteStatus.PENDING) {
      throw new RuntimeException("VOTE_007:아직 진행 중인 투표입니다");
    }

    int agree = detail.getVoteCount() != null ? detail.getVoteCount().getAgree() : 0;
    int disagree = detail.getVoteCount() != null ? detail.getVoteCount().getDisagree() : 0;
    int total = detail.getVoteCount() != null ? detail.getVoteCount().getTotal() : 0;
    int requiredApproval = detail.getRequiredApproval();

    double approvalRate = total > 0 ? (double) agree / total * 100.0 : 0.0;
    boolean passed = detail.getStatus() == VoteStatus.APPROVED;

    return VoteResultResponse.builder()
        .voteId(detail.getVoteId())
        .type(detail.getType())
        .status(detail.getStatus())
        .voteCount(VoteResultResponse.VoteCount.builder()
            .agree(agree)
            .disagree(disagree)
            .total(total)
            .build())
        .eligibleVoters(detail.getEligibleVoters())
        .requiredApproval(requiredApproval)
        .passed(passed)
        .approvalRate(approvalRate)
        .build();
  }

  @Transactional
  // [학습] 투표를 행사하고 결과 상태를 갱신한다.
  public CastVoteResponse castVote(String voteId, String userId, CastVoteRequest request) {
    Map<String, Object> basicInfo = voteQueryMapper.findByIdBasic(voteId);
    if (basicInfo == null) {
      throw new RuntimeException("VOTE_001:투표를 찾을 수 없습니다");
    }

    String challengeId = asString(basicInfo.get("CHALLENGE_ID"));
    Map<String, Object> memberInfo = requireMemberAny(challengeId, userId);

    VoteType type = parseVoteType(asString(basicInfo.get("TYPE")));
    VoteStatus status = parseVoteStatus(asString(basicInfo.get("STATUS")));
    LocalDateTime deadline = toLocalDateTime(basicInfo.get("DEADLINE"));

    boolean alreadyVoted;
    if (type == VoteType.MEETING_ATTENDANCE) {
      alreadyVoted = voteMapper.checkVoteRecordExisting(voteId, userId) > 0;
    } else if (type == VoteType.EXPENSE) {
      alreadyVoted = expenseVoteMapper.checkRecordExisting(voteId, userId) > 0;
    } else {
      alreadyVoted = generalVoteMapper.checkRecordExisting(voteId, userId) > 0;
    }
    if (alreadyVoted) {
      throw new RuntimeException("VOTE_006:이미 투표했습니다");
    }

    if (!isCastableStatus(status)) {
      throw new RuntimeException("VOTE_005:투표가 이미 종료되었습니다");
    }
    if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
      expireVote(voteId, type);
      throw new RuntimeException("VOTE_005:투표가 이미 종료되었습니다");
    }

    String choice = normalizeRequestChoice(request);

    if (type == VoteType.MEETING_ATTENDANCE) {
      validateRevokedPolicy(type, asString(basicInfo.get("MEETING_ID")), asString(memberInfo.get("STATUS")));

      if (voteMapper.checkVoteRecordExisting(voteId, userId) > 0) {
        throw new RuntimeException("VOTE_006:이미 투표했습니다");
      }

      voteMapper.insertVoteRecord(UUID.randomUUID().toString(), voteId, userId, choice);

      Map<String, Object> counts = voteMapper.findVoteCounts(voteId);
      int eligibleCount = toInt(basicInfo.get("ELIGIBLE_COUNT"));
      int requiredCount = toInt(basicInfo.get("REQUIRED_COUNT"));
      updateVoteStatusByCounts(voteId, counts, eligibleCount, requiredCount, voteMapper::updateStatus);

      return buildCastResponse(voteId, choice, counts);
    }

    if (type == VoteType.EXPENSE) {
      if (expenseVoteMapper.checkRecordExisting(voteId, userId) > 0) {
        throw new RuntimeException("VOTE_006:이미 투표했습니다");
      }

      ExpenseVote expenseVote = expenseVoteMapper.findById(voteId);
      if (expenseVote == null) {
        throw new RuntimeException("VOTE_001:투표를 찾을 수 없습니다");
      }

      ExpenseRequest expenseRequest = expenseRequestMapper.findById(expenseVote.getExpenseRequestId());
      if (expenseRequest == null) {
        throw new RuntimeException("VOTE_001:투표를 찾을 수 없습니다");
      }

      validateRevokedPolicy(type, expenseRequest.getMeetingId(), asString(memberInfo.get("STATUS")));

      if (hasText(expenseRequest.getMeetingId())) {
        int attendee = meetingMapper.isActualAttendee(expenseRequest.getMeetingId(), userId);
        if (attendee == 0) {
          throw new RuntimeException("VOTE_007:해당 모임 참석자만 투표할 수 있습니다");
        }
      }

      ExpenseVoteRecord record = ExpenseVoteRecord.builder()
          .id(UUID.randomUUID().toString())
          .expenseVoteId(voteId)
          .userId(userId)
          .choice("AGREE".equals(choice) ? "APPROVE" : "REJECT")
          .createdAt(LocalDateTime.now())
          .build();
      expenseVoteMapper.insertRecord(record);

      Map<String, Object> counts = expenseVoteMapper.findVoteCounts(voteId);
      int eligibleCount = expenseVote.getEligibleCount() != null ? expenseVote.getEligibleCount() : toInt(basicInfo.get("ELIGIBLE_COUNT"));
      int requiredCount = expenseVote.getRequiredCount() != null ? expenseVote.getRequiredCount() : toInt(basicInfo.get("REQUIRED_COUNT"));
      VoteStatus finalizedStatus = determineVoteStatusByCounts(counts, eligibleCount, requiredCount);
      if (finalizedStatus != null) {
        expenseVoteMapper.updateStatus(voteId, finalizedStatus.name());
        finalizeExpenseVote(challengeId, expenseRequest, finalizedStatus);
      }

      return buildCastResponse(voteId, choice, counts);
    }

    if (generalVoteMapper.checkRecordExisting(voteId, userId) > 0) {
      throw new RuntimeException("VOTE_006:이미 투표했습니다");
    }

    GeneralVote generalVote = generalVoteMapper.findById(voteId);
    if (generalVote == null) {
      throw new RuntimeException("VOTE_001:투표를 찾을 수 없습니다");
    }
    if ((type == VoteType.KICK || type == VoteType.LEADER_KICK)
        && hasText(generalVote.getTargetUserId())
        && generalVote.getTargetUserId().equals(userId)) {
      throw new RuntimeException("VOTE_007:강퇴 대상자는 투표할 수 없습니다");
    }

    GeneralVoteRecord record = GeneralVoteRecord.builder()
        .id(UUID.randomUUID().toString())
        .generalVoteId(voteId)
        .userId(userId)
        .choice("AGREE".equals(choice) ? "APPROVE" : "REJECT")
        .createdAt(LocalDateTime.now())
        .build();
    generalVoteMapper.insertRecord(record);

    Map<String, Object> counts = generalVoteMapper.findVoteCounts(voteId);

    int agree = toInt(counts.get("AGREE"));
    int disagree = toInt(counts.get("DISAGREE"));
    int eligibleCount = generalVote.getEligibleCount() != null ? generalVote.getEligibleCount() : toInt(basicInfo.get("ELIGIBLE_COUNT"));
    int requiredCount = generalVote.getRequiredCount() != null ? generalVote.getRequiredCount() : toInt(basicInfo.get("REQUIRED_COUNT"));

    boolean approved = false;
    if (type == VoteType.DISSOLVE) {
      if (disagree > 0) {
        generalVoteMapper.updateStatus(voteId, VoteStatus.REJECTED.name());
      } else if (agree >= requiredCount) {
        generalVoteMapper.updateStatus(voteId, VoteStatus.APPROVED.name());
        approved = true;
        challengeService.dissolveChallenge(challengeId);
      }
    } else {
      if (agree >= requiredCount) {
        generalVoteMapper.updateStatus(voteId, VoteStatus.APPROVED.name());
        approved = true;
      } else if (disagree > eligibleCount - requiredCount) {
        generalVoteMapper.updateStatus(voteId, VoteStatus.REJECTED.name());
      }
    }

    if (approved && type == VoteType.KICK) {
      executeMemberKickIfApproved(challengeId, generalVote.getTargetUserId());
    }
    if (approved && type == VoteType.LEADER_KICK) {
      executeLeaderKickIfApproved(challengeId, generalVote.getTargetUserId());
    }

    return buildCastResponse(voteId, choice, counts);
  }

  // [학습] 지출 승인 투표의 투표 가능 인원을 계산한다.
  private int calculateExpenseEligibleCount(String challengeId, String meetingId) {
    if (!hasText(meetingId)) {
      throw new RuntimeException("VOTE_004:지출 승인 투표는 모임 선택이 필요합니다");
    }

    Map<String, Object> meeting = meetingMapper.findById(meetingId);
    if (meeting == null) {
      throw new RuntimeException("MEETING_001:모임을 찾을 수 없습니다");
    }
    String meetingChallengeId = asString(meeting.get("CHALLENGE_ID"));
    if (!challengeId.equals(meetingChallengeId)) {
      throw new RuntimeException("MEETING_001:모임을 찾을 수 없습니다");
    }

    int attendeeCount = meetingMapper.countActualAttendees(meetingId);
    if (attendeeCount <= 0) {
      throw new RuntimeException("VOTE_009:모임 참석자가 없어 지출 투표를 생성할 수 없습니다");
    }
    return attendeeCount;
  }

  // [학습] 일반 투표 유형의 투표 가능 인원을 계산한다.
  private int calculateGeneralEligibleCount(VoteType type, String challengeId, int activeMemberCount, String targetUserId) {
    if ((type == VoteType.KICK || type == VoteType.LEADER_KICK) && hasText(targetUserId)) {
      Map<String, Object> targetMember = challengeMemberMapper.findByUserIdAndChallengeId(targetUserId, challengeId);
      boolean targetActive = targetMember != null && "ACTIVE".equals(asString(targetMember.get("STATUS")));
      if (targetActive) {
        return Math.max(1, activeMemberCount - 1);
      }
    }
    return activeMemberCount;
  }

  // [학습] 투표 유형별 가결 정족수를 계산한다.
  private int calculateRequiredCount(VoteType type, int eligibleCount) {
    int safeEligible = Math.max(1, eligibleCount);

    if (type == VoteType.MEETING_ATTENDANCE || type == VoteType.EXPENSE) {
      return safeEligible / 2 + 1;
    }
    if (type == VoteType.KICK) {
      return Math.max(1, (int) Math.ceil(safeEligible * 0.7));
    }
    if (type == VoteType.LEADER_KICK) {
      return Math.max(1, (int) Math.ceil(safeEligible * 0.5));
    }
    if (type == VoteType.DISSOLVE) {
      return safeEligible;
    }
    return Math.max(1, (int) Math.ceil(safeEligible * 0.7));
  }

  // [학습] 투표 발의자의 권한 정책을 검증한다.
  private void validateProposerPermission(VoteType type, String role, String userId, String targetUserId) {
    boolean isLeader = "LEADER".equals(role);

    if (type == VoteType.KICK) {
      if (!isLeader) {
        throw new RuntimeException("VOTE_003:투표 생성 권한이 없습니다");
      }
      if (!hasText(targetUserId)) {
        throw new RuntimeException("VOTE_004:퇴출 대상이 필요합니다");
      }
      if (userId.equals(targetUserId)) {
        throw new RuntimeException("VOTE_003:대상자는 해당 투표를 발의할 수 없습니다");
      }
      return;
    }

    if (type == VoteType.LEADER_KICK) {
      if (isLeader) {
        throw new RuntimeException("VOTE_003:리더는 리더 강퇴 투표를 발의할 수 없습니다");
      }
      return;
    }

    if (type == VoteType.EXPENSE || type == VoteType.MEETING_ATTENDANCE || type == VoteType.DISSOLVE) {
      if (!isLeader) {
        throw new RuntimeException("VOTE_003:투표 생성 권한이 없습니다");
      }
      return;
    }
  }

  // [학습] 권한 박탈(REVOKED) 사용자 정책을 검증한다.
  private String resolveGeneralVoteTargetId(VoteType type, String challengeId, String targetUserId) {
    if (type != VoteType.KICK && type != VoteType.LEADER_KICK) {
      return targetUserId;
    }
    if (hasText(targetUserId)) {
      return targetUserId;
    }

    if (type == VoteType.LEADER_KICK) {
      List<Map<String, Object>> activeMembers = challengeMemberMapper.findAllActiveMembers(challengeId);
      for (Map<String, Object> member : activeMembers) {
        if ("LEADER".equals(asString(member.get("ROLE")))) {
          return asString(member.get("USER_ID"));
        }
      }
    }
    return targetUserId;
  }

  private void validateKickTarget(VoteType type, String challengeId, String targetUserId) {
    if (type != VoteType.KICK && type != VoteType.LEADER_KICK) {
      return;
    }
    if (!hasText(targetUserId)) {
      throw new RuntimeException("VOTE_004:퇴출 대상이 필요합니다");
    }

    Map<String, Object> targetMember = challengeMemberMapper.findByUserIdAndChallengeId(targetUserId, challengeId);
    if (targetMember == null || !"ACTIVE".equals(asString(targetMember.get("STATUS")))) {
      throw new RuntimeException("VOTE_004:퇴출 대상이 유효하지 않습니다");
    }
    if (type == VoteType.KICK && "LEADER".equals(asString(targetMember.get("ROLE")))) {
      throw new RuntimeException("VOTE_004:일반 강퇴 투표 대상은 리더가 될 수 없습니다");
    }
    if (type == VoteType.LEADER_KICK && !"LEADER".equals(asString(targetMember.get("ROLE")))) {
      throw new RuntimeException("VOTE_004:리더 강퇴는 리더를 대상으로 해야 합니다");
    }
  }

  private void validateLeaderKickPolicy(VoteType type, String challengeId, String targetLeaderUserId) {
    if (type != VoteType.LEADER_KICK) {
      return;
    }
    LocalDateTime since = LocalDateTime.now().minusDays(30);
    Challenge challenge = challengeMapper.findById(challengeId);
    LocalDateTime lastActiveAt = challenge != null ? challenge.getLeaderLastActiveAt() : null;

    if (lastActiveAt != null && !lastActiveAt.isBefore(since)) {
      throw new RuntimeException("VOTE_010:최근 30일 내 리더 활동이 있어 리더 강퇴 투표를 생성할 수 없습니다");
    }
    if (lastActiveAt == null) {
      int recentMeetings = meetingMapper.countCompletedMeetingsSince(challengeId, targetLeaderUserId, since);
      int recentExpenses = expenseRequestMapper.countApprovedExpensesSince(challengeId, targetLeaderUserId, since);
      if (recentMeetings > 0 || recentExpenses > 0) {
        throw new RuntimeException("VOTE_010:최근 30일 내 리더 활동이 있어 리더 강퇴 투표를 생성할 수 없습니다");
      }
    }

    Map<String, Object> candidate = challengeMemberMapper.findTopBrixActiveMemberExcludingUser(challengeId, targetLeaderUserId);
    if (candidate == null) {
      throw new RuntimeException("VOTE_009:리더 승계 가능한 멤버가 없습니다");
    }
  }

  private void validateRevokedPolicy(VoteType type, String meetingId, String privilegeStatus) {
    boolean meetingRelated = type == VoteType.MEETING_ATTENDANCE || (type == VoteType.EXPENSE && hasText(meetingId));
    if (meetingRelated && "REVOKED".equals(privilegeStatus)) {
      throw new RuntimeException("VOTE_008:권한 박탈 상태에서는 모임 관련 투표에 참여할 수 없습니다");
    }
  }

  // [학습] 챌린지 멤버 여부를 검증한다(LEFT 제외).
  private Map<String, Object> requireMemberAny(String challengeId, String userId) {
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null || "LEFT".equals(asString(memberInfo.get("STATUS")))) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }
    return memberInfo;
  }

  // [학습] 챌린지 존재 여부를 검증한다.
  private void requireChallengeExists(String challengeId) {
    Challenge challenge = challengeMapper.findById(challengeId);
    if (challenge == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }
  }

  // [학습] 조회 결과 Map을 VoteDto로 변환한다.
  private void finalizeExpenseVote(String challengeId, ExpenseRequest expenseRequest, VoteStatus status) {
    if (status == VoteStatus.APPROVED) {
      expenseRequestMapper.updateStatus(expenseRequest.getId(), "APPROVED", LocalDateTime.now());
      challengeMapper.touchLeaderLastActiveAt(challengeId, expenseRequest.getCreatedBy());
      issueBarcodeIfAbsent(challengeId, expenseRequest);
      return;
    }
    if (status == VoteStatus.REJECTED) {
      expenseRequestMapper.updateStatus(expenseRequest.getId(), "REJECTED", null);
    }
  }

  private void issueBarcodeIfAbsent(String challengeId, ExpenseRequest expenseRequest) {
    PaymentBarcode existing = paymentBarcodeMapper.findByExpenseRequestId(expenseRequest.getId());
    if (existing != null) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    PaymentBarcode barcode = PaymentBarcode.builder()
        .id(UUID.randomUUID().toString())
        .expenseRequestId(expenseRequest.getId())
        .challengeId(challengeId)
        .barcodeNumber(generateBarcodeNumber())
        .amount(expenseRequest.getAmount() != null ? expenseRequest.getAmount() : 0L)
        .status("ACTIVE")
        .expiresAt(now.plusMinutes(10))
        .createdAt(now)
        .build();
    paymentBarcodeMapper.insert(barcode);
  }

  private String generateBarcodeNumber() {
    String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    return "WD-" + random;
  }

  private void executeMemberKickIfApproved(String challengeId, String targetUserId) {
    if (!hasText(targetUserId)) {
      return;
    }
    int updated = challengeMemberMapper.updateLeaveMember(targetUserId, challengeId, LeaveReason.KICKED.name());
    if (updated > 0) {
      challengeMapper.decrementCurrentMembers(challengeId);
    }
  }

  private void executeLeaderKickIfApproved(String challengeId, String leaderUserId) {
    if (!hasText(leaderUserId)) {
      return;
    }
    Map<String, Object> leaderMember = challengeMemberMapper.findByUserIdAndChallengeId(leaderUserId, challengeId);
    if (leaderMember == null) {
      return;
    }
    if (!"ACTIVE".equals(asString(leaderMember.get("STATUS"))) || !"LEADER".equals(asString(leaderMember.get("ROLE")))) {
      return;
    }
    Map<String, Object> nextLeader = challengeMemberMapper.findTopBrixActiveMemberExcludingUser(challengeId, leaderUserId);
    if (nextLeader == null) {
      return;
    }

    String leaderMemberId = asString(leaderMember.get("MEMBER_ID"));
    String nextLeaderMemberId = asString(nextLeader.get("MEMBER_ID"));
    if (!hasText(leaderMemberId) || !hasText(nextLeaderMemberId)) {
      return;
    }

    challengeMemberMapper.updateRole("FOLLOWER", leaderMemberId, challengeId);
    challengeMemberMapper.updateRole("LEADER", nextLeaderMemberId, challengeId);
    challengeMapper.touchLeaderLastActiveAt(challengeId, asString(nextLeader.get("USER_ID")));
  }

  private void validateSupportedVoteType(VoteType type) {
    if (type == VoteType.MEETING_ATTENDANCE
        || type == VoteType.EXPENSE
        || type == VoteType.KICK
        || type == VoteType.LEADER_KICK
        || type == VoteType.DISSOLVE) {
      return;
    }
    throw new RuntimeException("VOTE_004:지원하지 않는 투표 유형입니다");
  }

  private GeneralVoteType toGeneralVoteType(VoteType type) {
    if (type == VoteType.KICK) {
      return GeneralVoteType.KICK;
    }
    if (type == VoteType.LEADER_KICK) {
      return GeneralVoteType.LEADER_KICK;
    }
    if (type == VoteType.DISSOLVE) {
      return GeneralVoteType.DISSOLVE;
    }
    throw new RuntimeException("VOTE_004:일반 투표 유형이 아닙니다");
  }

  private void expireVote(String voteId, VoteType type) {
    if (type == VoteType.MEETING_ATTENDANCE) {
      voteMapper.updateStatus(voteId, VoteStatus.EXPIRED.name());
      return;
    }
    if (type == VoteType.EXPENSE) {
      expenseVoteMapper.updateStatus(voteId, VoteStatus.EXPIRED.name());
      ExpenseVote expenseVote = expenseVoteMapper.findById(voteId);
      if (expenseVote != null) {
        expenseRequestMapper.updateStatus(expenseVote.getExpenseRequestId(), "REJECTED", null);
      }
      return;
    }
    generalVoteMapper.updateStatus(voteId, VoteStatus.EXPIRED.name());
  }

  private VoteDto mapToVoteDto(Map<String, Object> map) {
    VoteDto dto = new VoteDto();
    dto.setVoteId(asString(map.get("VOTE_ID")));

    String typeStr = asString(map.get("TYPE"));
    if (hasText(typeStr)) {
      dto.setType(parseVoteType(typeStr));
    }

    String statusStr = asString(map.get("STATUS"));
    if (hasText(statusStr)) {
      dto.setStatus(parseVoteStatus(statusStr));
    }

    dto.setTitle(asString(map.get("TITLE")));
    dto.setCreatedBy(VoteDto.CreatorDto.builder()
        .userId(asString(map.get("CREATED_BY_ID")))
        .nickname(asString(map.get("CREATED_BY_NICKNAME")))
        .build());

    int agree = toInt(map.get("AGREE_COUNT"));
    int disagree = toInt(map.get("DISAGREE_COUNT"));
    int total = toInt(map.get("TOTAL_VOTE_COUNT"));
    dto.setVoteCount(VoteDto.VoteCountDto.builder().agree(agree).disagree(disagree).total(total).build());

    dto.setDeadline(toLocalDateTime(map.get("DEADLINE")));
    dto.setCreatedAt(toLocalDateTime(map.get("CREATED_AT")));
    return dto;
  }

  // [학습] 카운트 Map을 VoteCount DTO로 변환한다.
  private VoteDto.VoteCountDto toVoteCount(Map<String, Object> counts) {
    return VoteDto.VoteCountDto.builder()
        .agree(toInt(counts.get("AGREE")))
        .disagree(toInt(counts.get("DISAGREE")))
        .total(toInt(counts.get("TOTAL")))
        .build();
  }

  // [학습] 투표 응답(CastVoteResponse)을 조립한다.
  private CastVoteResponse buildCastResponse(String voteId, String myChoice, Map<String, Object> counts) {
    return CastVoteResponse.builder()
        .voteId(voteId)
        .myVote(myChoice)
        .voteCount(toVoteCount(counts))
        .votedAt(LocalDateTime.now())
        .message("투표가 완료되었습니다")
        .build();
  }

  // [학습] 득표 수 기준으로 투표 상태를 전이한다.
  private void updateVoteStatusByCounts(String voteId,
      Map<String, Object> counts,
      int eligibleCount,
      int requiredCount,
      VoteStatusUpdater updater) {
    VoteStatus status = determineVoteStatusByCounts(counts, eligibleCount, requiredCount);
    if (status != null) {
      updater.update(voteId, status.name());
    }
  }

  private VoteStatus determineVoteStatusByCounts(Map<String, Object> counts, int eligibleCount, int requiredCount) {
    int agree = toInt(counts.get("AGREE"));
    int disagree = toInt(counts.get("DISAGREE"));
    int safeEligibleCount = Math.max(eligibleCount, requiredCount);

    if (agree >= requiredCount) {
      return VoteStatus.APPROVED;
    }
    if (disagree > safeEligibleCount - requiredCount) {
      return VoteStatus.REJECTED;
    }
    return null;
  }

  // [학습] 현재 투표 상태가 투표 가능한지 판별한다.
  private boolean isCastableStatus(VoteStatus status) {
    return status == VoteStatus.PENDING || status == VoteStatus.OPEN || status == VoteStatus.IN_PROGRESS;
  }

  // [학습] 상태 필터 입력값을 정규화한다.
  private String normalizeStatusFilter(String status) {
    if (!hasText(status)) {
      return null;
    }
    String normalized = status.trim().toUpperCase();
    if ("OPEN".equals(normalized) || "IN_PROGRESS".equals(normalized)) {
      return VoteStatus.PENDING.name();
    }
    return normalized;
  }

  // [학습] 타입 필터 입력값을 정규화한다.
  private String normalizeTypeFilter(String type) {
    if (!hasText(type)) {
      return null;
    }
    return type.trim().toUpperCase();
  }

  // [학습] 요청 선택값을 투표 타입에 맞게 정규화한다.
  private String normalizeRequestChoice(CastVoteRequest request) {
    if (request == null || !hasText(request.getChoice())) {
      throw new RuntimeException("VOTE_004:투표 선택값이 필요합니다");
    }

    String choice = request.getChoice().trim().toUpperCase();
    if (!"AGREE".equals(choice) && !"DISAGREE".equals(choice)) {
      throw new RuntimeException("VOTE_004:지원하지 않는 투표 선택값입니다");
    }
    return choice;
  }

  // [학습] 모임 참석 투표 선택값을 정규화한다.
  private String normalizeMeetingChoice(String choice) {
    if ("AGREE".equals(choice) || "DISAGREE".equals(choice)) {
      return choice;
    }
    return null;
  }

  // [학습] 지출 승인 투표 선택값을 정규화한다.
  private String normalizeExpenseChoice(String choice) {
    if ("APPROVE".equals(choice)) {
      return "AGREE";
    }
    if ("REJECT".equals(choice)) {
      return "DISAGREE";
    }
    return null;
  }

  // [학습] 일반 투표 선택값을 정규화한다.
  private String normalizeGeneralChoice(String choice) {
    if ("APPROVE".equals(choice)) {
      return "AGREE";
    }
    if ("REJECT".equals(choice)) {
      return "DISAGREE";
    }
    return null;
  }

  // [학습] 문자열 투표 타입을 Enum으로 변환한다.
  private VoteType parseVoteType(String rawType) {
    try {
      return VoteType.valueOf(rawType.toUpperCase());
    } catch (Exception e) {
      throw new RuntimeException("VOTE_004:지원하지 않는 투표 유형입니다");
    }
  }

  // [학습] 문자열 투표 상태를 Enum으로 변환한다.
  private VoteStatus parseVoteStatus(String rawStatus) {
    if (!hasText(rawStatus)) {
      return VoteStatus.PENDING;
    }

    String normalized = rawStatus.toUpperCase();
    if ("OPEN".equals(normalized) || "IN_PROGRESS".equals(normalized)) {
      return VoteStatus.PENDING;
    }
    try {
      return VoteStatus.valueOf(normalized);
    } catch (Exception e) {
      return VoteStatus.PENDING;
    }
  }

  // [학습] 타임스탬프 값을 LocalDateTime으로 변환한다.
  private LocalDateTime toLocalDateTime(Object timestamp) {
    if (timestamp == null) {
      return null;
    }
    if (timestamp instanceof java.sql.Timestamp ts) {
      return ts.toLocalDateTime();
    }
    if (timestamp instanceof LocalDateTime dt) {
      return dt;
    }
    return null;
  }

  // [학습] 숫자/문자열 값을 int로 안전하게 변환한다.
  private int toInt(Object value) {
    if (value == null) {
      return 0;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  // [학습] Object 값을 null-safe 문자열로 변환한다.
  private String asString(Object value) {
    return value == null ? null : value.toString();
  }

  // [학습] 문자열이 비어있지 않은지 확인한다.
  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  @FunctionalInterface
  private interface VoteStatusUpdater {
    void update(String voteId, String status);
  }
}
