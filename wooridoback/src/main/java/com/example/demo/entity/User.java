package com.example.demo.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "woorido")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(length = 36)
    private String id; // UUID

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String name; // 실명

    @Column(unique = true, length = 50)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

   

    @Column(name = "agreed_terms")
    private String agreedTerms; // Y, N

    @Column(name = "agreed_privacy")
    private String agreedPrivacy;

    @Column(name = "agreed_marketing")
    private String agreedMarketing;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    
    @Column(name = "account_status", length = 20) // 설계서의 account_status 반영
    @Builder.Default
    private String accountStatus = "ACTIVE"; // 기본값 'ACTIVE' 적용

    // 명시적으로 getStatus() 메서드를 만들어 SignupResponse의 호출에 대응합니다.
    public String getStatus() {
        return this.accountStatus;}
    
    
    
    
    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID().toString(); // UUID 생성
        this.createdAt = LocalDateTime.now();
    }
}