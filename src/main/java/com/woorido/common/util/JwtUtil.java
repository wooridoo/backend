package com.woorido.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
  // Learning note:
  // - Centralizes JWT issue/validate/claim extraction to avoid duplicated auth logic.

  private final SecretKey secretKey;
  private final long accessTokenExpiration;
  private final long refreshTokenExpiration;

  public JwtUtil(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
      @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  public String generateAccessToken(String userId, String email) {
    return Jwts.builder()
        .subject(userId)
        .claim("email", email)
        .claim("type", "access")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(secretKey)
        .compact();
  }

  public String generateRefreshToken(String userId) {
    return Jwts.builder()
        .subject(userId)
        .claim("type", "refresh")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
        .signWith(secretKey)
        .compact();
  }

  public String getUserIdFromToken(String token) {
    return parseClaims(token).getSubject();
  }

  public String getTokenType(String token) {
    return parseClaims(token).get("type", String.class);
  }

  public boolean isAccessToken(String token) {
    return validateTokenType(token, "access");
  }

  public boolean isRefreshToken(String token) {
    return validateTokenType(token, "refresh");
  }

  public boolean validateTokenType(String token, String expectedType) {
    try {
      String tokenType = getTokenType(token);
      return expectedType.equals(tokenType);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public long getAccessTokenExpiration() {
    return accessTokenExpiration / 1000;
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
