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
    private final LedgerEntryMapper ledgerEntryMapper;

    /**
     * ?????⑤９???熬곣뫀肄??釉뚰???
     */
    @Transactional(readOnly = true)
    // [학습] 내 계좌 요약 정보를 조회한다.
    public MyAccountResponse getMyAccount(String accessToken) {
        String userId = jwtUtil.getUserIdFromToken(accessToken);

        // 2. DB ?釉뚰???
        Account account = accountMapper.findByUserId(userId);

        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
        }

        // 3. ???쑩?젆?DTO ?怨뚮뼚???
        return MyAccountResponse.from(account);
    }

    /**
     * 癲꾧퀗???????⑤９肉??釉뚰???
     */
    @Transactional(readOnly = true)
    // [학습] 거래 내역 목록/합계/페이지 정보를 조회한다.
    public TransactionHistoryResponse getTransactionHistory(String accessToken, TransactionSearchRequest request) {
        // 1. ???ャ뀕??????userId ??⑤베毓??
        String userId = jwtUtil.getUserIdFromToken(accessToken);

        // 2. ??節뚮쳮辱??釉뚰???
        Account account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
        }

        // 3. ?濡ろ떟????釉뚰???쨨??accountId ???源놁젳
        request.setAccountId(account.getId());

        // 4. 癲꾧퀗???????⑤９肉??釉뚰???
        List<AccountTransaction> transactions = accountMapper.findTransactions(request);

        // 5. ????좊즵獒???釉뚰???
        Long totalElements = accountMapper.countTransactions(request);

        // 6. ???쒓낮??癲ル슣???????룸폍???釉뚰???
        Map<String, Long> sums = accountMapper.sumAmountsByDirection(request);

        // 7. ???쑩?젆?DTO ?怨뚮뼚??????袁⑸즵???
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
        // RelatedChallenge ????늄??
        // JOIN?????⑤챶?뺜벧猿뗪묄??????⑤베??癲??????? ?嶺뚮㉡?€쾮????????(?????????열野?
        TransactionHistoryResponse.RelatedChallenge related = null;
        if (tx.getRelatedChallengeId() != null) {
            related = TransactionHistoryResponse.RelatedChallenge.builder()
                    .challengeId(null) // ID ?怨뚮뼚??????⑤８??????⑤베??null
                    .name(null) // ?怨뚮옓?????釉뚰?????ш끽維??
                    .build();
        }

        // UUID -> Long ?怨뚮뼚??????⑤８?. ??????袁⑸젻泳?
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

    // Map ????????????뺤깓????寃뗏????釉뚰???
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
     * ??????野껊챶爾????釉먯뒜??
     */
    @Transactional
    // [학습] 크레딧 충전 결제를 요청하고 결제 세션을 생성한다.
    public CreditChargeResponse requestCreditCharge(String accessToken, CreditChargeRequest request) {
        log.info("[CHARGE] ?野껊챶爾????釉먯뒜????筌믨퀣援?- amount: {}, paymentMethod: {}, returnUrl: {}",
                request.getAmount(), request.getPaymentMethod(), request.getReturnUrl());

        // 1. ???ャ뀕???濡ろ떟?癲?????????嶺뚮Ĳ?됮?
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        log.info("[CHARGE] userId: {}", userId);

        // 2. ??ヂ???쎈눀??濡ろ떟?癲?
        if (request.getAmount() < 10000) {
            throw new RuntimeException("ACCOUNT_002:Charge amount must be at least 10000");
        }
        if (request.getAmount() % 10000 != 0) {
            throw new RuntimeException("ACCOUNT_007:Charge amount must be in units of 10000");
        }

        // 3. ?濡ろ뜏?????嚥▲꺂???濡ろ떟?癲?
        if (!List.of("CARD", "BANK_TRANSFER").contains(request.getPaymentMethod())) {
            throw new RuntimeException("ACCOUNT_008:결제 수단은 CARD 또는 BANK_TRANSFER만 가능합니다");
        }

        // 4. ??筌뚯슜鍮????節뚮쳮雅?(10,000 ~ 200,000?? 3%)
        // ??ш끽維???嶺뚮Ĳ????200,000???縕????癲ル슢?뤸뤃???? ????⒱봼???⑤슢猷? ???⑤베?????뽮덧???癲ル슢?꾤땟?????ヂ???쎈눀??????3% ???ㅼ굣??????? 200,000????湲븐땡?堉온癲?3%?嶺? ?嶺뚮Ĳ?됮?
        // ??ш끽維??
        // ???뽮덫???"10,000 ~ 200,000?? 3% ??딅텑???????뫢???筌뚯슦苑????源낆쓱. ????????쐠????????嶺뚮∥梨?????⑤챶苡?
        // ???⑤베??癲ル슢?꾤땟????野껊챶爾??癲꾧퀗????덩?????3%????좊읈??嶺뚮쮳?년봼??????열野?(??좊읈??????源놁벁?????⑤똾留?.
        long fee = (long) (request.getAmount() * 0.03);
        long totalPaymentAmount = request.getAmount() + fee;

        // 5. OrderId ??獄쏅똻??(ORD + yyyyMMddHHmmss + Random 5?????
        String orderId = generateOrderId();

        // 6. PaymentUrl ??獄쏅똻??(Mock)
        String paymentUrl = "https://pay.woorido.com/checkout/" + orderId;

        // 7. 癲ル슢???彛??癰???(15????
        LocalDateTime expiresAtTime = LocalDateTime.now().plusMinutes(15);
        String expiresAt = expiresAtTime.toString();

        // 8. Session ????(API 018 ?熬곣뫖痢딀뤆?癲ル슪?ｇ몭???????ш낄援??
        Session session = Session.builder()
                .id(orderId)
                .userId(userId)
                .sessionType("CHARGE")
                .returnUrl(request.getReturnUrl() + "?amount=" + request.getAmount()) // ??ヂ???쎈눀??濡ろ떟?癲ル슣鍮섌뜮????⑥??URL??????(??ш끽維뽳쭛??袁⑸젻泳??
                .isUsed("N")
                .expiresAt(expiresAtTime)
                .build();
        sessionMapper.save(session);
        log.info("[CHARGE] ?嶺뚮ㅎ?????????ш끽維??- orderId: {}, userId: {}, amount: {}", orderId, userId, request.getAmount());

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
     * ?野껊챶爾???熬곣뫖痢딀뤆?癲ル슪?ｇ몭??
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
        try {
            String[] parts = returnUrl.split("amount=");
            if (parts.length > 1) {
                // ???怨좊군 ????렺?????앗꾩쒀?濡?뎄?臾뺥맀筌? ??됰Ŧ????????源끹걬雅?퍔源???&????筌먦끉踰????????
                String amountStr = parts[1].split("&")[0];
                return Long.parseLong(amountStr);
            }
        } catch (Exception e) {
            // ?????????됰꽡 ???濡ろ떟?癲????袁⑤툞 (?棺??짆????影??탿????援???
        }
        return null; // temporary
    }

    /**
     * ??⑥レ툏????釉먯뒜??
     */
    @Transactional
    // [학습] 출금 정책 검증 후 잔액 차감 및 출금 거래를 생성한다.
    public WithdrawResponse requestWithdraw(String accessToken, WithdrawRequest request) {
        // 1. ???????節뚮쳮辱??濡ろ떟?癲?
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        Account account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
        }

        // 2. ??⑥レ툏?????룸폍???釉뚰???(Strategy ??ш끽維???
        long dailyTotal = accountMapper.sumWithdrawalsToday(account.getId());
        long monthlyTotal = accountMapper.sumWithdrawalsThisMonth(account.getId());

        // 3. ?嶺뚮Ĳ????濡ろ떟?癲?(??筌먲퐣?? ??釉먯뒠筌???
        withdrawalPolicyStrategy.validate(account, request.getAmount(), dailyTotal, monthlyTotal);

        // 4. ??筌뚯슜鍮????節뚮쳮雅?
        long fee = withdrawalPolicyStrategy.calculateFee(request.getAmount());
        long netAmount = request.getAmount(); // ??筌뚯슜鍮?????嶺????????釉먯뒜????ヂ???쎈눀???숆강筌?????⑥レ툏??(癲ル슢??節됰쑏???筌뚯슜鍮??룸챷?? 癲ル슓堉곤쭗?ㅒ??筌먲퐢?뀐┼??棺??짆?먰맪??怨뚮뼚?????ш끽維??
        // ??ш끽維???嶺뚮Ĳ??? ??筌뚯슜鍮?????嶺? 癲ル슢??節됰쑏???筌뚯슜鍮??룸챷?? ????덊렡癲???釉먯뒠筌?????(amount + fee)??癲ル슓堉곤쭗?ㅒ???, amount???????? ?濡ろ뜏?????ш끽維??
        // ?????筌먲퐢痢?amount癲ル슢???移???⑥レ툏????寃뗏???筌뚯슜鍮??룸챷???0????얠×苡?鍮㎳????縕???

        long totalDeduction = request.getAmount() + fee;

        // 5. ??釉먯뒠筌?????녿ぅ??熬곣뫀肄?
        long balanceBefore = account.getBalance();
        long newBalance = balanceBefore - totalDeduction;
        account.setBalance(newBalance);
        int updated = accountMapper.update(account);
        if (updated == 0) {
            throw new RuntimeException("ACCOUNT_014:동시 요청으로 계좌 처리에 실패했습니다. 다시 시도해주세요.");
        }

        // 6. 癲꾧퀗???????⑤９肉???獄쏅똻????????(Factory ??筌믨퀡裕?
        AccountTransaction tx = accountTransactionFactory.createWithdrawTransaction(
                account.getId(), request, balanceBefore, newBalance);
        accountMapper.saveTransaction(tx);

        // 7. ???쑩?젆???獄쏅똻??
        WithdrawResponse.BankInfo bankInfo = WithdrawResponse.BankInfo.builder()
                .bankCode(request.getBankCode())
                .bankName(BankCode.getNameByCode(request.getBankCode()))
                .accountNumber(request.getAccountNumber())
                .build();

        return WithdrawResponse.builder()
                .withdrawId(Math.abs((long) tx.getId().hashCode())) // UUID -> Long (??ш끽維뽳쭛?
                .amount(request.getAmount())
                .fee(fee)
                .netAmount(netAmount)
                .newBalance(newBalance)
                .bankInfo(bankInfo)
                .estimatedArrival(LocalDateTime.now().plusSeconds(30).toString()) // 30???????용∥????좊읈???
                .createdAt(LocalDateTime.now().toString())
                .build();
    }

    /**
     * ?????룎????嚥▲꺃彛???獄???(API 020)
     */
    @Transactional
    // [학습] 챌린지 후원금을 납부하고 계좌/챌린지 잔액을 갱신한다.
    public SupportResponse requestSupport(String accessToken, SupportRequest request) {
        // 1. ??????嶺뚮Ĳ?됮?
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        Account account = accountMapper.findByUserId(userId);
        if (account == null) {
            throw new RuntimeException("ACCOUNT_001:계좌 정보를 찾을 수 없습니다");
        }

        // 2. 癲??????? ?釉뚰???
        Challenge challenge = challengeMapper.findById(request.getChallengeId());
        if (challenge == null) {
            throw new RuntimeException("CHALLENGE_001:챌린지를 찾을 수 없습니다");
        }

        // 3. 癲ル슢???볥뼀???? ?嶺뚮Ĳ?됮?
        int isMember = challengeMapper.countMemberByChallengeIdAndUserId(request.getChallengeId(), userId);
        if (isMember == 0) {
            throw new RuntimeException("CHALLENGE_003:챌린지 멤버가 아닙니다");
        }

        // 4. ?????????獄?????? ?嶺뚮Ĳ?됮?
        int supportCount = accountMapper.countSupportByMonth(account.getId(), request.getChallengeId());
        if (supportCount > 0) {
            throw new RuntimeException("SUPPORT_001:이번 달에는 이미 후원금을 납부했습니다");
        }

        // 5. ??釉먯뒠筌??濡ろ떟?癲?
        long amount = challenge.getMonthlyFee();
        if (account.getBalance() < amount) {
            throw new RuntimeException("ACCOUNT_004:잔액이 부족합니다");
        }

        // 6. ?嶺뚮ㅎ??????癲ル슪?ｇ몭??(???????節뚮쳮辱?
        long balanceBefore = account.getBalance();
        long newBalance = balanceBefore - amount;

        account.setBalance(newBalance);
        int accountUpdated = accountMapper.update(account);
        if (accountUpdated == 0) {
            throw new RuntimeException("ACCOUNT_014:동시 요청으로 계좌 처리에 실패했습니다. 다시 시도해주세요.");
        }

        // ?????癲꾧퀗???????⑤９肉?
        AccountTransaction tx = accountTransactionFactory.createSupportTransaction(
                account.getId(), request.getChallengeId(), amount, balanceBefore, newBalance);
        accountMapper.saveTransaction(tx);

        // 7. ?嶺뚮ㅎ??????癲ル슪?ｇ몭??(癲??????? ??節뚮쳮辱??潁?)
        long challengeBalanceBefore = challenge.getBalance();
        long newChallengeBalance = challengeBalanceBefore + amount;

        challenge.setBalance(newChallengeBalance);
        int challengeUpdated = challengeMapper.updateBalance(challenge);
        if (challengeUpdated == 0) {
            throw new RuntimeException("CHALLENGE_014:동시 요청으로 챌린지 잔액 반영에 실패했습니다. 다시 시도해주세요.");
        }

        // 癲??????? ?潁? ??れ삀??쎈뭄?
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

        // 8. 癲??????룎????? ?嶺뚮Ĳ?됮?
        int totalSupport = accountMapper.countTotalSupport(account.getId(), challenge.getId());
        boolean isFirstSupport = (totalSupport == 1); // ?袁⑸젻泳???壤굿? 1癲꾧퀗?????????野껊갭??癲??????룎??

        // 9. ???쑩?젆???獄쏅똻??
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
