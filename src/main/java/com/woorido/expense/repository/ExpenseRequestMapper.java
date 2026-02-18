package com.woorido.expense.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.woorido.expense.domain.ExpenseRequest;

@Mapper
public interface ExpenseRequestMapper {
    void insert(ExpenseRequest expenseRequest);

    ExpenseRequest findById(String id);

    int updateStatus(@Param("id") String id, @Param("status") String status, @Param("approvedAt") java.time.LocalDateTime approvedAt);

    int markExpiredVotingAsRejected();

    int countApprovedExpensesSince(@Param("challengeId") String challengeId,
            @Param("userId") String userId,
            @Param("since") java.time.LocalDateTime since);
}
