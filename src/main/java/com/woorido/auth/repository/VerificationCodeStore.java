package com.woorido.auth.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.woorido.auth.domain.VerificationCode;

/**
 * In-Memory 인증 코드 저장소
 * ConcurrentHashMap 기반
 */
@Repository
public class VerificationCodeStore {

    // Key: email, Value: VerificationCode
    private final Map<String, VerificationCode> store = new ConcurrentHashMap<>();

    public void save(VerificationCode code) {
        store.put(code.getEmail(), code);
    }

    public Optional<VerificationCode> findByEmail(String email) {
        return Optional.ofNullable(store.get(email));
    }

    public void remove(String email) {
        store.remove(email);
    }

    /**
     * 만료된 코드 정리 (선택적, 스케줄러로 호출 가능)
     */
    public void cleanExpired() {
        store.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
