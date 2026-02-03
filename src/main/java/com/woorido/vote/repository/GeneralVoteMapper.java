package com.woorido.vote.repository;

import com.woorido.vote.domain.GeneralVote;
import com.woorido.vote.domain.GeneralVoteRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

@Mapper
public interface GeneralVoteMapper {
  void insert(GeneralVote vote);

  GeneralVote findById(String id);

  // 투표 기록 관련
  void insertRecord(GeneralVoteRecord record);

  int checkRecordExisting(@Param("voteId") String voteId, @Param("userId") String userId);

  // 집계 조회
  Map<String, Object> findVoteCounts(String voteId);
}
