package com.woorido.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new ThrowController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void mapsAuthToUnauthorized() throws Exception {
    mockMvc.perform(get("/test/runtime").param("message", "AUTH_001:인증이 필요합니다"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("AUTH_001:인증이 필요합니다"));
  }

  @Test
  void mapsMemberToForbidden() throws Exception {
    mockMvc.perform(get("/test/runtime").param("message", "MEMBER_001:권한 없음"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("MEMBER_001:권한 없음"));
  }

  @Test
  void mapsChallengeNotFoundTo404() throws Exception {
    mockMvc.perform(get("/test/illegal").param("message", "CHALLENGE_001:없음"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("CHALLENGE_001:없음"));
  }

  @Test
  void mapsChallengeConflictTo409() throws Exception {
    mockMvc.perform(get("/test/runtime").param("message", "CHALLENGE_011:중복"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("CHALLENGE_011:중복"));
  }

  @Test
  void mapsUserConflictTo409() throws Exception {
    mockMvc.perform(get("/test/runtime").param("message", "USER_007:중복 닉네임"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("USER_007:중복 닉네임"));
  }

  @Test
  void mapsPostNotFoundTo404() throws Exception {
    mockMvc.perform(get("/test/illegal").param("message", "POST_001:없음"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("POST_001:없음"));
  }

  @Test
  void mapsImageValidationTo400() throws Exception {
    mockMvc.perform(get("/test/illegal").param("message", "IMAGE_002:용량 초과"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("IMAGE_002:용량 초과"));
  }

  @Test
  void mapsLedgerDjangoUnavailableTo503() throws Exception {
    mockMvc.perform(get("/test/runtime").param("message", "LEDGER_004:Django ledger service unavailable"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("LEDGER_004:Django ledger service unavailable"));
  }

  @Test
  void mapsLedgerAuthMismatchTo502() throws Exception {
    mockMvc.perform(get("/test/runtime").param("message", "LEDGER_010:Django ledger 인증/설정 불일치"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("LEDGER_010:Django ledger 인증/설정 불일치"));
  }

  @Test
  void mapsLedgerContractMismatchTo502() throws Exception {
    mockMvc.perform(get("/test/runtime").param("message", "LEDGER_011:Django ledger 응답 계약 불일치"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("LEDGER_011:Django ledger 응답 계약 불일치"));
  }

  @Test
  void mapsUnknownTo500WithoutInternalMessageLeak() throws Exception {
    mockMvc.perform(get("/test/runtime").param("message", "UNKNOWN_ERROR:debug detail")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다"));
  }

  @RestController
  static class ThrowController {
    @GetMapping("/test/runtime")
    void throwRuntime(@RequestParam String message) {
      throw new RuntimeException(message);
    }

    @GetMapping("/test/illegal")
    void throwIllegal(@RequestParam String message) {
      throw new IllegalArgumentException(message);
    }
  }
}
