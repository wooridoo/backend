package com.woorido.auth.social;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class AbstractSocialOAuthClient implements SocialOAuthClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(7);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected JsonNode postForm(String url, Map<String, String> formData) {
        try {
            String requestBody = formData.entrySet().stream()
                    .filter(entry -> StringUtils.hasText(entry.getValue()))
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                            + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = response.body();
                log.warn("OAuth token exchange failed: url={}, status={}, body={}",
                        url, response.statusCode(), summarize(body));
                throw mapTokenExchangeFailure(body);
            }
            return objectMapper.readTree(response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("OAuth token exchange exception: url={}, message={}", url, e.getMessage());
            throw new RuntimeException("AUTH_018:소셜 제공자 응답 처리에 실패했습니다");
        }
    }

    protected JsonNode getJson(String url, String bearerToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OAuth user profile request failed: url={}, status={}, body={}",
                        url, response.statusCode(), summarize(response.body()));
                throw new RuntimeException("AUTH_018:소셜 제공자 응답 처리에 실패했습니다");
            }
            return objectMapper.readTree(response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("OAuth user profile exception: url={}, message={}", url, e.getMessage());
            throw new RuntimeException("AUTH_018:소셜 제공자 응답 처리에 실패했습니다");
        }
    }

    private RuntimeException mapTokenExchangeFailure(String body) {
        try {
            JsonNode payload = objectMapper.readTree(body);
            String errorCode = payload.path("error").asText("");
            if ("invalid_grant".equalsIgnoreCase(errorCode)) {
                return new RuntimeException("AUTH_017:인가 코드가 만료되었거나 이미 사용되었습니다");
            }
        } catch (Exception ignored) {
            // JSON 파싱이 실패해도 일반 제공자 오류로 처리합니다.
        }

        return new RuntimeException("AUTH_018:소셜 제공자 응답 처리에 실패했습니다");
    }

    private String summarize(String rawBody) {
        if (!StringUtils.hasText(rawBody)) {
            return "<empty>";
        }

        String normalized = rawBody.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (normalized.length() <= 280) {
            return normalized;
        }
        return normalized.substring(0, 280) + "...";
    }
}
