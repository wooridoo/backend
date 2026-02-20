package com.woorido.django.ledger.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.woorido.django.ledger.dto.DjangoLedgerGraphRequest;
import com.woorido.django.ledger.dto.DjangoLedgerGraphResponse;
import java.util.Objects;

@Component
public class DjangoLedgerClient {

  private final RestClient restClient;
  private final String apiKey;

  public DjangoLedgerClient(RestClient.Builder restClientBuilder,
      @Value("${django.ledger.base-url:${django.brix.base-url:http://localhost:8000}}") String baseUrl,
      @Value("${django.ledger.api-key:${django.brix.api-key:woorido-django-internal-key}}") String apiKey) {
    String resolvedBaseUrl = Objects.requireNonNull(baseUrl, "django.ledger.base-url must not be null");
    this.restClient = restClientBuilder.baseUrl(resolvedBaseUrl).build();
    this.apiKey = Objects.requireNonNull(apiKey, "django.ledger.api-key must not be null");
  }

  public DjangoLedgerGraphResponse calculateGraph(DjangoLedgerGraphRequest request) {
    DjangoLedgerGraphRequest safeRequest = Objects.requireNonNull(request, "request must not be null");
    MediaType contentType = Objects.requireNonNull(MediaType.APPLICATION_JSON, "application/json must not be null");

    try {
      DjangoLedgerGraphResponse response = restClient.post()
          .uri("/internal/brix/ledger/chart")
          .contentType(contentType)
          .header("X-Api-Key", apiKey)
          .body(safeRequest)
          .retrieve()
          .body(DjangoLedgerGraphResponse.class);
      if (response == null) {
        throw new RuntimeException("LEDGER_001:Django ledger graph response is empty");
      }
      return response;
    } catch (RestClientException e) {
      throw new RuntimeException("LEDGER_001:Failed to call Django ledger graph API", e);
    }
  }
}
