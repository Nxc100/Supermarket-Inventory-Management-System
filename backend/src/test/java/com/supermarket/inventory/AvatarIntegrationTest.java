package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.inventory.profile.service.AvatarService;
import java.nio.file.*;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;

/** File effects are tested across real committed binding transactions, not rollback-only mocks. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvatarIntegrationTest {
  @TempDir static Path directory;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("app.upload-directory", () -> directory.toString());
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource dataSource;
  @Autowired PasswordEncoder passwords;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired AvatarService avatars;
  long userId;
  String token;

  @BeforeEach
  void account() throws Exception {
    try (var connection = dataSource.getConnection()) {
      assertThat(connection.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
    String name = "avatar_" + UUID.randomUUID().toString().substring(0, 8);
    jdbc.update(
        "INSERT INTO sys_user(username,password_hash,real_name,role_code,avatar_path) VALUES(?,?,?,'CASHIER',NULL)",
        name,
        passwords.encode("Test123456"),
        "头像验证");
    userId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, name);
    var result =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            java.util.Map.of("username", name, "password", "Test123456"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    token = json.readTree(result).path("data").path("token").asText();
  }

  @AfterEach
  void cleanup() {
    if (userId != 0) {
      avatars.remove(userId);
      jdbc.update("DELETE FROM sys_user WHERE id=?", userId);
    }
  }

  String upload() throws Exception {
    var response =
        mvc.perform(
                multipart("/api/v1/profile/avatar")
                    .file(
                        new MockMultipartFile(
                            "file", "avatar.png", "image/png", ImageStorageTest.png(80, 80)))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(response).path("data").path("avatarPath").asText();
  }

  @Test
  void uploadReplaceInvalidUploadAndRemovePreserveExpectedBinding() throws Exception {
    String first = upload();
    mvc.perform(
            multipart("/api/v1/profile/avatar")
                .file(
                    new MockMultipartFile(
                        "file", "avatar.png", "image/png", ImageStorageTest.png(80, 80)))
                .param("userId", "1")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/media/" + first))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"));
    assertThat(
            jdbc.queryForObject(
                "SELECT avatar_path FROM sys_user WHERE id=?", String.class, userId))
        .isEqualTo(first);
    mvc.perform(
            multipart("/api/v1/profile/avatar")
                .file(
                    new MockMultipartFile("file", "fake.png", "image/png", "not image".getBytes()))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
    assertThat(
            jdbc.queryForObject(
                "SELECT avatar_path FROM sys_user WHERE id=?", String.class, userId))
        .isEqualTo(first);
    String second = upload();
    assertThat(directory.resolve(first)).doesNotExist();
    assertThat(directory.resolve(second)).exists();
    mvc.perform(delete("/api/v1/profile/avatar").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.avatarPath").isEmpty());
    assertThat(directory.resolve(second)).doesNotExist();
  }

  @Test
  void sharedProductImageIsNotReclaimedByAvatarReplacement() throws Exception {
    String first = upload();
    String previous =
        jdbc.queryForObject("SELECT image_path FROM product WHERE id=1", String.class);
    jdbc.update("UPDATE product SET image_path=? WHERE id=1", first);
    try {
      upload();
      assertThat(directory.resolve(first)).exists();
    } finally {
      jdbc.update("UPDATE product SET image_path=? WHERE id=1", previous);
      Files.deleteIfExists(directory.resolve(first));
    }
  }

  @Test
  void imageSharedByAnotherUserIsNotReclaimed() throws Exception {
    String first = upload();
    String sharedUser = "shared_" + UUID.randomUUID().toString().substring(0, 8);
    jdbc.update(
        "INSERT INTO sys_user(username,password_hash,real_name,role_code,avatar_path) SELECT ?,password_hash,'共享头像','CASHIER',? FROM sys_user WHERE id=?",
        sharedUser,
        first,
        userId);
    try {
      upload();
      assertThat(directory.resolve(first)).exists();
    } finally {
      jdbc.update("DELETE FROM sys_user WHERE username=?", sharedUser);
      Files.deleteIfExists(directory.resolve(first));
    }
  }

  @Test
  void deletingAnUnusedAccountReclaimsItsAvatarOnlyAfterTheAccountCommit() throws Exception {
    String path = upload();
    String administrator =
        json.readTree(
                mvc.perform(
                        post("/api/v1/auth/login")
                            .contentType("application/json")
                            .content(
                                json.writeValueAsString(
                                    java.util.Map.of(
                                        "username", "admin", "password", "Test123456"))))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("data")
            .path("token")
            .asText();
    var row =
        json.readTree(
                mvc.perform(
                        get("/api/v1/system/users/" + userId)
                            .header("Authorization", "Bearer " + administrator))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("data");
    long deleted = userId;
    mvc.perform(
            delete("/api/v1/system/users/" + userId)
                .header("Authorization", "Bearer " + administrator)
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        java.util.Map.of("expectedUpdatedAt", row.path("updatedAt").asText()))))
        .andExpect(status().isOk());
    userId = 0;
    try {
      assertThat(
              jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id=?", Long.class, deleted))
          .isZero();
      assertThat(directory.resolve(path)).doesNotExist();
      mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isUnauthorized());
    } finally {
      jdbc.update(
          "DELETE FROM operation_log WHERE target_type='USER' AND target_id=?",
          Long.toString(deleted));
    }
  }
}
