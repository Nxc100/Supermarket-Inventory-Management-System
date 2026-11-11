package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
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
class CashierInventoryIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbc;
  String token;

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
                            .content("{\"username\":\"cashier01\",\"password\":\"Test123456\"}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("data")
            .path("token")
            .asText();
  }

  JsonNode result(String keyword) throws Exception {
    return json.readTree(
            mvc.perform(
                    get("/api/v1/inventory/saleable-products")
                        .param("keyword", keyword)
                        .param("includeCost", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("data");
  }

  @Test
  void frozenStockKeepsItsQuantityAndResponseHasOnlyAllowedFields() throws Exception {
    JsonNode row = result("P0010").path("records").get(0);
    assertThat(row.path("saleableQty").asLong()).isEqualTo(20);
    assertThat(row.path("frozen").asBoolean()).isTrue();
    Set<String> fields = new HashSet<>();
    row.fieldNames().forEachRemaining(fields::add);
    assertThat(fields)
        .containsExactlyInAnyOrder(
            "id",
            "code",
            "barcode",
            "name",
            "categoryId",
            "categoryName",
            "specification",
            "unit",
            "salePrice",
            "imagePath",
            "saleableQty",
            "frozen");
    assertThat(result("已停用历史商品").path("records").get(0).path("saleableQty").asLong()).isZero();
    assertThat(result("禁止销售商品").path("records").get(0).path("saleableQty").asLong()).isZero();
  }

  @Test
  void filtersAndPaginationUseSameCurrentData() throws Exception {
    var latest =
        jdbc.queryForObject("SELECT MAX(created_at) FROM product", java.time.LocalDateTime.class);
    jdbc.update("UPDATE product SET created_at=? WHERE id=1", latest.plusDays(1));
    mvc.perform(
            get("/api/v1/inventory/saleable-products").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[0].id").value("1"));
    JsonNode row = result("6900000000010").path("records").get(0);
    assertThat(row.path("code").asText()).isEqualTo("P0010");
    long expected =
        jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE category_id=1", Long.class);
    mvc.perform(
            get("/api/v1/inventory/saleable-products")
                .param("categoryId", "1")
                .param("pageSize", "20")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(expected))
        .andExpect(jsonPath("$.data.pageSize").value(20));
    mvc.perform(
            get("/api/v1/inventory/saleable-products")
                .param("pageSize", "1000")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void endpointIsReadOnlyAndRequiresAuthentication() throws Exception {
    long before = jdbc.queryForObject("SELECT SUM(stock_qty) FROM product", Long.class);
    mvc.perform(get("/api/v1/inventory/saleable-products")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/inventory/saleable-products")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"stockQty\":9999}"))
        .andExpect(status().isMethodNotAllowed());
    assertThat(jdbc.queryForObject("SELECT SUM(stock_qty) FROM product", Long.class))
        .isEqualTo(before);
  }
}
