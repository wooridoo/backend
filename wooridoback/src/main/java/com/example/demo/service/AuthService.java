package com.example.demo.service;

import com.example.demo.dto.SignupRequest;
import com.example.demo.dto.SignupResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder; // 주입 받기

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 1. 필수 약관 동의 검증
        if (!request.getTermsAgreed() || !request.getPrivacyAgreed()) {
            throw new IllegalArgumentException("필수 약관 동의가 필요합니다.");
        }

        // 2. 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("USER_002: 이미 존재하는 이메일입니다.");
        }

        // 3. Entity 변환 및 암호화 저장 (핵심!)
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // 암호화 적용
                .name(request.getName())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .agreedTerms(request.getTermsAgreed() ? "Y" : "N")
                .agreedPrivacy(request.getPrivacyAgreed() ? "Y" : "N")
                .agreedMarketing(request.getMarketingAgreed() ? "Y" : "N")
                .build();

        User savedUser = userRepository.save(user);

        return SignupResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .status(savedUser.getAccountStatus())
                .createdAt(savedUser.getCreatedAt().toString())
                .build();
    }

    @Transactional(readOnly = true)
    public String login(String email, String userInputPassword) {
        // 1. DB에서 이메일로 사용자 조회 (UserRepository 사용)
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // 2. 암호화된 비밀번호와 입력값 비교 (matches 함수 사용)
            if (passwordEncoder.matches(userInputPassword, user.getPasswordHash())) {
                return "SUCCESS";
            }
        }
        return "FAIL";
    }
}