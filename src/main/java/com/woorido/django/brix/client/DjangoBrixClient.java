package com.woorido.django.brix.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.woorido.django.brix.dto.DjangoBrixCalculateRequest;
import com.woorido.django.brix.dto.DjangoBrixCalculateResponse;

@Component
public class DjangoBrixClient {

  private final RestClient restClient;
  private final String apiKey;

  public DjangoBrixClient(RestClient.Builder restClientBuilder,
      @Value("${django.brix.base-url:http://localhost:8000}") String baseUrl,
      @Value("${django.brix.api-key:woorido-django-internal-key}") String apiKey) {
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    this.apiKey = apiKey;
  }

  public DjangoBrixCalculateResponse calculate(DjangoBrixCalculateRequest request) {
    try {
      DjangoBrixCalculateResponse response = restClient.post()
          .uri("/internal/brix/calculate")
          .contentType(MediaType.APPLICATION_JSON)
          .header("X-Api-Key", apiKey)
          .body(request)
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
