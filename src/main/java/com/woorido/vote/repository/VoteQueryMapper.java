package com.woorido.vote.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface VoteQueryMapper {
  // Learning note:
  // - Methods here map 1:1 to MyBatis XML statement ids.
  List<Map<String, Object>> findAllUnionByChallengeId(@Param("challengeId") String challengeId,
      @Param("status") String status,
      @Param("type") String type,
      @Param("offset") int offset,
      @Param("limit") int limit);

  long countAllUnionByChallengeId(@Param("challengeId") String challengeId,
      @Param("status") String status,
      @Param("type") String type);

  Map<String, Object> findByIdBasic(@Param("voteId") String voteId);
}
