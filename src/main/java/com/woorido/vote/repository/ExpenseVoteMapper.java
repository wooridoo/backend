package com.woorido.vote.repository;

import com.woorido.vote.domain.ExpenseVote;
import com.woorido.vote.domain.ExpenseVoteRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface ExpenseVoteMapper {
  // Learning note:
  // - Methods here map 1:1 to MyBatis XML statement ids.
  void insert(ExpenseVote vote);

  ExpenseVote findById(String id);

  void insertRecord(ExpenseVoteRecord record);

  int checkRecordExisting(@Param("voteId") String voteId, @Param("userId") String userId);

  Map<String, Object> findVoteCounts(String voteId);

  String findMyVote(@Param("voteId") String voteId, @Param("userId") String userId);

  int updateStatus(@Param("voteId") String voteId, @Param("status") String status);

  int expirePendingVotes();
}
