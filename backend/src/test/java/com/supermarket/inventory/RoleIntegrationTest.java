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
class RoleIntegrationTest {
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

  JsonNode data(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
  }

  ResultActions write(String method, String path, String token, JsonNode body) throws Exception {
    return mvc.perform(
        request(org.springframework.http.HttpMethod.valueOf(method), path)
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content(body.toString()));
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

  JsonNode role(String code) throws Exception {
    return data(
        mvc.perform(get("/api/v1/system/roles/" + code).header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk()));
  }

  ObjectNode body(JsonNode role, String remove, String add) {
    var result =
        json.createObjectNode().put("expectedVersion", role.path("role").path("version").asLong());
    var list = result.putArray("permissions");
    for (var p : role.path("permissions")) if (!p.asText().equals(remove)) list.add(p.asText());
    if (add != null) list.add(add);
    return result;
  }

  @Test
  void fixedRolesExposeOnlyTheirCeilingAndDescribeTheRealDataScopes() throws Exception {
    var roles =
        data(
            mvc.perform(get("/api/v1/system/roles").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()));
    assertThat(roles.size()).isEqualTo(3);
    for (String code : List.of("ADMIN", "PURCHASER", "CASHIER")) {
      var d = role(code);
      var allowed = new HashSet<String>();
      for (var n : d.path("nodes")) {
        allowed.add(n.path("code").asText());
        assertThat(n.path("label").asText()).containsPattern("[\\p{IsHan}]");
      }
      for (var p : d.path("permissions")) assertThat(allowed).contains(p.asText());
      for (var p : d.path("protectedPermissions")) assertThat(allowed).contains(p.asText());
    }
    assertThat(role("PURCHASER").toString())
        .doesNotContain("\"ai.sales\"", "\"sale.order\"", "\"system.user\"");
    assertThat(role("CASHIER").toString())
        .doesNotContain("\"purchase.order\"", "\"ai.inventory\"", "\"inventory.stocktake.review\"");
  }

  @Test
  void saveAndRestoreInvalidateRelatedSessionsAndKeepBothAuditSnapshots() throws Exception {
    var before = role("PURCHASER");
    var changed =
        data(
            write(
                    "PUT",
                    "/api/v1/system/roles/PURCHASER/permissions",
                    admin,
                    body(before, "ai.inventory", null))
                .andExpect(status().isOk()));
    assertThat(changed.path("role").path("version").asLong())
        .isEqualTo(before.path("role").path("version").asLong() + 1);
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + buyer))
        .andExpect(status().isUnauthorized());
    buyer = login("buyer01");
    mvc.perform(get("/api/v1/suggestions/context").header("Authorization", "Bearer " + buyer))
        .andExpect(status().isForbidden());
    var restored =
        data(
            write(
                    "POST",
                    "/api/v1/system/roles/PURCHASER/restore",
                    admin,
                    json.createObjectNode()
                        .put("expectedVersion", changed.path("role").path("version").asLong()))
                .andExpect(status().isOk()));
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + buyer))
        .andExpect(status().isUnauthorized());
    buyer = login("buyer01");
    mvc.perform(get("/api/v1/suggestions/context").header("Authorization", "Bearer " + buyer))
        .andExpect(status().isOk());
    assertThat(restored.path("permissions")).isEqualTo(before.path("permissions"));
    write("PUT", "/api/v1/system/roles/PURCHASER/permissions", admin, body(before, null, null))
        .andExpect(status().isConflict());
    var records =
        jdbc.queryForList(
            "SELECT before_data,after_data FROM operation_log WHERE target_type='ROLE' AND target_id='PURCHASER' ORDER BY id DESC LIMIT 2");
    assertThat(records).hasSize(2);
    assertThat(records.toString())
        .contains("ai.inventory")
        .doesNotContain("Test123456", "password_hash");
  }

  @Test
  void forgedCeilingsMissingParentsAndCriticalAdministratorPermissionRemovalAreRejected()
      throws Exception {
    var purchaser = role("PURCHASER");
    write(
            "PUT",
            "/api/v1/system/roles/PURCHASER/permissions",
            admin,
            body(purchaser, null, "purchase.order.review"))
        .andExpect(status().isForbidden());
    write(
            "PUT",
            "/api/v1/system/roles/PURCHASER/permissions",
            admin,
            body(purchaser, "purchase.order", null))
        .andExpect(status().isBadRequest());
    write(
            "PUT",
            "/api/v1/system/roles/PURCHASER/permissions",
            admin,
            body(purchaser, null, "common"))
        .andExpect(status().isBadRequest());
    var adminRole = role("ADMIN");
    write(
            "PUT",
            "/api/v1/system/roles/ADMIN/permissions",
            admin,
            body(adminRole, "system.role.save", null))
        .andExpect(status().isConflict());
    write(
            "PUT",
            "/api/v1/system/roles/PURCHASER/permissions",
            admin,
            body(purchaser, null, null).put("dataScope", "ALL"))
        .andExpect(status().isBadRequest());
    assertThat(role("ADMIN").path("role").path("version"))
        .isEqualTo(adminRole.path("role").path("version"));
  }

  @Test
  void ordinaryRolesCannotReadOrWriteTheConfigurationThroughAnyEntry() throws Exception {
    var before = role("CASHIER");
    for (String token : List.of(buyer, cashier)) {
      mvc.perform(get("/api/v1/system/roles").header("Authorization", "Bearer " + token))
          .andExpect(status().isForbidden());
      mvc.perform(get("/api/v1/system/roles/CASHIER").header("Authorization", "Bearer " + token))
          .andExpect(status().isForbidden());
      write("PUT", "/api/v1/system/roles/CASHIER/permissions", token, body(before, null, null))
          .andExpect(status().isForbidden());
      write(
              "POST",
              "/api/v1/system/roles/CASHIER/restore",
              token,
              json.createObjectNode()
                  .put("expectedVersion", before.path("role").path("version").asLong()))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void changingTheAdministratorsOwnRoleRequiresARealNewLoginButKeepsRecoveryPermissions()
      throws Exception {
    var before = role("ADMIN");
    var updated =
        data(
            write(
                    "PUT",
                    "/api/v1/system/roles/ADMIN/permissions",
                    admin,
                    body(before, "base.product.delete", null))
                .andExpect(status().isOk()));
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + admin))
        .andExpect(status().isUnauthorized());
    admin = login("admin");
    mvc.perform(delete("/api/v1/products/1").header("Authorization", "Bearer " + admin))
        .andExpect(status().isForbidden());
    write(
            "POST",
            "/api/v1/system/roles/ADMIN/restore",
            admin,
            json.createObjectNode()
                .put("expectedVersion", updated.path("role").path("version").asLong()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + admin))
        .andExpect(status().isUnauthorized());
    admin = login("admin");
    assertThat(role("ADMIN").path("permissions")).isEqualTo(before.path("permissions"));
  }
}
