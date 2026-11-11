package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.supermarket.inventory.ai.service.AiSettingsService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "app.ai.config-master-file=.local/secrets/test-ai-master.key",
      "app.ai.api-key=",
      "app.ai.key-file="
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiSettingsIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired AiSettingsService service;
  String admin, buyer, cashier;

  @BeforeEach
  void setup() throws Exception {
    assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class))
        .startsWith("supermarket_inventory_")
        .endsWith("test");
    admin = login("admin");
    buyer = login("buyer01");
    cashier = login("cashier01");
  }

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

  JsonNode getSettings() throws Exception {
    return json.readTree(
            mvc.perform(
                    get("/api/v1/system/ai/settings").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("data");
  }

  ResultActions save(JsonNode body, String actor) throws Exception {
    return mvc.perform(
        put("/api/v1/system/ai/settings")
            .header("Authorization", "Bearer " + actor)
            .contentType("application/json")
            .content(body.toString()));
  }

  com.fasterxml.jackson.databind.node.ObjectNode body(long version, String action) {
    return json.createObjectNode()
        .put("expectedVersion", version)
        .put("defaultModel", "qwen-flash")
        .put("keyAction", action)
        .set("models", json.valueToTree(List.of("qwen-plus", "qwen-flash")));
  }

  @Test
  void credentialsAreEncryptedWriteOnlyAuditedSafelyAndVersionProtected() throws Exception {
    String secret = "sk-test-private-never-a-real-key";
    long version = getSettings().path("version").asLong();
    var input = body(version, "REPLACE").put("apiKey", secret);
    String response =
        save(input, admin)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(response).doesNotContain(secret, "encryptedKey");
    var stored =
        json.readTree(
            jdbc.queryForObject(
                "SELECT CAST(config_value AS CHAR) FROM sys_config WHERE config_key='ai.provider_settings'",
                String.class));
    assertThat(stored.toString()).doesNotContain(secret);
    assertThat(stored.path("encryptedKey").asText()).isNotEmpty();
    assertThat(service.call("qwen-flash").apiKey()).isEqualTo(secret);
    assertThat(
            jdbc.queryForObject(
                "SELECT CAST(after_data AS CHAR) FROM operation_log WHERE action='AI_CONFIG_UPDATE' ORDER BY id DESC LIMIT 1",
                String.class))
        .doesNotContain(secret, "encryptedKey");
    save(input, admin).andExpect(status().isConflict());
    save(body(version + 1, "KEEP"), admin).andExpect(status().isOk());
    assertThat(service.call("qwen-flash").apiKey()).isEqualTo(secret);
    save(body(version + 2, "USE_LOCAL"), admin).andExpect(status().isOk());
    assertThat(getSettings().path("keyConfigured").asBoolean()).isFalse();
  }

  @Test
  void rolesAndInvalidModelListsCannotChangeSettings() throws Exception {
    for (String actor : List.of(buyer, cashier)) {
      mvc.perform(get("/api/v1/system/ai/settings").header("Authorization", "Bearer " + actor))
          .andExpect(status().isForbidden());
      mvc.perform(get("/api/v1/system/ai/usage").header("Authorization", "Bearer " + actor))
          .andExpect(status().isForbidden());
      save(body(0, "KEEP"), actor).andExpect(status().isForbidden());
    }
    long version = getSettings().path("version").asLong();
    var invalid = body(version, "KEEP").put("defaultModel", "qwen-missing");
    save(invalid, admin).andExpect(status().isBadRequest());
    save(body(version, "REPLACE").put("apiKey", "key with whitespace"), admin)
        .andExpect(status().isBadRequest());
    assertThat(getSettings().path("version").asLong()).isEqualTo(version);
  }

  @Test
  void usageSumsProviderNumbersKeepsUnknownAndExcludesFixtures() throws Exception {
    // Explicitly transient SQL projection fixtures, rolled back with this whole test. Never
    // real-call evidence.
    String model = "qwen-rollback-test";
    for (int i = 0; i < 3; i++)
      jdbc.update(
          "INSERT INTO ai_suggestion(request_key,suggestion_type,operator_id,operator_name,period_start,period_end,filter_data,input_summary,snapshot_at,provider,model_name,is_fixture,status,content,requested_at,finished_at,prompt_tokens,completion_tokens,total_tokens,cached_tokens) VALUES(?,'INVENTORY',1,'回滚用量测试',CURRENT_DATE(),CURRENT_DATE(),'{}','{}',NOW(6),'FIXTURE',?,?,'SUCCESS','测试投影，不是真实调用',NOW(6),NOW(6),?,?,?,?)",
          UUID.randomUUID().toString(),
          model,
          i == 2 ? 1 : 0,
          i == 1 ? null : 20,
          i == 1 ? null : 10,
          i == 1 ? null : 30,
          i == 1 ? null : 5);
    var r =
        json.readTree(
                mvc.perform(
                        get("/api/v1/system/ai/usage")
                            .param("model", model)
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("data");
    assertThat(r.path("totals").path("calls").asInt()).isEqualTo(2);
    assertThat(r.path("totals").path("reportedCalls").asInt()).isEqualTo(1);
    assertThat(r.path("totals").path("unreportedCalls").asInt()).isEqualTo(1);
    assertThat(r.path("totals").path("totalTokens").asInt()).isEqualTo(30);
    assertThat(r.path("models").size()).isEqualTo(1);
    mvc.perform(
            get("/api/v1/system/ai/usage")
                .param("startDate", "2026-12-01")
                .param("endDate", "2026-11-01")
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isBadRequest());
  }
}
