package com.woorido.vote.repository;

import com.woorido.vote.domain.Vote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface VoteMapper {
  // Learning note:
  // - Methods here map 1:1 to MyBatis XML statement ids.

  void insert(Vote vote);

  List<Map<String, Object>> findAllByChallengeIdWithFilter(
      @Param("challengeId") String challengeId,
      @Param("status") String status,
      @Param("type") String type,
      @Param("offset") int offset,
      @Param("size") int size);

  long countAllByChallengeIdWithFilter(
      @Param("challengeId") String challengeId,
      @Param("status") String status,
      @Param("type") String type);

  Vote findById(String id);

  String findMyVote(@Param("voteId") String voteId, @Param("userId") String userId);

  Map<String, Object> findVoteCounts(String voteId);

  int checkVoteRecordExisting(@Param("voteId") String voteId, @Param("userId") String userId);

  void insertVoteRecord(@Param("id") String id, @Param("voteId") String voteId, @Param("userId") String userId,
      @Param("choice") String choice);

  int updateStatus(@Param("voteId") String voteId, @Param("status") String status);

  int expirePendingVotes();
}
