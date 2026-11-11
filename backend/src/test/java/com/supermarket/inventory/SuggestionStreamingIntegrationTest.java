package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.supermarket.inventory.ai.client.*;
import com.supermarket.inventory.ai.enums.SuggestionType;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SuggestionStreamingIntegrationTest.ClientConfiguration.class)
class SuggestionStreamingIntegrationTest {
  @TestConfiguration
  static class ClientConfiguration {
    @Bean
    @Primary
    Fake client() {
      return new Fake();
    }
  }

  static class Fake implements SuggestionClient {
    CountDownLatch first = new CountDownLatch(1), release = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    String selected;

    public Identity identity() {
      return new Identity("FIXTURE", "qwen-plus", true);
    }

    public Result generate(SuggestionType t, String s) {
      throw new IllegalStateException("Streaming only test");
    }

    public Result stream(
        SuggestionType t, String s, String model, java.util.function.Consumer<String> delta) {
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
      calls.incrementAndGet();
      selected = model;
      delta.accept("### 数据现象\n\n");
      first.countDown();
      try {
        assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
      delta.accept("**夹具**，非真实调用。");
      return new Result(
          "### 数据现象\n\n**夹具**，非真实调用。",
          new Identity("FIXTURE", model, true),
          new TokenUsage(20L, 10L, 30L, 0L, "test-only"));
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired Fake client;
  String admin, buyer, key;
  String beforeSettings;

  String login(String name) throws Exception {
    return json.readTree(
            mvc.perform(
                    post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(
                            json.writeValueAsString(
                                Map.of("username", name, "password", "Test123456"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("data")
        .path("token")
        .asText();
  }

  @BeforeEach
  void setup() throws Exception {
    assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class))
        .startsWith("supermarket_inventory_")
        .endsWith("test");
    admin = login("admin");
    buyer = login("buyer01");
    key = UUID.randomUUID().toString();
    client.calls.set(0);
    client.first = new CountDownLatch(1);
    client.release = new CountDownLatch(1);
    var rows =
        jdbc.queryForList(
            "SELECT CAST(config_value AS CHAR) value FROM sys_config WHERE config_key='ai.provider_settings'");
    beforeSettings = rows.isEmpty() ? null : rows.get(0).get("value").toString();
  }

  @AfterEach
  void cleanup() {
    client.release.countDown();
    jdbc.update("DELETE FROM operation_log WHERE target_type='AI' AND target_no=?", key);
    jdbc.update("DELETE FROM ai_suggestion WHERE request_key=?", key);
    if (beforeSettings == null)
      jdbc.update("DELETE FROM sys_config WHERE config_key='ai.provider_settings'");
    else
      jdbc.update(
          "UPDATE sys_config SET config_value=? WHERE config_key='ai.provider_settings'",
          beforeSettings);
  }

  @Test
  void authenticatedSseArrivesBeforeFinishPersistsUsageAndSameKeyNeverCallsAgain()
      throws Exception {
    var preview = Map.of("requestKey", key, "type", "INVENTORY", "model", "qwen-flash");
    mvc.perform(
            post("/api/v1/suggestions/preview")
                .header("Authorization", "Bearer " + buyer)
                .contentType("application/json")
                .content(json.writeValueAsString(preview)))
        .andExpect(status().isOk());
    // Configuration changes after preview must not replace the selected model.
    jdbc.update(
        "INSERT INTO sys_config(config_key,config_value,description,editable,updated_by,updated_at) VALUES('ai.provider_settings',?,'测试设置',1,1,NOW(6)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)",
        "{\"version\":1,\"defaultModel\":\"qwen-plus\",\"models\":[\"qwen-plus\",\"qwen-flash\"],\"encryptedKey\":null}");
    var pending =
        mvc.perform(
                post("/api/v1/suggestions/by-request/" + key + "/stream")
                    .header("Authorization", "Bearer " + buyer)
                    .contentType("application/json")
                    .content("{}"))
            .andExpect(request().asyncStarted())
            .andReturn();
    assertThat(client.first.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(pending.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
        .contains("event:delta", "数据现象")
        .doesNotContain("event:result");
    client.release.countDown();
    pending.getAsyncResult(5000);
    String text =
        mvc.perform(asyncDispatch(pending))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(text).contains("event:result", "\"totalTokens\":30", "\"fixture\":true");
    assertThat(client.selected).isEqualTo("qwen-flash");
    mvc.perform(
            post("/api/v1/suggestions/by-request/" + key + "/generate")
                .header("Authorization", "Bearer " + buyer)
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.record.modelName").value("qwen-flash"));
    assertThat(client.calls.get()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT total_tokens FROM ai_suggestion WHERE request_key=?", Long.class, key))
        .isEqualTo(30);
    mvc.perform(
            get("/api/v1/suggestions/by-request/" + key).header("Authorization", "Bearer " + admin))
        .andExpect(status().isNotFound());
  }

  @Test
  void anotherOwnerCannotStartAStream() throws Exception {
    mvc.perform(
            post("/api/v1/suggestions/preview")
                .header("Authorization", "Bearer " + buyer)
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("requestKey", key, "type", "INVENTORY"))))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/suggestions/by-request/" + key + "/stream")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isConflict());
    assertThat(client.calls.get()).isZero();
  }
}
