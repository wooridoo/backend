package com.woorido.account.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.woorido.challenge.domain.LedgerEntryType;
import com.woorido.challenge.repository.ChallengeMapper;
import com.woorido.challenge.repository.ChallengeMemberMapper;
import com.woorido.challenge.repository.LedgerEntryMapper;

@Service
@RequiredArgsConstructor
public class AccountService {
  // Learning note:
  // - Read flow as: validate auth/role -> execute domain logic -> persist via Mapper.

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountMapper accountMapper;
    private final SessionMapper sessionMapper;
    private final WithdrawalPolicyStrategy withdrawalPolicyStrategy;
    private final AccountTransactionFactory accountTransactionFactory;
    private final JwtUtil jwtUtil;

    private final ChallengeMapper challengeMapper;
    private final ChallengeMemberMapper challengeMemberMapper;
    private final LedgerEntryMapper ledgerEntryMapper;

    /**
     */
    @Transactional(readOnly = true)
    // [학습] 내 계좌 요약 정보를 조회한다.
    public MyAccountResponse getMyAccount(String accessToken) {
        String userId = jwtUtil.getUserIdFromToken(accessToken);

        Account account = accountMapper.findByUserId(userId);

        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
        }

        long usedToday = accountMapper.sumOutgoingToday(account.getId());
        long usedThisMonth = accountMapper.sumOutgoingThisMonth(account.getId());
        long dailyLimit = withdrawalPolicyStrategy.getDailyLimit();
        long monthlyLimit = withdrawalPolicyStrategy.getMonthlyLimit();

