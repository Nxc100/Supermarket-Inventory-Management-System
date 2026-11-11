package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.supermarket.inventory.catalog.service.ProductImageService;
import com.supermarket.inventory.security.*;
import java.nio.file.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductImageIntegrationTest {
  @TempDir static Path directory;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("app.upload-directory", () -> directory.toString());
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource dataSource;
  @Autowired ProductImageService images;
  @Autowired AccountMapper accounts;
  @Autowired AccountPrincipalService principals;
  String token, id;
  CurrentUser admin;

  @BeforeEach
  void setup() throws Exception {
    try (var c = dataSource.getConnection()) {
      assertThat(c.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
    token =
        json.readTree(
                mvc.perform(
                        post("/api/v1/auth/login")
                            .contentType("application/json")
                            .content("{\"username\":\"admin\",\"password\":\"Test123456\"}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("data")
            .path("token")
            .asText();
    admin = principals.principal(accounts.findByUsername("admin"));
    var input =
        json.createObjectNode()
            .put("code", "IMG_" + UUID.randomUUID().toString().substring(0, 8))
            .put("name", "商品图片验证")
            .put("categoryId", "1")
            .put("unit", "件")
            .put("salePrice", "5.00")
            .put("managesExpiry", false)
            .put("nearExpiryDays", 0)
            .put("stockLowerLimit", 0)
            .put("canPurchase", true)
            .put("canSell", true);
    id =
        json.readTree(
                mvc.perform(
                        post("/api/v1/products")
                            .header("Authorization", "Bearer " + token)
                            .contentType("application/json")
                            .content(input.toString()))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("data")
            .path("id")
            .asText();
  }

  @AfterEach
  void cleanup() {
    if (id != null) {
      if (jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE id=?", Long.class, id) > 0)
        images.delete(Long.parseLong(id), admin);
      jdbc.update("DELETE FROM operation_log WHERE target_type='PRODUCT' AND target_id=?", id);
    }
  }

  String upload() throws Exception {
    return json.readTree(
            mvc.perform(
                    multipart("/api/v1/products/" + id + "/image")
                        .file(
                            new MockMultipartFile(
                                "file", "product.png", "image/png", ImageStorageTest.png(80, 80)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("data")
        .path("imagePath")
        .asText();
  }

  @Test
  void imageLifecycleIsCommittedAndFailedUploadKeepsCurrentImage() throws Exception {
    String first = upload();
    assertThat(directory.resolve(first)).exists();
    mvc.perform(
            multipart("/api/v1/products/" + id + "/image")
                .file(new MockMultipartFile("file", "fake.png", "image/png", "fake".getBytes()))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
    assertThat(jdbc.queryForObject("SELECT image_path FROM product WHERE id=?", String.class, id))
        .isEqualTo(first);
    String second = upload();
    assertThat(directory.resolve(first)).doesNotExist();
    assertThat(directory.resolve(second)).exists();
    mvc.perform(
            delete("/api/v1/products/" + id + "/image").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(directory.resolve(second)).doesNotExist();
  }

  @Test
  void deletionReclaimsImageAndMissingProductNeverCreatesAnOrphan() throws Exception {
    String path = upload();
    mvc.perform(delete("/api/v1/products/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(directory.resolve(path)).doesNotExist();
    mvc.perform(
            multipart("/api/v1/products/" + id + "/image")
                .file(
                    new MockMultipartFile(
                        "file", "product.png", "image/png", ImageStorageTest.png(80, 80)))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
    try (var files = Files.list(directory)) {
      assertThat(files.count()).isZero();
    }
  }
}
