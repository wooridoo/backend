package com.woorido.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woorido.account.domain.Account;
import com.woorido.account.domain.AccountTransaction;
import com.woorido.account.dto.request.TransactionSearchRequest;
import com.woorido.account.dto.response.MyAccountResponse;
import com.woorido.account.dto.response.TransactionHistoryResponse;
import com.woorido.account.repository.AccountMapper;
import com.woorido.common.util.JwtUtil;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;

import com.woorido.account.dto.request.CreditChargeRequest;
import com.woorido.account.dto.response.CreditChargeResponse;
import com.woorido.account.dto.request.ChargeCallbackRequest;
import com.woorido.account.dto.response.ChargeCallbackResponse;
import com.woorido.account.domain.TransactionType;

import lombok.RequiredArgsConstructor;

import com.woorido.account.domain.Session;
import com.woorido.account.repository.SessionMapper;

import com.woorido.account.dto.request.WithdrawRequest;
import com.woorido.account.dto.response.WithdrawResponse;
import com.woorido.account.factory.AccountTransactionFactory;
import com.woorido.account.model.BankCode;
import com.woorido.account.strategy.WithdrawalPolicyStrategy;

import com.woorido.account.dto.request.SupportRequest;
import com.woorido.account.dto.response.SupportResponse;
import com.woorido.challenge.domain.Challenge;
import com.woorido.challenge.domain.LedgerEntry;
import com.woorido.challenge.repository.ChallengeMapper;
import com.woorido.challenge.repository.LedgerEntryMapper;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountMapper accountMapper;
    private final SessionMapper sessionMapper;
    private final WithdrawalPolicyStrategy withdrawalPolicyStrategy;
    private final AccountTransactionFactory accountTransactionFactory;
    private final JwtUtil jwtUtil;

    private final ChallengeMapper challengeMapper;
    private final LedgerEntryMapper ledgerEntryMapper;

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

    /**
     * 거래 내역 조회
     */
    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactionHistory(String accessToken, TransactionSearchRequest request) {
        // 1. 토큰에서 userId 추출
        String userId = jwtUtil.getUserIdFromToken(accessToken);

        // 2. 계좌 조회
        Account account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌를 찾을 수 없습니다");
        }

        // 3. 검색 조건에 accountId 설정
        request.setAccountId(account.getId());

        // 4. 거래 내역 조회
        List<AccountTransaction> transactions = accountMapper.findTransactions(request);

        // 5. 총 개수 조회
        Long totalElements = accountMapper.countTransactions(request);

        // 6. 수입/지출 합계 조회
        Map<String, Long> sums = accountMapper.sumAmountsByDirection(request);

        // 7. 응답 DTO 변환 및 반환
        return buildResponse(transactions, totalElements, request, sums);
    }

    private TransactionHistoryResponse buildResponse(
            List<AccountTransaction> transactions,
            Long totalElements,
            TransactionSearchRequest request,
            Map<String, Long> sums) {

        List<TransactionHistoryResponse.TransactionItem> content = transactions.stream()
                .map(this::toTransactionItem)
                .toList();

        int totalPages = request.getSize() > 0
                ? (int) Math.ceil((double) totalElements / request.getSize())
                : 0;

        TransactionHistoryResponse.PageInfo pageInfo = TransactionHistoryResponse.PageInfo.builder()
                .number(request.getPage())
                .size(request.getSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();

        TransactionHistoryResponse.Summary summary = TransactionHistoryResponse.Summary.builder()
                .totalIncome(getMapValue(sums, "totalIncome"))
                .totalExpense(getMapValue(sums, "totalExpense"))
                .period(TransactionHistoryResponse.Period.builder()
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .build())
                .build();

        return TransactionHistoryResponse.builder()
                .content(content)
                .page(pageInfo)
                .summary(summary)
                .build();
    }

    private TransactionHistoryResponse.TransactionItem toTransactionItem(AccountTransaction tx) {
        // RelatedChallenge 구성
        // JOIN이 없으므로 일단 챌린지 정보는 비워둠 (추후 구현)
        TransactionHistoryResponse.RelatedChallenge related = null;
        if (tx.getRelatedChallengeId() != null) {
            related = TransactionHistoryResponse.RelatedChallenge.builder()
                    .challengeId(null) // ID 변환 이슈로 일단 null
                    .name(null) // 별도 조회 필요
                    .build();
        }

        // UUID -> Long 변환 이슈. 음수 방지
        Long transactionId = Math.abs((long) tx.getId().hashCode());

        return TransactionHistoryResponse.TransactionItem.builder()
                .transactionId(transactionId)
                .type(tx.getType() != null ? tx.getType().name() : null)
                .amount(tx.getAmount())
                .balance(tx.getBalanceAfter())
                .description(tx.getDescription())
                .relatedChallenge(related)
                .createdAt(tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null)
                .build();
    }

    // Map 키 대소문자 무시하고 값 조회
    private Long getMapValue(Map<String, ?> map, String key) {
        if (map == null)
            return 0L;

        Object value = map.get(key.toUpperCase());
        if (value == null) {
            value = map.get(key.toLowerCase());
        }

        if (value == null)
            return 0L;
        if (value instanceof Number)
            return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 크레딧 충전 요청
     */
    @Transactional
    public CreditChargeResponse requestCreditCharge(String accessToken, CreditChargeRequest request) {
        // 1. 토큰 검증 및 사용자 확인
        String userId = jwtUtil.getUserIdFromToken(accessToken);

        // 2. 금액 검증
        if (request.getAmount() < 10000) {
            throw new RuntimeException("ACCOUNT_002:충전 금액은 10,000원 이상이어야 합니다");
        }
        if (request.getAmount() % 10000 != 0) {
            throw new RuntimeException("ACCOUNT_007:충전 금액은 10,000원 단위여야 합니다");
        }

        // 3. 결제 수단 검증
        if (!List.of("CARD", "BANK_TRANSFER").contains(request.getPaymentMethod())) {
            throw new RuntimeException("ACCOUNT_008:결제 수단은 CARD 또는 BANK_TRANSFER 만 가능합니다");
        }

        // 4. 수수료 계산 (10,000 ~ 200,000원: 3%)
        // 현재 정책상 200,000원 초과는 명시되지 않았으나, 일단 문맥상 모든 금액에 대해 3% 적용 혹은 200,000원까지만 3%인지 확인
        // 필요.
        // 문서상 "10,000 ~ 200,000원: 3% 부과"라고 되어 있음. 그 외 구간에 대한 언급 없음.
        // 일단 모든 충전 건에 대해 3%로 가정하고 구현 (가장 안전한 해석).
        long fee = (long) (request.getAmount() * 0.03);
        long totalPaymentAmount = request.getAmount() + fee;

        // 5. OrderId 생성 (ORD + yyyyMMddHHmmss + Random 5자리)
        String orderId = generateOrderId();

        // 6. PaymentUrl 생성 (Mock)
        String paymentUrl = "https://pay.woorido.com/checkout/" + orderId;

        // 7. 만료시간 (15분 후)
        LocalDateTime expiresAtTime = LocalDateTime.now().plusMinutes(15);
        String expiresAt = expiresAtTime.toString();

        // 8. Session 저장 (API 018 콜백 처리를 위해)
        Session session = Session.builder()
                .id(orderId)
                .userId(userId)
                .sessionType("CHARGE")
                .returnUrl(request.getReturnUrl() + "?amount=" + request.getAmount()) // 금액 검증용으로 URL에 포함 (임시 방편)
                .isUsed("N")
                .expiresAt(expiresAtTime)
                .build();
        sessionMapper.save(session);

        return CreditChargeResponse.builder()
                .orderId(orderId)
                .amount(request.getAmount())
                .fee(fee)
                .totalPaymentAmount(totalPaymentAmount)
                .paymentUrl(paymentUrl)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 충전 콜백 처리
     */
    @Transactional
    public ChargeCallbackResponse processChargeCallback(ChargeCallbackRequest request) {
        // 1. Session 조회
        Session session = sessionMapper.findById(request.getOrderId());
        if (session == null) {
            throw new RuntimeException("ACCOUNT_009:유효하지 않은 주문입니다");
        }

        // 2. 이미 처리된 주문 체크
        if ("Y".equals(session.getIsUsed())) {
            throw new RuntimeException("ACCOUNT_010:이미 처리된 주문입니다");
        }

        // 3. 만료 체크
        // expiresAt이 null인 경우(legacy)는 통과시키는 정책 or 필수 체크 정책. 일단 null이면 통과? 아니면 현재 시간
        // 기준?
        // 방금 만든 Session에는 expiresAt이 있으므로 null 체크 후 비교
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("ACCOUNT_011:만료된 주문입니다");
        }

        // 4. 상태 체크 (PG 결제 실패 시)
        if (!"SUCCESS".equals(request.getStatus())) {
            // 실패 시에도 세션은 사용 처리하여 재사용 방지 (정책에 따라 다를 수 있음)
            sessionMapper.markAsUsed(session.getId());
            throw new RuntimeException("ACCOUNT_012:결제 실패: " + request.getStatus());
        }

        // 5. 금액 검증 (권장)
        // Session의 returnUrl에서 amount 파싱하여 비교 (임시 방편)
        // returnUrl 형식: ...?amount=10000
        Long expectedAmount = parseAmountFromReturnUrl(session.getReturnUrl());
        if (expectedAmount != null && !expectedAmount.equals(request.getAmount())) {
            throw new RuntimeException("ACCOUNT_013:결제 금액 불일치");
        }

        // 6. 계좌 조회 및 잔액 업데이트
        Account account = accountMapper.findByUserId(session.getUserId());
        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌를 찾을 수 없습니다");
        }

        long balanceBefore = account.getBalance();
        long newBalance = balanceBefore + request.getAmount();

        account.setBalance(newBalance);
        int updated = accountMapper.update(account);
        if (updated == 0) {
            throw new RuntimeException("ACCOUNT_014:동시 요청으로 처리에 실패했습니다. 다시 시도해주세요.");
        }

        // 7. 거래 내역 저장
        AccountTransaction tx = new AccountTransaction();
        tx.setId(java.util.UUID.randomUUID().toString());
        tx.setAccountId(account.getId());
        tx.setType(TransactionType.CHARGE);
        tx.setAmount(request.getAmount());
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(newBalance);
        tx.setLockedBefore(account.getLockedBalance());
        tx.setLockedAfter(account.getLockedBalance());
        tx.setDescription("크레딧 충전");
        // tx.setRelatedChallengeId(null);
        // tx.setRelatedUserId(null);
        tx.setPgProvider("TOSS"); // 예시
        tx.setPgTxId(request.getPaymentKey());
        tx.setCreatedAt(LocalDateTime.now()); // DB DEFAULT가 있지만 명시적으로 설정

        accountMapper.saveTransaction(tx);

        // 8. Session 사용 처리
        sessionMapper.markAsUsed(session.getId());

        // UUID -> Long 변환 (임시)
        Long transactionId = Math.abs((long) tx.getId().hashCode());

        return ChargeCallbackResponse.builder()
                .transactionId(transactionId)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .newBalance(newBalance)
                .completedAt(LocalDateTime.now().toString())
                .build();
    }

    private Long parseAmountFromReturnUrl(String returnUrl) {
        if (returnUrl == null || !returnUrl.contains("amount=")) {
            return null;
        }
        try {
            String[] parts = returnUrl.split("amount=");
            if (parts.length > 1) {
                // 뒤에 다른 파라미터가 붙을 수 있으므로 &로 한번 더 자름
                String amountStr = parts[1].split("&")[0];
                return Long.parseLong(amountStr);
            }
        } catch (Exception e) {
            // 파싱 실패 시 검증 스킵 (로그 남기기 권장)
        }
        return null; // temporary
    }

    /**
     * 출금 요청
     */
    @Transactional
    public WithdrawResponse requestWithdraw(String accessToken, WithdrawRequest request) {
        // 1. 사용자/계좌 검증
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        Account account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌를 찾을 수 없습니다");
        }

        // 2. 출금 합계 조회 (Strategy 전달용)
        long dailyTotal = accountMapper.sumWithdrawalsToday(account.getId());
        long monthlyTotal = accountMapper.sumWithdrawalsThisMonth(account.getId());

        // 3. 정책 검증 (한도, 잔액 등)
        withdrawalPolicyStrategy.validate(account, request.getAmount(), dailyTotal, monthlyTotal);

        // 4. 수수료 계산
        long fee = withdrawalPolicyStrategy.calculateFee(request.getAmount());
        long netAmount = request.getAmount(); // 수수료 무료이므로 요청 금액 그대로 출금 (만약 수수료가 차감된다면 로직 변경 필요)
        // 현재 정책: 수수료 무료. 만약 수수료가 있다면 잔액에서 (amount + fee)를 차감할지, amount에서 뗄지 결정 필요.
        // 여기서는 amount만큼 출금하고 수수료는 0원이므로 단순화.

        long totalDeduction = request.getAmount() + fee;

        // 5. 잔액 업데이트
        long balanceBefore = account.getBalance();
        long newBalance = balanceBefore - totalDeduction;
        account.setBalance(newBalance);
        int updated = accountMapper.update(account);
        if (updated == 0) {
            throw new RuntimeException("ACCOUNT_014:동시 요청으로 처리에 실패했습니다. 다시 시도해주세요.");
        }

        // 6. 거래 내역 생성 및 저장 (Factory 활용)
        AccountTransaction tx = accountTransactionFactory.createWithdrawTransaction(
                account.getId(), request, balanceBefore, newBalance);
        accountMapper.saveTransaction(tx);

        // 7. 응답 생성
        WithdrawResponse.BankInfo bankInfo = WithdrawResponse.BankInfo.builder()
                .bankCode(request.getBankCode())
                .bankName(BankCode.getNameByCode(request.getBankCode()))
                .accountNumber(request.getAccountNumber())
                .build();

        return WithdrawResponse.builder()
                .withdrawId(Math.abs((long) tx.getId().hashCode())) // UUID -> Long (임시)
                .amount(request.getAmount())
                .fee(fee)
                .netAmount(netAmount)
                .newBalance(newBalance)
                .bankInfo(bankInfo)
                .estimatedArrival(LocalDateTime.now().plusSeconds(30).toString()) // 30초 후 입금 가정
                .createdAt(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 서포트 수동 납입 (API 020)
     */
    @Transactional
    public SupportResponse requestSupport(String accessToken, SupportRequest request) {
        // 1. 사용자 확인
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        Account account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌를 찾을 수 없습니다");
        }

        // 2. 챌린지 조회
        Challenge challenge = challengeMapper.findById(request.getChallengeId());
        if (challenge == null) {
            throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
        }

        // 3. 멤버 여부 확인
        int isMember = challengeMapper.countMemberByChallengeIdAndUserId(request.getChallengeId(), userId);
        if (isMember == 0) {
            throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
        }

        // 4. 이번 달 납입 여부 확인
        int supportCount = accountMapper.countSupportByMonth(account.getId(), request.getChallengeId());
        if (supportCount > 0) {
            throw new RuntimeException("SUPPORT_001:이미 이번 달 서포트를 납입했습니다");
        }

        // 5. 잔액 검증
        long amount = challenge.getMonthlyFee();
        if (account.getBalance() < amount) {
            throw new RuntimeException("ACCOUNT_004:잔액이 부족합니다");
        }

        // 6. 트랜잭션 처리 (사용자 계좌)
        long balanceBefore = account.getBalance();
        long newBalance = balanceBefore - amount;

        account.setBalance(newBalance);
        int accountUpdated = accountMapper.update(account);
        if (accountUpdated == 0) {
            throw new RuntimeException("ACCOUNT_014:동시 요청으로 처리에 실패했습니다. 다시 시도해주세요.");
        }

        // 사용자 거래 내역
        AccountTransaction tx = accountTransactionFactory.createSupportTransaction(
                account.getId(), request.getChallengeId(), amount, balanceBefore, newBalance);
        accountMapper.saveTransaction(tx);

        // 7. 트랜잭션 처리 (챌린지 계좌/장부)
        long challengeBalanceBefore = challenge.getBalance();
        long newChallengeBalance = challengeBalanceBefore + amount;

        challenge.setBalance(newChallengeBalance);
        int challengeUpdated = challengeMapper.updateBalance(challenge);
        if (challengeUpdated == 0) {
            throw new RuntimeException("CHALLENGE_014:동시 요청으로 처리에 실패했습니다. 다시 시도해주세요.");
        }

        // 챌린지 장부 기록
        LedgerEntry ledger = LedgerEntry.builder()
                .id(java.util.UUID.randomUUID().toString())
                .challengeId(challenge.getId())
                .type("SUPPORT") // or TransactionType.SUPPORT if mapped but LedgerEntry uses String in DDL
                .amount(amount)
                .balanceBefore(challengeBalanceBefore)
                .balanceAfter(newChallengeBalance)
                .relatedUserId(userId)
                .description("월 회비 납입")
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryMapper.save(ledger);

        // 8. 첫 서포트 여부 확인
        int totalSupport = accountMapper.countTotalSupport(account.getId(), challenge.getId());
        boolean isFirstSupport = (totalSupport == 1); // 방금 넣은 1건이 전부라면 첫 서포트

        // 9. 응답 생성
        return SupportResponse.builder()
                .transactionId(Math.abs((long) tx.getId().hashCode()))
                .challengeId(challenge.getId())
                .challengeName(challenge.getName())
                .amount(amount)
                .newBalance(newBalance)
                .newChallengeBalance(newChallengeBalance)
                .isFirstSupport(isFirstSupport)
                .createdAt(LocalDateTime.now().toString())
                .build();
    }

    private String generateOrderId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomNum = RANDOM.nextInt(90000) + 10000; // 10000 ~ 99999
        return "ORD" + timestamp + randomNum;
    }
}
