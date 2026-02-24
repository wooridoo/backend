package com.woorido.django.ledger.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorido.django.ledger.dto.DjangoLedgerGraphRequest;
import com.woorido.django.ledger.dto.DjangoLedgerGraphResponse;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
public class DjangoLedgerClient {

  private static final String HEALTHY = "HEALTHY";
  private static final String DEGRADED = "DEGRADED";
  private static final String MISCONFIGURED = "MISCONFIGURED";
  private static final String LEDGER_OK = "LEDGER_OK";
  private static final String CODE_NETWORK = "LEDGER_004";
  private static final String CODE_AUTH = "LEDGER_010";
  private static final String CODE_CONTRACT = "LEDGER_011";

  private final RestClient restClient;
  private final String apiKey;
  private final String baseUrl;
  private final ObjectMapper objectMapper;

  private volatile String healthStatus = DEGRADED;
  private volatile String lastErrorCode = CODE_NETWORK;
  private volatile Instant lastCheckedAt;

  public DjangoLedgerClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper,
      @Value("${django.ledger.base-url:${django.brix.base-url:http://localhost:8000}}") String baseUrl,
      @Value("${django.ledger.api-key:${django.brix.api-key:}}") String apiKey,
      @Value("${django.ledger.connect-timeout-ms:3000}") int connectTimeoutMs,
      @Value("${django.ledger.read-timeout-ms:5000}") int readTimeoutMs) {
    this.baseUrl = Objects.requireNonNull(baseUrl, "django.ledger.base-url must not be null");

    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeoutMs);
    requestFactory.setReadTimeout(readTimeoutMs);

    this.restClient = restClientBuilder
        .baseUrl(this.baseUrl)
        .requestFactory(requestFactory)
        .build();
    this.apiKey = Objects.requireNonNull(apiKey, "django.ledger.api-key must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @EventListener(ApplicationReadyEvent.class)
  public void performStartupPreflight() {
    if (apiKey.isBlank()) {
      updateHealth(MISCONFIGURED, CODE_AUTH);
      log.warn("Django ledger preflight skipped. reason=missing_api_key, baseUrl={}", baseUrl);
      return;
    }

    DjangoLedgerGraphRequest probeRequest = DjangoLedgerGraphRequest.builder()
        .challengeId("internal-ledger-preflight")
        .months(1)
        .currentBalance(0L)
        .entries(List.of())
        .build();

    try {
      calculateGraph(probeRequest);
      log.info("Django ledger preflight succeeded. baseUrl={}", baseUrl);
    } catch (RuntimeException e) {
      String code = extractCode(e.getMessage());
      log.warn("Django ledger preflight failed. code={}, baseUrl={}, message={}",
          code,
          baseUrl,
          e.getMessage());
    }
  }

  public DjangoLedgerGraphResponse calculateGraph(DjangoLedgerGraphRequest request) {
    DjangoLedgerGraphRequest safeRequest = Objects.requireNonNull(request, "request must not be null");
    MediaType contentType = Objects.requireNonNull(MediaType.APPLICATION_JSON, "application/json must not be null");

    if (apiKey.isBlank()) {
      updateHealth(MISCONFIGURED, CODE_AUTH);
      throw new RuntimeException(CODE_AUTH + ":Django ledger API key가 비어 있습니다");
    }

    final String payload;
    try {
      payload = objectMapper.writeValueAsString(safeRequest);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("LEDGER_001:요청 JSON 직렬화에 실패했습니다", e);
    }

    try {
      DjangoLedgerGraphResponse response = restClient.post()
          .uri("/internal/brix/ledger/chart")
          .contentType(contentType)
          .header("X-Api-Key", apiKey)
          .body(payload)
          .retrieve()
          .body(DjangoLedgerGraphResponse.class);
      validateResponseContract(response);
      updateHealth(HEALTHY, LEDGER_OK);
      return response;
    } catch (RestClientResponseException e) {
      throw classifyResponseException(e);
    } catch (RestClientException e) {
      throw classifyClientException(e);
    }
  }

  public String getLedgerDjangoHealth() {
    return healthStatus;
  }

  public String getLastCheckedAt() {
    return lastCheckedAt == null ? null : lastCheckedAt.toString();
  }

  public String getLastErrorCode() {
    return lastErrorCode;
  }

  private void validateResponseContract(DjangoLedgerGraphResponse response) {
    if (response == null || response.getMonthlyExpenses() == null || response.getMonthlyBalances() == null) {
      updateHealth(MISCONFIGURED, CODE_CONTRACT);
      throw new RuntimeException(CODE_CONTRACT + ":Django ledger 응답 형식이 올바르지 않습니다");
    }
  }

  private RuntimeException classifyResponseException(RestClientResponseException e) {
    int status = e.getStatusCode().value();
    if (status == 401 || status == 403) {
      updateHealth(MISCONFIGURED, CODE_AUTH);
      log.warn("Django ledger auth/config mismatch. status={}, baseUrl={}, body={}",
          status,
          baseUrl,
          e.getResponseBodyAsString());
      return new RuntimeException(CODE_AUTH + ":Django ledger 인증/설정 불일치", e);
    }

    if (status >= 500) {
      updateHealth(DEGRADED, CODE_NETWORK);
      log.warn("Django ledger service unavailable. status={}, baseUrl={}", status, baseUrl);
      return new RuntimeException(CODE_NETWORK + ":Django ledger service unavailable", e);
    }

    updateHealth(MISCONFIGURED, CODE_CONTRACT);
    log.warn("Django ledger contract mismatch. status={}, baseUrl={}, body={}",
        status,
        baseUrl,
        e.getResponseBodyAsString());
    return new RuntimeException(CODE_CONTRACT + ":Django ledger 응답 계약 불일치", e);
  }

  private RuntimeException classifyClientException(RestClientException e) {
    if (isContractException(e)) {
      updateHealth(MISCONFIGURED, CODE_CONTRACT);
      log.warn("Django ledger response parse failed. baseUrl={}, message={}", baseUrl, e.getMessage());
      return new RuntimeException(CODE_CONTRACT + ":Django ledger 응답 파싱 실패", e);
    }

    updateHealth(DEGRADED, CODE_NETWORK);
    log.warn("Django ledger network/timeout failure. baseUrl={}, message={}", baseUrl, e.getMessage());
    return new RuntimeException(CODE_NETWORK + ":Django ledger service unavailable", e);
  }

  private boolean isContractException(Throwable throwable) {
    Throwable cursor = throwable;
    while (cursor != null) {
      if (cursor instanceof JsonProcessingException || cursor instanceof HttpMessageConversionException) {
        return true;
      }
      cursor = cursor.getCause();
    }
    return false;
  }

  private void updateHealth(String status, String errorCode) {
    this.healthStatus = status;
    this.lastErrorCode = errorCode;
    this.lastCheckedAt = Instant.now();
  }

  private String extractCode(String message) {
    if (message == null || message.isBlank()) {
      return CODE_NETWORK;
    }
    int separatorIndex = message.indexOf(':');
    String code = separatorIndex >= 0 ? message.substring(0, separatorIndex) : message;
    return code.isBlank() ? CODE_NETWORK : code;
  }
}
