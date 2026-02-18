package com.woorido.vote.repository;

import com.woorido.vote.domain.GeneralVote;
import com.woorido.vote.domain.GeneralVoteRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface GeneralVoteMapper {
  // Learning note:
  // - Methods here map 1:1 to MyBatis XML statement ids.
  void insert(GeneralVote vote);

  GeneralVote findById(String id);

  void insertRecord(GeneralVoteRecord record);

  int checkRecordExisting(@Param("voteId") String voteId, @Param("userId") String userId);

  Map<String, Object> findVoteCounts(String voteId);

  String findMyVote(@Param("voteId") String voteId, @Param("userId") String userId);

  int updateStatus(@Param("voteId") String voteId, @Param("status") String status);

  int expirePendingVotes();
}
