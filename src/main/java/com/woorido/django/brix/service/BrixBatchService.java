package com.woorido.django.brix.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woorido.common.mapper.UserMapper;
import com.woorido.django.brix.client.DjangoBrixClient;
import com.woorido.django.brix.dto.BrixBatchResult;
import com.woorido.django.brix.dto.DjangoBrixCalculateRequest;
import com.woorido.django.brix.dto.DjangoBrixCalculateResponse;
import com.woorido.django.brix.model.BrixMetricRow;
import com.woorido.django.brix.repository.BrixMetricMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrixBatchService {

  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private final BrixMetricMapper brixMetricMapper;
  private final DjangoBrixClient djangoBrixClient;
  private final UserMapper userMapper;

  @Transactional
  public BrixBatchResult recalculate(LocalDateTime cutoffAt) {
    List<BrixMetricRow> rows = brixMetricMapper.findMetricsUpTo(cutoffAt);
    if (rows == null || rows.isEmpty()) {
      return BrixBatchResult.builder()
          .cutoffAt(cutoffAt.toString())
          .requestedUsers(0)
          .updatedUsers(0)
          .build();
    }

    DjangoBrixCalculateRequest request = DjangoBrixCalculateRequest.builder()
        .cutoffAt(cutoffAt.toString())
        .users(toUserMetrics(rows))
        .build();

    DjangoBrixCalculateResponse response = djangoBrixClient.calculate(request);
    List<DjangoBrixCalculateResponse.UserScore> scores = response.getResults();
    if (scores == null) {
      throw new RuntimeException("BRIX_001:Django BRIX 계산 결과가 비어 있습니다");
    }

    int updatedUsers = 0;
    String calculatedMonth = cutoffAt.format(MONTH_FORMATTER);
    for (DjangoBrixCalculateResponse.UserScore score : scores) {
      if (score == null || score.getUserId() == null || score.getUserId().isBlank()) {
        continue;
      }
      double totalScore = score.getTotalScore() != null ? score.getTotalScore() : 12.0;
      userMapper.upsertTotalScoreByUserId(UUID.randomUUID().toString(), score.getUserId(), totalScore, cutoffAt,
          calculatedMonth);
      updatedUsers++;
    }

    return BrixBatchResult.builder()
        .cutoffAt(cutoffAt.toString())
        .requestedUsers(rows.size())
        .updatedUsers(updatedUsers)
        .build();
  }

  private List<DjangoBrixCalculateRequest.UserMetric> toUserMetrics(List<BrixMetricRow> rows) {
    List<DjangoBrixCalculateRequest.UserMetric> users = new ArrayList<>();
    for (BrixMetricRow row : rows) {
      users.add(DjangoBrixCalculateRequest.UserMetric.builder()
          .userId(row.getUserId())
          .attendance(toSafeInt(row.getAttendanceCount()))
          .paymentMonths(toSafeInt(row.getPaymentMonths()))
          .overdue(toSafeInt(row.getOverdueCount()))
          .consecutiveOverdue(toSafeInt(row.getConsecutiveOverdueCount()))
          .feed(toSafeInt(row.getFeedCount()))
          .comment(toSafeInt(row.getCommentCount()))
          .like(toSafeInt(row.getLikeCount()))
          .leaderMonths(toSafeInt(row.getLeaderMonths()))
          .voteAbsence(toSafeInt(row.getVoteAbsenceCount()))
          .reportReceived(toSafeInt(row.getReportReceivedCount()))
          .kickCount(toSafeInt(row.getKickCount()))
          .build());
    }
    return users;
  }

  private int toSafeInt(Integer value) {
    return value != null ? value : 0;
  }
}
