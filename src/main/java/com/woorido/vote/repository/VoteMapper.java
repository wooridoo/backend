package com.woorido.vote.repository;

import com.woorido.vote.domain.Vote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface VoteMapper {

  // 투표 생성
  void insert(Vote vote);

  // 투표 목록 조회 (Map으로 반환하여 DTO 매핑 용이하게)
  List<Map<String, Object>> findAllByChallengeIdWithFilter(
      @Param("challengeId") String challengeId,
      @Param("status") String status,
      @Param("type") String type,
      @Param("offset") int offset,
      @Param("size") int size);

  // 투표 총 개수 조회
  long countAllByChallengeIdWithFilter(
      @Param("challengeId") String challengeId,
      @Param("status") String status,
      @Param("type") String type);

  Vote findById(String id);

  // 내 투표 정보 조회 (AGREE, DISAGREE check)
  String findMyVote(@Param("voteId") String voteId, @Param("userId") String userId);

  // 투표 집계 (AGREE, DISAGREE, TOTAL)
  Map<String, Object> findVoteCounts(String voteId);

  // 투표 참여
  int checkVoteRecordExisting(@Param("voteId") String voteId, @Param("userId") String userId);

  void insertVoteRecord(@Param("id") String id, @Param("voteId") String voteId, @Param("userId") String userId,
      @Param("choice") String choice);
}
