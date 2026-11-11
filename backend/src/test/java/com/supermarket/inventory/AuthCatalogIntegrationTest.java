package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Real MySQL + actual JWT filter + actual services, all per-test writes rolled back. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthCatalogIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource dataSource;
  @Autowired PasswordEncoder passwords;

  @BeforeEach
  void ensureIsolatedDatabase() throws Exception {
    try (var connection = dataSource.getConnection()) {
      assertThat(connection.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
    jdbc.update(
        "INSERT INTO sys_user(username,password_hash,real_name,role_code) VALUES(?,?,?,?)",
        "qa_account",
        passwords.encode("Test123456"),
        "验证账号",
        "CASHIER");
  }

  String login(String username) throws Exception {
    String result =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            java.util.Map.of("username", username, "password", "Test123456"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(result).path("data").path("token").asText();
  }

  @Test
  void threeRolesLoginAndSensitiveFieldsNeverLeaveServer() throws Exception {
    for (String name : new String[] {"admin", "buyer01", "cashier01"}) {
      String token = login(name);
      String result =
          mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.data.id").isString())
              .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
              .andExpect(jsonPath("$.data.credentialVersion").doesNotExist())
              .andReturn()
              .getResponse()
              .getContentAsString();
      assertThat(json.readTree(result).path("data").path("permissions").size()).isPositive();
    }
  }

  @Test
  void failedAndDisabledLoginHaveClearErrors() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"missing\",\"password\":\"bad\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("账号或密码错误"));
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"disabled01\",\"password\":\"Test123456\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
  }

  @Test
  void categoryApisRejectAnonymousAndOrdinaryRoles() throws Exception {
    mvc.perform(get("/api/v1/categories")).andExpect(status().isUnauthorized());
    for (String name : new String[] {"buyer01", "cashier01"}) {
      String token = login(name);
      mvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + token))
          .andExpect(status().isForbidden());
      mvc.perform(
              post("/api/v1/categories")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"code\":\"QA\",\"name\":\"验证分类\",\"sortOrder\":0}"))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void logoutInvalidatesAllExistingTokens() throws Exception {
    String first = login("qa_account");
    String second = login("qa_account");
    mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + first))
        .andExpect(status().isOk());
    for (String token : new String[] {first, second})
      mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isUnauthorized());
  }

  @Test
  void accountAndRoleVersionChangesAreCheckedOnEveryRequest() throws Exception {
    String token = login("qa_account");
    jdbc.update("UPDATE sys_role SET auth_version=auth_version+1 WHERE code='CASHIER'");
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
    token = login("qa_account");
    jdbc.update(
        "UPDATE sys_user SET enabled=0,credential_version=credential_version+1 WHERE username='qa_account'");
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void profileIsSelfOnlyAndRejectsProtectedFields() throws Exception {
    String token = login("qa_account");
    mvc.perform(
            patch("/api/v1/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"realName\":\"新的姓名\",\"phone\":null,\"roleCode\":\"ADMIN\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/api/v1/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"realName\":\"新的姓名\",\"phone\":\"13912345678\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.realName").value("新的姓名"))
        .andExpect(jsonPath("$.data.roleCode").value("CASHIER"));
    assertThat(
            jdbc.queryForObject(
                "SELECT real_name FROM sys_user WHERE username='qa_account'", String.class))
        .isEqualTo("新的姓名");
    mvc.perform(
            patch("/api/v1/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"realName\":\"合法姓名\",\"phone\":\"123\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void passwordRejectsWrongOldAndInvalidatesTokenAfterSuccess() throws Exception {
    String token = login("qa_account");
    String hash =
        jdbc.queryForObject(
            "SELECT password_hash FROM sys_user WHERE username='qa_account'", String.class);
    mvc.perform(
            post("/api/v1/profile/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"oldPassword\":\"wrong\",\"newPassword\":\"Next123456\",\"confirmPassword\":\"Next123456\"}"))
        .andExpect(status().isBadRequest());
    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash FROM sys_user WHERE username='qa_account'", String.class))
        .isEqualTo(hash);
    mvc.perform(
            post("/api/v1/profile/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"oldPassword\":\"Test123456\",\"newPassword\":\"Next123456\",\"confirmPassword\":\"Next123456\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
    assertThat(
            passwords.matches(
                "Next123456",
                jdbc.queryForObject(
                    "SELECT password_hash FROM sys_user WHERE username='qa_account'",
                    String.class)))
        .isTrue();
  }

  @Test
  void categoryFullLifecycleAndTransactionalAudit() throws Exception {
    String token = login("admin");
    String result =
        mvc.perform(
                post("/api/v1/categories")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"QA_CATEGORY\",\"name\":\"验证分类\",\"sortOrder\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = json.readTree(result).path("data").path("id").asText();
    mvc.perform(
            patch("/api/v1/categories/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"QA_CATEGORY\",\"name\":\"更名分类\",\"sortOrder\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("更名分类"));
    mvc.perform(
            post("/api/v1/categories/" + id + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.enabled").value(false));
    mvc.perform(
            get("/api/v1/categories")
                .param("keyword", "QA_CATEGORY")
                .param("enabled", "false")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.pageSize").value(10));
    mvc.perform(delete("/api/v1/categories/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM operation_log WHERE target_type='CATEGORY' AND target_id=?",
                Integer.class,
                id))
        .isEqualTo(4);
  }

  @Test
  void categoryUniquenessReferencesAndValidationAreEnforced() throws Exception {
    String token = login("admin");
    mvc.perform(
            post("/api/v1/categories")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"C001\",\"name\":\"重复编码\",\"sortOrder\":0}"))
        .andExpect(status().isConflict());
    mvc.perform(delete("/api/v1/categories/1").header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/v1/categories")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"INVALID-!\",\"name\":\"短\",\"sortOrder\":-1}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/categories")
                .param("pageSize", "10000")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category WHERE id=1", Integer.class))
        .isEqualTo(1);
  }
}
