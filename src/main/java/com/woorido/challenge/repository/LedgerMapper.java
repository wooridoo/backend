package com.woorido.challenge.repository;

import com.woorido.challenge.domain.LedgerEntry;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LedgerMapper {
  void insert(LedgerEntry ledgerEntry);

  List<LedgerEntry> findSupportHistory(@Param("challengeId") String challengeId, @Param("userId") String userId);
}
