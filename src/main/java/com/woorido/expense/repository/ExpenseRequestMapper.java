package com.woorido.expense.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.expense.domain.ExpenseRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExpenseRequestMapper {
    void insert(ExpenseRequest expenseRequest);

    ExpenseRequest findById(String id);

    List<Map<String, Object>> findAllByChallengeId(
            @Param("challengeId") String challengeId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countAllByChallengeId(
            @Param("challengeId") String challengeId,
            @Param("status") String status);

    Long sumAmountByChallengeId(
            @Param("challengeId") String challengeId,
            @Param("status") String status);

    Map<String, Object> findByIdWithChallenge(
            @Param("id") String id,
            @Param("challengeId") String challengeId);

    int update(ExpenseRequest expenseRequest);

    int updateStatus(
            @Param("id") String id,
            @Param("status") String status,
            @Param("approvedAt") LocalDateTime approvedAt);
}
