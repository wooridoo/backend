package com.woorido.challenge.repository;

import com.woorido.challenge.domain.LedgerEntry;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LedgerMapper {
  void insert(LedgerEntry ledgerEntry);

  List<LedgerEntry> findSupportHistory(@Param("challengeId") String challengeId, @Param("userId") String userId);

  List<LedgerEntry> findByChallengeId(
      @Param("challengeId") String challengeId,
      @Param("offset") int offset,
      @Param("limit") int limit);

  long countByChallengeId(@Param("challengeId") String challengeId);

  LedgerEntry findById(@Param("entryId") String entryId);

  int updateLedgerEntry(LedgerEntry ledgerEntry);

  Map<String, Object> summarizeByChallengeId(@Param("challengeId") String challengeId);
}
