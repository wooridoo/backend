package com.woorido.challenge.repository;

import org.apache.ibatis.annotations.Mapper;
import com.woorido.challenge.domain.LedgerEntry;

@Mapper
public interface LedgerEntryMapper {
    void save(LedgerEntry ledgerEntry);
}
