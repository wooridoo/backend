package com.woorido.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woorido.account.domain.Account;
import com.woorido.account.dto.response.MyAccountResponse;
import com.woorido.account.repository.AccountMapper;
import com.woorido.common.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;
    private final JwtUtil jwtUtil;

    /**
     * 내 어카운트 조회
     */
    @Transactional(readOnly = true)
    public MyAccountResponse getMyAccount(String accessToken) {
        String userId = jwtUtil.getUserIdFromToken(accessToken);

        // 2. DB 조회
        Account account = accountMapper.findByUserId(userId);

        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌를 찾을 수 없습니다");
        }

        // 3. 응답 DTO 변환
        return MyAccountResponse.from(account);
    }
}
