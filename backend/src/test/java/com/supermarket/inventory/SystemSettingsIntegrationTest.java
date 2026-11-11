package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SystemSettingsIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource source;
  String admin, buyer, cashier;

  @BeforeEach
  void setup() throws Exception {
    try (var c = source.getConnection()) {
      assertThat(c.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
    admin = login("admin");
    buyer = login("buyer01");
    cashier = login("cashier01");
  }

  String login(String name) throws Exception {
    return data(mvc.perform(
                post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("username", name, "password", "Test123456"))))
            .andExpect(status().isOk()))
        .path("token")
        .asText();
  }

  JsonNode data(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
  }

  JsonNode defaults() throws Exception {
    return data(
        mvc.perform(get("/api/v1/system/defaults").header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk()));
  }

  ObjectNode update(JsonNode original, long lower, int near) {
    return json.createObjectNode()
        .put("stockLowerLimit", lower)
        .put("nearExpiryDays", near)
        .put("expectedStockLowerLimit", original.path("stockLowerLimit").asLong())
        .put("expectedNearExpiryDays", original.path("nearExpiryDays").asInt());
  }

  ResultActions save(ObjectNode body, String token) throws Exception {
    return mvc.perform(
        put("/api/v1/system/defaults")
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content(body.toString()));
  }

  @Test
  void changingDefaultsPreservesExistingProductsAndAuditsBothValuesAtomically() throws Exception {
    var before = defaults();
    var product =
        jdbc.queryForMap(
            "SELECT stock_lower_limit,near_expiry_days,stock_qty FROM product WHERE id=1");
    long lower = before.path("stockLowerLimit").asLong() == 0 ? 9 : 0;
    int near = before.path("nearExpiryDays").asInt() == 0 ? 3 : 0;
    var result = data(save(update(before, lower, near), admin).andExpect(status().isOk()));
    assertThat(result.path("stockLowerLimit").asLong()).isEqualTo(lower);
    assertThat(
            jdbc.queryForMap(
                "SELECT stock_lower_limit,near_expiry_days,stock_qty FROM product WHERE id=1"))
        .isEqualTo(product);
    var newDefaults =
        data(
            mvc.perform(get("/api/v1/products/defaults").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()));
    assertThat(newDefaults.path("stockLowerLimit").asLong()).isEqualTo(lower);
    assertThat(newDefaults.path("nearExpiryDays").asInt()).isEqualTo(near);
    var log =
        jdbc.queryForMap(
            "SELECT before_data,after_data FROM operation_log WHERE action='CONFIG_UPDATE' ORDER BY id DESC LIMIT 1");
    assertThat(json.readTree(log.get("before_data").toString())).isEqualTo(before);
    assertThat(json.readTree(log.get("after_data").toString())).isEqualTo(result);
    save(update(before, 9, 8), admin).andExpect(status().isConflict());
    assertThat(defaults()).isEqualTo(result);
  }

  @Test
  void defaultsEnforceRolesLimitsKnownFieldsAndIndependentSavePermission() throws Exception {
    var before = defaults();
    for (String token : List.of(buyer, cashier)) {
      mvc.perform(get("/api/v1/system/defaults").header("Authorization", "Bearer " + token))
          .andExpect(status().isForbidden());
      save(update(before, 1, 2), token).andExpect(status().isForbidden());
    }
    save(update(before, -1, 0), admin).andExpect(status().isBadRequest());
    save(update(before, 1, 3651), admin).andExpect(status().isBadRequest());
    save(update(before, 1, 2).put("apiKey", "must-not-be-accepted"), admin)
        .andExpect(status().isBadRequest());
    save(update(before, 1, 2).put("stockLowerLimit", 1.5), admin)
        .andExpect(status().isBadRequest());
    var permissions =
        json.readTree(
            jdbc.queryForObject(
                "SELECT permissions FROM sys_role WHERE code='ADMIN'", String.class));
    var remaining = json.createArrayNode();
    for (var p : permissions) if (!p.asText().equals("system.config.save")) remaining.add(p);
    jdbc.update(
        "UPDATE sys_role SET permissions=?,auth_version=auth_version+1 WHERE code='ADMIN'",
        remaining.toString());
    admin = login("admin");
    save(update(before, 1, 2), admin).andExpect(status().isForbidden());
    assertThat(defaults()).isEqualTo(before);
  }

  @Test
  void logFiltersAndDetailsStayAdminOnlyAndRedactSensitiveSnapshotFields() throws Exception {
    String marker = "QA_LOG_" + UUID.randomUUID();
    jdbc.update(
        "INSERT INTO operation_log(actor_id,actor_name,action,target_type,target_id,target_no,result,reason,before_data,after_data) VALUES(1,'日志测试员','CONFIG_UPDATE','CONFIG','42',?,'SUCCESS',?, ?, ?)",
        marker,
        "过滤验证",
        "{\"passwordHash\":\"never-return-secret\",\"userId\":42}",
        "{\"nested\":{\"apiKey\":\"never-return-secret\"},\"stockLowerLimit\":3}");
    var list =
        data(
            mvc.perform(
                    get("/api/v1/system/logs")
                        .param("keyword", marker)
                        .param("actor", "日志测试员")
                        .param("action", "CONFIG_UPDATE")
                        .param("result", "SUCCESS")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()));
    assertThat(list.path("total").asLong()).isEqualTo(1);
    String id = list.path("records").get(0).path("id").asText();
    var detail =
        data(
            mvc.perform(get("/api/v1/system/logs/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()));
    assertThat(detail.path("record").path("targetId").isTextual()).isTrue();
    assertThat(detail.path("before").path("userId").isTextual()).isTrue();
    assertThat(detail.toString()).doesNotContain("never-return-secret").contains("已隐藏");
    var empty =
        data(
            mvc.perform(
                    get("/api/v1/system/logs")
                        .param("keyword", marker)
                        .param("result", "FAILURE")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()));
    assertThat(empty.path("total").asLong()).isZero();
    for (String token : List.of(buyer, cashier))
      for (String path :
          List.of(
              "/api/v1/system/logs", "/api/v1/system/logs/actions", "/api/v1/system/logs/" + id))
        mvc.perform(get(path).header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/system/logs")
                .param("startDate", "2026-11-12")
                .param("endDate", "2026-11-11")
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isBadRequest());
  }
}