        return MyAccountResponse.from(
                account,
                dailyLimit,
                monthlyLimit,
                usedToday,
                usedThisMonth);
    }

    /**
     */
    @Transactional(readOnly = true)
    // [학습] 거래 내역 목록/합계/페이지 정보를 조회한다.
    public TransactionHistoryResponse getTransactionHistory(String accessToken, TransactionSearchRequest request) {
        String userId = jwtUtil.getUserIdFromToken(accessToken);

        Account account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
        }

        request.setAccountId(account.getId());

        List<AccountTransaction> transactions = accountMapper.findTransactions(request);

        Long totalElements = accountMapper.countTransactions(request);

        Map<String, Long> sums = accountMapper.sumAmountsByDirection(request);

        return buildResponse(transactions, totalElements, request, sums);
    }

    // [학습] 거래 목록/페이지/요약 정보를 응답 DTO로 조립한다.
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

    // [학습] 계좌 거래 엔티티를 응답 아이템 DTO로 변환한다.
    private TransactionHistoryResponse.TransactionItem toTransactionItem(AccountTransaction tx) {
        TransactionHistoryResponse.RelatedChallenge related = null;
        if (tx.getRelatedChallengeId() != null) {
            related = TransactionHistoryResponse.RelatedChallenge.builder()
                    .challengeId(tx.getRelatedChallengeId())
                    .name(null)
                    .build();
        }

        String transactionId = tx.getId();

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

    // [학습] 집계 Map에서 숫자 값을 안전하게 추출한다.
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
     */
    @Transactional
    // [학습] 크레딧 충전 결제를 요청하고 결제 세션을 생성한다.
    public CreditChargeResponse requestCreditCharge(String accessToken, CreditChargeRequest request) {
        log.info("[CHARGE] 충전 요청 수신 - amount: {}, paymentMethod: {}, returnUrl: {}",
                request.getAmount(), request.getPaymentMethod(), request.getReturnUrl());

        String userId = jwtUtil.getUserIdFromToken(accessToken);
        log.info("[CHARGE] userId: {}", userId);

        if (request.getAmount() < 10000) {
            throw new RuntimeException("ACCOUNT_002:Charge amount must be at least 10000");
        }
        if (request.getAmount() % 10000 != 0) {
            throw new RuntimeException("ACCOUNT_007:Charge amount must be in units of 10000");
        }

        if (!List.of("CARD", "BANK_TRANSFER").contains(request.getPaymentMethod())) {
            throw new RuntimeException("ACCOUNT_008:결제 수단은 CARD 또는 BANK_TRANSFER만 가능합니다");
        }

        long fee = (long) (request.getAmount() * 0.03);
        long totalPaymentAmount = request.getAmount() + fee;

        String orderId = generateOrderId();

        String paymentUrl = "https://pay.woorido.com/checkout/" + orderId;

        LocalDateTime expiresAtTime = LocalDateTime.now().plusMinutes(15);
        String expiresAt = expiresAtTime.toString();

        Session session = Session.builder()
                .id(orderId)
                .userId(userId)
                .sessionType("CHARGE")
                .returnUrl(request.getReturnUrl() + "?amount=" + request.getAmount())
                .isUsed("N")
                .expiresAt(expiresAtTime)
                .build();
        sessionMapper.save(session);
        log.info("[CHARGE] 결제 세션 생성 완료 - orderId: {}, userId: {}, amount: {}", orderId, userId, request.getAmount());

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
     */
    @Transactional
    // [학습] 결제 콜백을 멱등 처리하고 잔액/거래를 반영한다.
    public ChargeCallbackResponse processChargeCallback(ChargeCallbackRequest request) {
        log.info("[CALLBACK] callback received - orderId: {}, paymentKey: {}, amount: {}, status: {}",
                request.getOrderId(), request.getPaymentKey(), request.getAmount(), request.getStatus());

        Session session = sessionMapper.findById(request.getOrderId());
        if (session == null) {
            log.error("[CALLBACK] session not found - orderId: {}", request.getOrderId());
            throw new RuntimeException("ACCOUNT_009:Invalid order");
        }

        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("ACCOUNT_011:Expired order");
        }
        if (!"CHARGE".equals(session.getSessionType())) {
            throw new RuntimeException("ACCOUNT_009:Invalid order");
        }

        if (!"SUCCESS".equals(request.getStatus())) {
            int lockedOnFailure = sessionMapper.markAsUsedIfUnused(session.getId());
            if (lockedOnFailure == 0) {
                throw new RuntimeException("ACCOUNT_010:Order already processed");
            }
            throw new RuntimeException("ACCOUNT_012:결제 실패 상태입니다: " + request.getStatus());
        }

        Long expectedAmount = parseAmountFromReturnUrl(session.getReturnUrl());
        if (expectedAmount != null && !expectedAmount.equals(request.getAmount())) {
            throw new RuntimeException("ACCOUNT_013:Payment amount mismatch");
        }

        int locked = sessionMapper.markAsUsedIfUnused(session.getId());
        if (locked == 0) {
            throw new RuntimeException("ACCOUNT_010:Order already processed");
        }

        Account account = accountMapper.findByUserId(session.getUserId());
        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
        }

        long balanceBefore = account.getBalance();
        long newBalance = balanceBefore + request.getAmount();
        log.info("[CALLBACK] updating balance - accountId: {}, before: {}, after: {}", account.getId(), balanceBefore,
                newBalance);

        account.setBalance(newBalance);
        int updated = accountMapper.update(account);
        if (updated == 0) {
            log.error("[CALLBACK] concurrent update failure - accountId: {}", account.getId());
            throw new RuntimeException("ACCOUNT_014:동시 요청으로 계좌 처리에 실패했습니다. 다시 시도해주세요.");
        }

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
        tx.setPgProvider("TOSS");
        tx.setPgTxId(request.getPaymentKey());
        tx.setCreatedAt(LocalDateTime.now());
        accountMapper.saveTransaction(tx);

        Long transactionId = Math.abs((long) tx.getId().hashCode());
        return ChargeCallbackResponse.builder()
                .transactionId(transactionId)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .newBalance(newBalance)
                .completedAt(LocalDateTime.now().toString())
                .build();
    }

    // [학습] 리턴 URL의 amount 파라미터를 파싱한다.
    private Long parseAmountFromReturnUrl(String returnUrl) {
        if (returnUrl == null || !returnUrl.contains("amount=")) {
            return null;
        }
        String[] parts = returnUrl.split("amount=", 2);
        if (parts.length <= 1) {
            return null;
        }
        String amountStr = parts[1].split("&", 2)[0];
        if (amountStr == null || amountStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("ACCOUNT_013:Payment amount mismatch");
        }
    }

    /**
     */
    @Transactional
    // [학습] 출금 정책 검증 후 잔액 차감 및 출금 거래를 생성한다.
    public WithdrawResponse requestWithdraw(String accessToken, WithdrawRequest request) {
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        Account account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
        }

        long dailyTotal = accountMapper.sumWithdrawalsToday(account.getId());
        long monthlyTotal = accountMapper.sumWithdrawalsThisMonth(account.getId());

        withdrawalPolicyStrategy.validate(account, request.getAmount(), dailyTotal, monthlyTotal);

        long fee = withdrawalPolicyStrategy.calculateFee(request.getAmount());
        long netAmount = request.getAmount();

        long totalDeduction = request.getAmount() + fee;

        long balanceBefore = account.getBalance();
        long newBalance = balanceBefore - totalDeduction;
        account.setBalance(newBalance);
        int updated = accountMapper.update(account);
        if (updated == 0) {
            throw new RuntimeException("ACCOUNT_014:동시 요청으로 계좌 처리에 실패했습니다. 다시 시도해주세요.");
        }

        AccountTransaction tx = accountTransactionFactory.createWithdrawTransaction(
                account.getId(), request, balanceBefore, newBalance);
        accountMapper.saveTransaction(tx);

        WithdrawResponse.BankInfo bankInfo = WithdrawResponse.BankInfo.builder()
                .bankCode(request.getBankCode())
                .bankName(BankCode.getNameByCode(request.getBankCode()))
                .accountNumber(request.getAccountNumber())
                .build();

        return WithdrawResponse.builder()
                .withdrawId(Math.abs((long) tx.getId().hashCode()))
                .amount(request.getAmount())
                .fee(fee)
                .netAmount(netAmount)
                .newBalance(newBalance)
                .bankInfo(bankInfo)
                .estimatedArrival(LocalDateTime.now().plusSeconds(30).toString())
                .createdAt(LocalDateTime.now().toString())
                .build();
    }

    /**
     */
    @Transactional
    // [학습] 챌린지 후원금을 납부하고 계좌/챌린지 잔액을 갱신한다.
    public SupportResponse requestSupport(String accessToken, SupportRequest request) {
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        Account account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
        }

        Challenge challenge = challengeMapper.findById(request.getChallengeId());
        if (challenge == null) {
            throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
        }

        String privilegeStatus = challengeMemberMapper.getPrivilegeStatus(request.getChallengeId(), userId);
        if (!"ACTIVE".equals(privilegeStatus)) {
            throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
        }

        int supportCount = accountMapper.countSupportByMonth(account.getId(), request.getChallengeId());
        if (supportCount > 0) {
            throw new RuntimeException("SUPPORT_001:이번 달에는 이미 후원금을 납부했습니다");
        }

        long amount = challenge.getMonthlyFee();
        if (account.getBalance() < amount) {
            throw new RuntimeException("ACCOUNT_004:잔액이 부족합니다");
        }

        long balanceBefore = account.getBalance();
        long newBalance = balanceBefore - amount;

        account.setBalance(newBalance);
        int accountUpdated = accountMapper.update(account);
        if (accountUpdated == 0) {
            throw new RuntimeException("ACCOUNT_014:동시 요청으로 계좌 처리에 실패했습니다. 다시 시도해주세요.");
        }

        AccountTransaction tx = accountTransactionFactory.createSupportTransaction(
                account.getId(), request.getChallengeId(), amount, balanceBefore, newBalance);
        accountMapper.saveTransaction(tx);

        long challengeBalanceBefore = challenge.getBalance();
        long newChallengeBalance = challengeBalanceBefore + amount;

        challenge.setBalance(newChallengeBalance);
        int challengeUpdated = challengeMapper.updateBalance(challenge);
        if (challengeUpdated == 0) {
            throw new RuntimeException("CHALLENGE_014:동시 요청으로 챌린지 잔액 반영에 실패했습니다. 다시 시도해주세요.");
        }

        LedgerEntry ledger = LedgerEntry.builder()
                .id(java.util.UUID.randomUUID().toString())
                .challengeId(challenge.getId())
                .type(LedgerEntryType.SUPPORT)
                .amount(amount)
                .balanceBefore(challengeBalanceBefore)
                .balanceAfter(newChallengeBalance)
                .relatedUserId(userId)
                .description("챌린지 월 후원금")
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryMapper.save(ledger);

        int totalSupport = accountMapper.countTotalSupport(account.getId(), challenge.getId());
        boolean isFirstSupport = (totalSupport == 1);

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

    // [학습] 결제 주문번호를 생성한다.
    private String generateOrderId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomNum = RANDOM.nextInt(90000) + 10000; // 10000 ~ 99999
        return "ORD" + timestamp + randomNum;
    }
}
