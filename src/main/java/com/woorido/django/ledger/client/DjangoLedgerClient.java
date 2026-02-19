package com.woorido.django.ledger.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.woorido.django.ledger.dto.DjangoLedgerGraphRequest;
import com.woorido.django.ledger.dto.DjangoLedgerGraphResponse;

@Component
public class DjangoLedgerClient {

  private final RestClient restClient;
  private final String apiKey;

  public DjangoLedgerClient(RestClient.Builder restClientBuilder,
      @Value("${django.ledger.base-url:${django.brix.base-url:http://localhost:8000}}") String baseUrl,
      @Value("${django.ledger.api-key:${django.brix.api-key:woorido-django-internal-key}}") String apiKey) {
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    this.apiKey = apiKey;
  }

  public DjangoLedgerGraphResponse calculateGraph(DjangoLedgerGraphRequest request) {
    try {
      DjangoLedgerGraphResponse response = restClient.post()
          .uri("/internal/brix/ledger/chart")
          .contentType(MediaType.APPLICATION_JSON)
          .header("X-Api-Key", apiKey)
          .body(request)
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
