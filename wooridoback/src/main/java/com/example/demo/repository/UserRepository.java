package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    // 이메일 중복 확인을 위한 쿼리 메서드 추가
    // 1. 유효한 이메일 형식 필수, 2. 중복 확인 (필수) 정책 반영
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}