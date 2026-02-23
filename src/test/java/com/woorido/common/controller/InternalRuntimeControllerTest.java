package com.woorido.common.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.woorido.common.exception.GlobalExceptionHandler;
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
    InternalRuntimeController controller = new InternalRuntimeController(
        new MockEnvironment(),
        beanFactory.getBeanProvider(BuildProperties.class),
        beanFactory.getBeanProvider(GitProperties.class),
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
        .andExpect(jsonPath("$.data.uploadPolicy.post.maxCount").value(10));
  }
}
