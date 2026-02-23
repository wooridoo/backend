package com.woorido.challenge.service;

import com.woorido.challenge.domain.LedgerEntry;
import com.woorido.challenge.dto.request.CreateLedgerEntryRequest;
import com.woorido.challenge.dto.request.UpdateLedgerEntryRequest;
import com.woorido.challenge.dto.response.LedgerEntryResponse;
import com.woorido.challenge.dto.response.LedgerListResponse;
import com.woorido.challenge.dto.response.LedgerSummaryResponse;
import com.woorido.challenge.repository.ChallengeMapper;
import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.challenge.repository.LedgerMapper;
import com.woorido.common.util.JwtUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LedgerService {
  private final LedgerMapper ledgerMapper;
  private final ChallengeMapper challengeMapper;
  private final ChallengeMemberMapper challengeMemberMapper;
  private final JwtUtil jwtUtil;

  @Transactional(readOnly = true)
  public LedgerListResponse getLedger(String challengeId, String authorization, int page, int size) {
    String userId = resolveUserId(authorization);
    requireChallengeMember(challengeId, userId);

    int safePage = Math.max(page, 0);
    int safeSize = Math.max(size, 1);
    int offset = safePage * safeSize;

    List<LedgerEntryResponse> content = ledgerMapper.findByChallengeId(challengeId, offset, safeSize)
        .stream()
        .map(this::toResponse)
        .toList();

    long totalElements = ledgerMapper.countByChallengeId(challengeId);
    int totalPages = (int) Math.ceil((double) totalElements / safeSize);

    return LedgerListResponse.builder()
        .content(content)
        .totalElements(totalElements)
        .totalPages(totalPages)
        .number(safePage)
        .size(safeSize)
        .build();
  }

  @Transactional(readOnly = true)
  public LedgerSummaryResponse getLedgerSummary(String challengeId, String authorization) {
    String userId = resolveUserId(authorization);
    requireChallengeMember(challengeId, userId);

    Map<String, Object> summary = ledgerMapper.summarizeByChallengeId(challengeId);
    return LedgerSummaryResponse.builder()
        .challengeId(challengeId)
        .totalIncome(toLong(summary.get("TOTAL_INCOME")))
        .totalExpense(toLong(summary.get("TOTAL_EXPENSE")))
        .balance(toLong(summary.get("BALANCE")))
        .entryCount(toLong(summary.get("ENTRY_COUNT")))
        .build();
  }

  @Transactional
  public LedgerEntryResponse createLedgerEntry(String challengeId, String authorization, CreateLedgerEntryRequest request) {
    String userId = resolveUserId(authorization);
    requireChallengeMember(challengeId, userId);

    throw new RuntimeException("LEDGER_003:수기 장부 생성은 비활성화되어 있습니다");
  }

  @Transactional
  public LedgerEntryResponse updateLedgerEntry(String entryId, String authorization, UpdateLedgerEntryRequest request) {
    String userId = resolveUserId(authorization);

    LedgerEntry current = ledgerMapper.findById(entryId);
    if (current == null) {
      throw new RuntimeException("LEDGER_001:장부 항목을 찾을 수 없습니다");
    }
    requireChallengeMember(current.getChallengeId(), userId);

    throw new RuntimeException("LEDGER_003:수기 장부 수정은 비활성화되어 있습니다");
  }

  private String resolveUserId(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new RuntimeException("AUTH_001:인증이 필요합니다");
    }

    String token = authorization.substring(7);
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_001:인증이 필요합니다");
    }
    return jwtUtil.getUserIdFromToken(token);
  }

  private void requireChallengeMember(String challengeId, String userId) {
    if (challengeMapper.findById(challengeId) == null) {
      throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
    }
    Map<String, Object> memberInfo = challengeMemberMapper.findByUserIdAndChallengeId(userId, challengeId);
    if (memberInfo == null || "LEFT".equals(String.valueOf(memberInfo.get("STATUS")))) {
      throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
    }
  }

  private LedgerEntryResponse toResponse(LedgerEntry entry) {
    return LedgerEntryResponse.builder()
        .entryId(entry.getId())
        .challengeId(entry.getChallengeId())
        .type(entry.getType() != null ? entry.getType().name() : null)
        .amount(entry.getAmount() != null ? entry.getAmount() : 0L)
        .description(entry.getDescription())
        .memo(entry.getMemo())
        .createdAt(formatDateTime(entry.getCreatedAt()))
        .updatedAt(formatDateTime(entry.getMemoUpdatedAt()))
        .build();
  }

  private String formatDateTime(LocalDateTime value) {
    return value != null ? value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
  }

  private long toLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value == null) {
      return 0L;
    }
    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException e) {
      return 0L;
    }
  }
}
