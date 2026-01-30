package com.woorido.challenge.repository;

import com.woorido.challenge.domain.LedgerEntry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerMapper {
  void insert(LedgerEntry ledgerEntry);
}
