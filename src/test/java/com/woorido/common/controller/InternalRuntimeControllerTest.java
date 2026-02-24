package com.woorido.common.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.woorido.common.exception.GlobalExceptionHandler;
import com.woorido.django.ledger.client.DjangoLedgerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InternalRuntimeControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    DjangoLedgerClient djangoLedgerClient = mock(DjangoLedgerClient.class);
    when(djangoLedgerClient.getLedgerDjangoHealth()).thenReturn("HEALTHY");
    when(djangoLedgerClient.getLastCheckedAt()).thenReturn("2026-02-24T00:00:00Z");
    when(djangoLedgerClient.getLastErrorCode()).thenReturn("LEDGER_OK");
    InternalRuntimeController controller = new InternalRuntimeController(
        new MockEnvironment(),
        beanFactory.getBeanProvider(BuildProperties.class),
        beanFactory.getBeanProvider(GitProperties.class),
        djangoLedgerClient,
        "test-internal-key",
        "uploads",
        "http://127.0.0.1:8000");

    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void returns403WhenApiKeyMismatch() throws Exception {
    mockMvc.perform(get("/internal/runtime/info")
        .header("X-Internal-Api-Key", "wrong-key"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("AUTH_001:인증이 필요합니다"));
  }

  @Test
  void returns200WhenApiKeyMatches() throws Exception {
    mockMvc.perform(get("/internal/runtime/info")
        .header("X-Internal-Api-Key", "test-internal-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.uploadPolicy.post.maxCount").value(10))
        .andExpect(jsonPath("$.data.ledgerDjangoHealth").value("HEALTHY"))
        .andExpect(jsonPath("$.data.lastErrorCode").value("LEDGER_OK"));
  }
}
