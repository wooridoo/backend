package com.woorido.auth.service;

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
public class SignupService {

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional

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
                    .accountStatus("ACTIVE")
                    .agreedTerms(Boolean.TRUE.equals(request.getTermsAgreed()) ? "Y" : "N")
                    .agreedPrivacy(Boolean.TRUE.equals(request.getPrivacyAgreed()) ? "Y" : "N")
                    .agreedMarketing(Boolean.TRUE.equals(request.getMarketingAgreed()) ? "Y" : "N")
                    .createdAt(LocalDateTime.now())
                    .build();

            // 5. DB에 저장 (MyBatis)

            userMapper.insertUser(user);

            // 5-2. Account(지갑) 생성

            Account account = Account.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(user.getId())
                    .balance(0L)
                    .lockedBalance(0L)
                    .version(1)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            accountMapper.save(account);

            // 6. 응답 생성
            return SignupResponse.from(user);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
