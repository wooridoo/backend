package com.woorido.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthHeaderResolver {

  private final JwtUtil jwtUtil;

  public String resolveUserId(String authorizationHeader) {
    String token = resolveToken(authorizationHeader);
    return jwtUtil.getUserIdFromToken(token);
  }

  public String resolveAccessUserId(String authorizationHeader) {
    String token = resolveToken(authorizationHeader);
    if (!jwtUtil.isAccessToken(token)) {
      throw new RuntimeException("AUTH_001:Authorization header is required");
    }
    return jwtUtil.getUserIdFromToken(token);
  }

  public String resolveToken(String authorizationHeader) {
    if (authorizationHeader == null
        || authorizationHeader.length() < 7
        || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
      throw new RuntimeException("AUTH_001:Authorization header is required");
    }

    String token = authorizationHeader.substring(7).trim();
    if (token.isBlank()) {
      throw new RuntimeException("AUTH_001:Authorization header is required");
    }
    if (!jwtUtil.validateToken(token)) {
      throw new RuntimeException("AUTH_002:Invalid access token");
    }
    return token;
  }
}
