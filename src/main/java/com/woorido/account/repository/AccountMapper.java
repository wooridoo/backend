package com.woorido.account.repository;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.account.domain.Account;
import com.woorido.account.domain.AccountTransaction;
import com.woorido.account.dto.request.TransactionSearchRequest;

@Mapper
public interface AccountMapper {
    Account findByUserId(@Param("userId") String userId);

    void save(Account account); // 계좌 생성용 (추후 필요)

    int update(Account account); // 반환값으로 낙관적 락 체크

    // 서포트 중복 확인
    int countSupportByMonth(@Param("accountId") String accountId, @Param("challengeId") String challengeId);

    // 첫 서포트 여부 확인
    int countTotalSupport(@Param("accountId") String accountId, @Param("challengeId") String challengeId);
    // 잔액 변경용 (추후 필요)

    // 거래 내역 조회
    List<AccountTransaction> findTransactions(TransactionSearchRequest request);

    // 거래 내역 총 개수
    Long countTransactions(TransactionSearchRequest request);

    // 수입/지출 합계
    Map<String, Long> sumAmountsByDirection(TransactionSearchRequest request);

    long sumWithdrawalsToday(String accountId);

    long sumWithdrawalsThisMonth(String accountId);

    void saveTransaction(AccountTransaction transaction);
}
