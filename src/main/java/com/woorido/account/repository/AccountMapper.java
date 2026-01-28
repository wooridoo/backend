package com.woorido.account.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.account.domain.Account;

@Mapper
public interface AccountMapper {
    Account findByUserId(@Param("userId") String userId);

    void save(Account account); // 계좌 생성용 (추후 필요)

    void update(Account account); // 잔액 변경용 (추후 필요)
}
