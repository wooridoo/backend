package com.woorido.auth.social;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.woorido.common.entity.SocialProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class SocialStateService {
    private final SecretKey stateSecretKey;
    private final long expirationSeconds;

    public SocialStateService(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${oauth.state-expiration-seconds:300}") long expirationSeconds) {
        this.stateSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String issue(SocialProvider provider, SocialAuthIntent intent, String returnTo) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("social-auth")
                .claim("provider", provider.name())
                .claim("intent", intent.name())
                .claim("returnTo", returnTo)
                .claim("nonce", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(stateSecretKey)
                .compact();
    }

    public SocialStatePayload verify(String stateToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(stateSecretKey)
                    .build()
                    .parseSignedClaims(stateToken)
                    .getPayload();

            String provider = claims.get("provider", String.class);
            String intent = claims.get("intent", String.class);
            String returnTo = claims.get("returnTo", String.class);

            return SocialStatePayload.builder()
                    .provider(SocialProvider.valueOf(provider))
                    .intent(SocialAuthIntent.from(intent))
                    .returnTo(returnTo)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("AUTH_012:유효하지 않은 소셜 인증 요청입니다");
        }
    }
}
