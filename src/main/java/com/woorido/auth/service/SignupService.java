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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public SignupResponse signup(SignupRequest request) {
        System.out.println("========== SIGNUP SERVICE 시작 ==========");
        System.out.println("email: " + request.getEmail());
        System.out.println("name: " + request.getName());
        System.out.println("nickname: " + request.getNickname());
        System.out.println("termsAgreed: " + request.getTermsAgreed());
        System.out.println("privacyAgreed: " + request.getPrivacyAgreed());

        try {
            // 1. 필수 약관 동의 검증
            if (request.getTermsAgreed() == null || !request.getTermsAgreed() ||
                    request.getPrivacyAgreed() == null || !request.getPrivacyAgreed()) {
                throw new IllegalArgumentException("필수 약관 동의가 필요합니다.");
            }

            // 2. 이메일 중복 확인
            System.out.println("이메일 중복 체크 시작...");
            int count = userMapper.countByEmail(request.getEmail());
            System.out.println("이메일 중복 체크 결과: " + count);
            if (count > 0) {
                throw new RuntimeException("USER_002: 이미 존재하는 이메일입니다.");
            }

            // 3. birthDate 파싱 (있는 경우)
            LocalDate birthDate = null;
            if (request.getBirthDate() != null && !request.getBirthDate().isEmpty()) {
                try {
                    birthDate = LocalDate.parse(request.getBirthDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (Exception e) {
                    System.out.println("birthDate 파싱 실패 (무시): " + e.getMessage());
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

            System.out.println("User 생성 완료, ID: " + user.getId());

            // 5. DB에 저장 (MyBatis)
            System.out.println("DB INSERT 시작...");
            userMapper.insertUser(user);
            System.out.println("DB INSERT 완료!");

            // 6. 응답 생성
            return SignupResponse.from(user);

        } catch (Exception e) {
            System.out.println("========== SIGNUP 에러 발생 ==========");
            System.out.println("에러 타입: " + e.getClass().getName());
            System.out.println("에러 메시지: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
