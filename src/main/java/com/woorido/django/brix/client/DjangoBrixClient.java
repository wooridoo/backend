package com.woorido.django.brix.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.woorido.django.brix.dto.DjangoBrixCalculateRequest;
import com.woorido.django.brix.dto.DjangoBrixCalculateResponse;
import java.util.Objects;

@Component
public class DjangoBrixClient {

  private final RestClient restClient;
  private final String apiKey;

  public DjangoBrixClient(RestClient.Builder restClientBuilder,
      @Value("${django.brix.base-url:http://localhost:8000}") String baseUrl,
      @Value("${django.brix.api-key:woorido-django-internal-key}") String apiKey) {
    String resolvedBaseUrl = Objects.requireNonNull(baseUrl, "django.brix.base-url must not be null");
    this.restClient = restClientBuilder.baseUrl(resolvedBaseUrl).build();
    this.apiKey = Objects.requireNonNull(apiKey, "django.brix.api-key must not be null");
  }

  public DjangoBrixCalculateResponse calculate(DjangoBrixCalculateRequest request) {
    DjangoBrixCalculateRequest safeRequest = Objects.requireNonNull(request, "request must not be null");
    MediaType contentType = Objects.requireNonNull(MediaType.APPLICATION_JSON, "application/json must not be null");

    try {
      DjangoBrixCalculateResponse response = restClient.post()
          .uri("/internal/brix/calculate")
          .contentType(contentType)
          .header("X-Api-Key", apiKey)
          .body(safeRequest)
          .retrieve()
          .body(DjangoBrixCalculateResponse.class);
      if (response == null) {
        throw new RuntimeException("BRIX_001:Django 응답이 비어 있습니다");
      }
      return response;
    } catch (RestClientException e) {
      throw new RuntimeException("BRIX_001:Django BRIX 계산 호출에 실패했습니다", e);
    }
  }
}
