package com.woorido.expense.service;

import com.woorido.challenge.domain.Challenge;
import com.woorido.challenge.domain.LedgerEntry;
import com.woorido.challenge.domain.LedgerEntryType;
import com.woorido.challenge.repository.ChallengeMapper;
import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.challenge.repository.LedgerMapper;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.expense.domain.ExpenseRequest;
import com.woorido.expense.domain.PaymentBarcode;
import com.woorido.expense.dto.request.ApproveExpenseRequest;
import com.woorido.expense.dto.request.CreateExpenseRequest;
import com.woorido.expense.dto.request.UpdateExpenseRequest;
import com.woorido.expense.dto.response.ExpenseListResponse;
import com.woorido.expense.dto.response.ExpenseResponse;
import com.woorido.expense.dto.response.ExpenseUserResponse;
import com.woorido.expense.repository.ExpenseRequestMapper;
import com.woorido.expense.repository.PaymentBarcodeMapper;
import com.woorido.meeting.repository.MeetingMapper;
import com.woorido.vote.domain.ExpenseVote;
import com.woorido.vote.domain.Vote.VoteStatus;
import com.woorido.vote.repository.ExpenseVoteMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {
  private final ExpenseRequestMapper expenseRequestMapper;
  private final PaymentBarcodeMapper paymentBarcodeMapper;
  private final ExpenseVoteMapper expenseVoteMapper;
  private final ChallengeMapper challengeMapper;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final MeetingMapper meetingMapper;
  private final LedgerMapper ledgerMapper;
  private final UserMapper userMapper;

  @Transactional(readOnly = true)
  public ExpenseListResponse getExpenses(String challengeId, String userId, String status, int page, int size) {
    requireMember(challengeId, userId);

    int offset = page * size;
    List<Map<String, Object>> rows = expenseRequestMapper.findAllByChallengeId(challengeId, status, offset, size);
    long totalElements = expenseRequestMapper.countAllByChallengeId(challengeId, status);
    Long totalAmount = expenseRequestMapper.sumAmountByChallengeId(challengeId, status);
    int totalPages = (int) Math.ceil(totalElements / (double) Math.max(size, 1));

    List<ExpenseResponse> content = rows.stream().map(this::toExpenseResponse).collect(Collectors.toList());
    return ExpenseListResponse.builder()
        .content(content)
        .totalAmount(totalAmount == null ? 0L : totalAmount)
        .totalElements(totalElements)
        .totalPages(totalPages)
        .number(page)
        .size(size)
        .build();
  }

  @Transactional(readOnly = true)
  public ExpenseResponse getExpense(String challengeId, String expenseId, String userId) {
    requireMember(challengeId, userId);
    Map<String, Object> row = expenseRequestMapper.findByIdWithChallenge(expenseId, challengeId);
    if (row == null) {
      throw new RuntimeException("EXPENSE_001:지출 내역을 찾을 수 없습니다");
    }
    return toExpenseResponse(row);
  }

  @Transactional
  public ExpenseResponse createExpense(String challengeId, String userId, CreateExpenseRequest request) {
    Map<String, Object> member = requireMember(challengeId, userId);
    if (!"LEADER".equals(asString(member.get("ROLE")))) {
      throw new RuntimeException("EXPENSE_005:등록 권한이 없습니다");
    }
    validateCreateRequest(challengeId, request);

    String expenseId = UUID.randomUUID().toString();
    LocalDateTime now = LocalDateTime.now();
    ExpenseRequest expenseRequest = ExpenseRequest.builder()
        .id(expenseId)
        .meetingId(request.getMeetingId())
        .createdBy(userId)
        .title(request.getTitle())
        .amount(request.getAmount())
        .description(request.getDescription())
        .receiptUrl(request.getReceiptUrl())
        .status("VOTING")
        .createdAt(now)
        .build();
    expenseRequestMapper.insert(expenseRequest);

    int attendeeCount = meetingMapper.countAttendees(request.getMeetingId());
    if (attendeeCount <= 0) {
      attendeeCount = Math.max(1, challengeMemberMapper.findAllActiveMembers(challengeId).size());
    }
    int requiredCount = attendeeCount / 2 + 1;

    String voteId = UUID.randomUUID().toString();
    ExpenseVote vote = ExpenseVote.builder()
        .id(voteId)
        .expenseRequestId(expenseId)
        .eligibleCount(attendeeCount)
        .requiredCount(requiredCount)
        .status(VoteStatus.PENDING)
        .createdAt(now)
        .expiresAt(request.getDeadline() != null ? request.getDeadline() : now.plusHours(24))
        .build();
    expenseVoteMapper.insert(vote);

    Map<String, Object> row = expenseRequestMapper.findByIdWithChallenge(expenseId, challengeId);
    ExpenseResponse base = toExpenseResponse(row);
    return ExpenseResponse.builder()
        .expenseId(base.getExpenseId())
        .challengeId(base.getChallengeId())
        .meetingId(base.getMeetingId())
        .voteId(voteId)
        .title(base.getTitle())
        .description(base.getDescription())
        .amount(base.getAmount())
        .category(base.getCategory())
        .status(base.getStatus())
        .requestedBy(base.getRequestedBy())
        .receiptUrl(base.getReceiptUrl())
        .approvedAt(base.getApprovedAt())
        .paidAt(base.getPaidAt())
        .createdAt(base.getCreatedAt())
        .build();
  }

  @Transactional
  public ExpenseResponse approveExpense(
      String challengeId,
      String expenseId,
      String userId,
      ApproveExpenseRequest request) {
    Map<String, Object> member = requireMember(challengeId, userId);
    if (!"LEADER".equals(asString(member.get("ROLE")))) {
      throw new RuntimeException("EXPENSE_005:승인 권한이 없습니다");
    }

    Map<String, Object> row = expenseRequestMapper.findByIdWithChallenge(expenseId, challengeId);
    if (row == null) {
      throw new RuntimeException("EXPENSE_001:지출 내역을 찾을 수 없습니다");
    }

    String currentStatus = asString(row.get("STATUS"));
    if ("APPROVED".equals(currentStatus) || "PAID".equals(currentStatus) || "USED".equals(currentStatus)) {
      throw new RuntimeException("EXPENSE_004:이미 처리된 지출입니다");
    }
    if ("CANCELLED".equals(currentStatus)) {
      throw new RuntimeException("EXPENSE_006:취소된 지출입니다");
    }

    boolean approved = request != null && Boolean.TRUE.equals(request.getApproved());
    LocalDateTime approvedAt = approved ? LocalDateTime.now() : null;
    String nextStatus = approved ? "APPROVED" : "REJECTED";

    expenseRequestMapper.updateStatus(expenseId, nextStatus, approvedAt);

    ExpenseVote vote = expenseVoteMapper.findByExpenseRequestId(expenseId);
    if (vote != null) {
      expenseVoteMapper.updateStatus(vote.getId(), approved ? VoteStatus.APPROVED.name() : VoteStatus.REJECTED.name());
    }

    if (approved) {
      Challenge challenge = challengeMapper.findById(challengeId);
      if (challenge == null) {
        throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
      }
      long beforeBalance = challenge.getBalance() == null ? 0L : challenge.getBalance();
      long amount = toLong(row.get("AMOUNT"));
      if (beforeBalance < amount) {
        throw new RuntimeException("ACCOUNT_004:잔액이 부족합니다");
      }

      challenge.setBalance(beforeBalance - amount);
      int updated = challengeMapper.updateBalance(challenge);
      if (updated == 0) {
        throw new RuntimeException("CHALLENGE_014:지출 승인 중 잔액 반영에 실패했습니다");
      }

      LedgerEntry ledger = LedgerEntry.builder()
          .id(UUID.randomUUID().toString())
          .challengeId(challengeId)
          .type(LedgerEntryType.EXPENSE)
          .amount(amount)
          .balanceBefore(beforeBalance)
          .balanceAfter(beforeBalance - amount)
          .description("지출 승인: " + asString(row.get("TITLE")))
          .relatedUserId(asString(row.get("CREATED_BY")))
          .relatedMeetingId(asString(row.get("MEETING_ID")))
          .relatedExpenseRequestId(expenseId)
          .build();
      ledgerMapper.insert(ledger);

      PaymentBarcode existing = paymentBarcodeMapper.findByExpenseRequestId(expenseId);
      if (existing == null) {
        PaymentBarcode barcode = PaymentBarcode.builder()
            .id(UUID.randomUUID().toString())
            .expenseRequestId(expenseId)
            .challengeId(challengeId)
            .barcodeNumber(generateBarcodeNumber())
            .amount(amount)
            .status("ACTIVE")
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
        paymentBarcodeMapper.insert(barcode);
      }
    }

    Map<String, Object> updatedRow = expenseRequestMapper.findByIdWithChallenge(expenseId, challengeId);
    ExpenseResponse response = toExpenseResponse(updatedRow);
    PaymentBarcode barcode = paymentBarcodeMapper.findByExpenseRequestId(expenseId);
    if (barcode == null) {
      return response;
    }
    return ExpenseResponse.builder()
        .expenseId(response.getExpenseId())
        .challengeId(response.getChallengeId())
        .meetingId(response.getMeetingId())
        .voteId(response.getVoteId())
        .title(response.getTitle())
        .description(response.getDescription())
        .amount(response.getAmount())
        .category(response.getCategory())
        .status(response.getStatus())
        .requestedBy(response.getRequestedBy())
        .receiptUrl(response.getReceiptUrl())
        .paymentBarcodeNumber(barcode.getBarcodeNumber())
        .approvedAt(response.getApprovedAt())
        .paidAt(response.getPaidAt())
        .createdAt(response.getCreatedAt())
        .build();
  }

  @Transactional
  public ExpenseResponse updateExpense(
      String challengeId,
      String expenseId,
      String userId,
      UpdateExpenseRequest request) {
    Map<String, Object> member = requireMember(challengeId, userId);
    Map<String, Object> row = expenseRequestMapper.findByIdWithChallenge(expenseId, challengeId);
    if (row == null) {
      throw new RuntimeException("EXPENSE_001:지출 내역을 찾을 수 없습니다");
    }

    String creatorId = asString(row.get("CREATED_BY"));
    boolean isLeader = "LEADER".equals(asString(member.get("ROLE")));
    if (!isLeader && !userId.equals(creatorId)) {
      throw new RuntimeException("EXPENSE_005:수정 권한이 없습니다");
    }

    String status = asString(row.get("STATUS"));
    if (!"VOTING".equals(status) && !"REJECTED".equals(status)) {
      throw new RuntimeException("EXPENSE_006:승인 후에는 수정할 수 없습니다");
    }

    if (request != null && request.getAmount() != null && request.getAmount() <= 0) {
      throw new RuntimeException("EXPENSE_002:유효하지 않은 금액입니다");
    }

    ExpenseRequest update = ExpenseRequest.builder()
        .id(expenseId)
        .title(request != null ? request.getTitle() : null)
        .description(request != null ? request.getDescription() : null)
        .amount(request != null ? request.getAmount() : null)
        .receiptUrl(request != null ? request.getReceiptUrl() : null)
        .build();
    expenseRequestMapper.update(update);

    Map<String, Object> updated = expenseRequestMapper.findByIdWithChallenge(expenseId, challengeId);
    return toExpenseResponse(updated);
  }

  @Transactional
  public void deleteExpense(String challengeId, String expenseId, String userId) {
    Map<String, Object> member = requireMember(challengeId, userId);
    Map<String, Object> row = expenseRequestMapper.findByIdWithChallenge(expenseId, challengeId);
    if (row == null) {
      throw new RuntimeException("EXPENSE_001:지출 내역을 찾을 수 없습니다");
    }

    String creatorId = asString(row.get("CREATED_BY"));
    boolean isLeader = "LEADER".equals(asString(member.get("ROLE")));
    if (!isLeader && !userId.equals(creatorId)) {
      throw new RuntimeException("EXPENSE_005:삭제 권한이 없습니다");
    }

    String status = asString(row.get("STATUS"));
    if ("APPROVED".equals(status) || "PAID".equals(status) || "USED".equals(status)) {
      throw new RuntimeException("EXPENSE_006:지급 완료 후에는 삭제할 수 없습니다");
    }

    expenseRequestMapper.updateStatus(expenseId, "CANCELLED", null);
    ExpenseVote vote = expenseVoteMapper.findByExpenseRequestId(expenseId);
    if (vote != null) {
      expenseVoteMapper.updateStatus(vote.getId(), VoteStatus.CANCELLED.name());
    }
  }

  private void validateCreateRequest(String challengeId, CreateExpenseRequest request) {
    if (request == null) {
      throw new RuntimeException("EXPENSE_002:유효하지 않은 요청입니다");
    }
    if (!hasText(request.getMeetingId())) {
      throw new RuntimeException("EXPENSE_002:meetingId가 필요합니다");
    }
    if (!hasText(request.getTitle())) {
      throw new RuntimeException("EXPENSE_002:title이 필요합니다");
    }
    if (request.getAmount() == null || request.getAmount() <= 0) {
      throw new RuntimeException("EXPENSE_002:유효하지 않은 금액입니다");
    }

    Map<String, Object> meeting = meetingMapper.findById(request.getMeetingId());
    if (meeting == null) {
      throw new RuntimeException("MEETING_001:모임을 찾을 수 없습니다");
    }
    if (!challengeId.equals(asString(meeting.get("CHALLENGE_ID")))) {
      throw new RuntimeException("MEETING_001:모임을 찾을 수 없습니다");
    }
  }

  private Map<String, Object> requireMember(String challengeId, String userId) {
    Map<String, Object> member = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (member == null || "LEFT".equals(asString(member.get("STATUS")))) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }
    return member;
  }

  private ExpenseResponse toExpenseResponse(Map<String, Object> row) {
    if (row == null) {
      return null;
    }
    String createdBy = asString(row.get("CREATED_BY"));
    User creator = createdBy != null ? userMapper.findById(createdBy) : null;
    String nickname = asString(row.get("CREATED_BY_NICKNAME"));
    if (!hasText(nickname) && creator != null) {
      nickname = creator.getNickname();
    }
    String profileImage = asString(row.get("CREATED_BY_PROFILE_IMAGE"));
    if (!hasText(profileImage) && creator != null) {
      profileImage = creator.getProfileImageUrl();
    }

    ExpenseVote vote = expenseVoteMapper.findByExpenseRequestId(asString(row.get("EXPENSE_ID")));
    return ExpenseResponse.builder()
        .expenseId(asString(row.get("EXPENSE_ID")))
        .challengeId(asString(row.get("CHALLENGE_ID")))
        .meetingId(asString(row.get("MEETING_ID")))
        .voteId(vote != null ? vote.getId() : null)
        .title(asString(row.get("TITLE")))
        .description(asString(row.get("DESCRIPTION")))
        .amount(toLong(row.get("AMOUNT")))
        .category("OTHER")
        .status(asString(row.get("STATUS")))
        .requestedBy(ExpenseUserResponse.builder()
            .userId(createdBy)
            .nickname(hasText(nickname) ? nickname : "Unknown")
            .profileImage(profileImage)
            .build())
        .receiptUrl(asString(row.get("RECEIPT_URL")))
        .approvedAt((LocalDateTime) row.get("APPROVED_AT"))
        .createdAt((LocalDateTime) row.get("CREATED_AT"))
        .build();
  }

  private String generateBarcodeNumber() {
    String source = UUID.randomUUID().toString().replace("-", "").toUpperCase();
    return source.substring(0, 20);
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private long toLong(Object value) {
    if (value == null) {
      return 0L;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (Exception ignored) {
      return 0L;
    }
  }
}
