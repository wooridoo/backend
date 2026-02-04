package com.woorido.vote.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface VoteQueryMapper {
  // 통합 조회 (Pagination)
  List<Map<String, Object>> findAllUnionByChallengeId(@Param("challengeId") String challengeId,
      @Param("offset") int offset,
      @Param("limit") int limit);

  // 통합 카운트
  long countAllUnionByChallengeId(@Param("challengeId") String challengeId);
  
  Map<String, Object> findByIdBasic(@Param("voteId") String voteId);
  
}
