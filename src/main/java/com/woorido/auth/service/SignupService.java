package com.woorido.auth.service;

import com.woorido.common.entity.AccountStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.woorido.auth.dto.request.SignupRequest;
import com.woorido.auth.dto.response.SignupResponse;
import com.woorido.common.entity.User;
import com.woorido.common.mapper.UserMapper;
import com.woorido.account.domain.Account;
import com.woorido.account.repository.AccountMapper;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SignupService {
  // Learning note:
  // - Read flow as: validate auth/role -> execute domain logic -> persist via Mapper.

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    // [학습] 회원가입과 초기 계좌 생성을 처리한다.
    public SignupResponse signup(SignupRequest request) {

        try {
            // 1. 필수 약관 동의 검증
            if (request.getTermsAgreed() == null || !request.getTermsAgreed() ||
                    request.getPrivacyAgreed() == null || !request.getPrivacyAgreed()) {
                throw new IllegalArgumentException("필수 약관 동의가 필요합니다.");
            }

            // 2. 이메일 중복 확인
            int count = userMapper.countByEmail(request.getEmail());
            if (count > 0) {
                throw new RuntimeException("USER_002: 이미 존재하는 이메일입니다.");
            }

            // 3. birthDate 파싱 (있는 경우)
            LocalDate birthDate = null;
            if (request.getBirthDate() != null && !request.getBirthDate().isEmpty()) {
                try {
                    birthDate = LocalDate.parse(request.getBirthDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (Exception e) {
                    // birthDate 파싱 실패 시 무시
                }
            }

            // 4. User 엔티티 생성
            User user = User.builder()
                    .id(UUID.randomUUID().toString())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .name(request.getName())
                    .nickname(request.getNickname())
                    .phone(request.getPhone())
                    .birthDate(birthDate)
                    .accountStatus(AccountStatus.ACTIVE)
                    .gender(request.getGender() != null
                            ? com.woorido.common.entity.UserGender.valueOf(request.getGender())
                            : null)
                    .agreedTerms(Boolean.TRUE.equals(request.getTermsAgreed()) ? "Y" : "N")
                    .agreedPrivacy(Boolean.TRUE.equals(request.getPrivacyAgreed()) ? "Y" : "N")
                    .agreedMarketing(Boolean.TRUE.equals(request.getMarketingAgreed()) ? "Y" : "N")
                    .createdAt(LocalDateTime.now())
                    .build();

            // 5. DB에 저장 (MyBatis)
            userMapper.insertUser(user);

            // 6. 계좌 생성 (초기 잔액 0원)
            Account account = Account.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(user.getId())
                    .balance(0L)
                    .lockedBalance(0L)
                    .accountNumber(generateAccountNumber())
                    .version(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            accountMapper.save(account);

            // 7. 초기 브릭스(12.0) 레코드 생성
            userMapper.insertInitialUserScore(UUID.randomUUID().toString(), user.getId(), 12.0);

            // 8. 응답 생성
            return SignupResponse.from(user);

        } catch (Exception e) {

            throw e;
        }
    }

    // [학습] 신규 계좌번호를 생성한다.
    private String generateAccountNumber() {
        // 12자리 랜덤 숫자 (예: 1000-0000-0000 형식이나 DB는 String)
        // 여기서는 간단히 랜덤 숫자만 생성
        return String.valueOf((long) (Math.random() * 900000000000L) + 100000000000L);
    }
}
