package com.woorido.vote.repository;

import com.woorido.vote.domain.ExpenseVote;
import com.woorido.vote.domain.ExpenseVoteRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

@Mapper
public interface ExpenseVoteMapper {
  void insert(ExpenseVote vote);

  ExpenseVote findById(String id);

  // 투표 기록 관련
  void insertRecord(ExpenseVoteRecord record);

  int checkRecordExisting(@Param("voteId") String voteId, @Param("userId") String userId);

  // 집계 조회 (APPROVE, REJECT count)
  Map<String, Object> findVoteCounts(String voteId);
}
