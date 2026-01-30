package com.woorido.auth.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.woorido.auth.domain.VerifiedEmail;

/**
 * In-Memory 인증 완료 토큰 저장소
 * Key: verificationToken
 */
@Repository
public class VerifiedEmailStore {

    private final Map<String, VerifiedEmail> store = new ConcurrentHashMap<>();

    public void save(VerifiedEmail verifiedEmail) {
        store.put(verifiedEmail.getVerificationToken(), verifiedEmail);
    }

    public Optional<VerifiedEmail> findByToken(String token) {
        return Optional.ofNullable(store.get(token));
    }

    public void remove(String token) {
        store.remove(token);
    }
}
