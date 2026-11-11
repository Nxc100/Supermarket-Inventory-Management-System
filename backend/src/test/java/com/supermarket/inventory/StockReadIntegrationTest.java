package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.Map;
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
class StockReadIntegrationTest {
  @Autowired org.mybatis.spring.SqlSessionTemplate sqlSession;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource source;
  @Autowired Clock clock;
  String admin, buyer, other, cashier;

  @BeforeEach
  void setup() throws Exception {
    try (var c = source.getConnection()) {
      assertThat(c.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
    admin = login("admin");
    buyer = login("buyer01");
    other = login("buyer02");
    cashier = login("cashier01");
  }

  String login(String username) throws Exception {
    return data(mvc.perform(
                post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("username", username, "password", "Test123456"))))
            .andExpect(status().isOk()))
        .path("token")
        .asText();
  }

  JsonNode data(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
  }

  JsonNode stock(String code, String token) throws Exception {
    return data(mvc.perform(
                get("/api/v1/inventory/products")
                    .param("keyword", code)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()))
        .path("records")
        .get(0);
  }

  @Test
  void bookAndBatchBalancesAgreeWhileFreezeDoesNotChangeSaleableQuantity() throws Exception {
    JsonNode paper = stock("P0007", admin);
    assertThat(paper.path("id").isTextual()).isTrue();
    assertThat(paper.path("bookQty").asLong()).isEqualTo(paper.path("batchQty").asLong());
    assertThat(paper.path("bookQty").asLong())
        .isEqualTo(jdbc.queryForObject("SELECT stock_qty FROM product WHERE id=7", Long.class));
    JsonNode frozen = stock("P0010", buyer);
    assertThat(frozen.path("frozen").asBoolean()).isTrue();
    assertThat(frozen.path("saleableQty").asLong())
        .isEqualTo(frozen.path("bookQty").asLong())
        .isPositive();
    JsonNode disabled = stock("P0013", buyer);
    assertThat(disabled.path("enabled").asBoolean()).isFalse();
    assertThat(disabled.path("saleableQty").asLong()).isZero();
  }

  @Test
  void expiryTodayIsSaleableAndDateChangeRecomputesWithoutWritingBalances() throws Exception {
    LocalDate today = LocalDate.now(clock);
    long before = jdbc.queryForObject("SELECT stock_qty FROM product WHERE id=1", Long.class);
    jdbc.update("UPDATE purchase_item SET expiry_date=? WHERE product_id=1", today);
    JsonNode current = stock("P0001", buyer);
    assertThat(current.path("saleableQty").asLong()).isEqualTo(before);
    assertThat(current.path("expiredQty").asLong()).isZero();
    jdbc.update("UPDATE purchase_item SET expiry_date=? WHERE product_id=1", today.minusDays(1));
    sqlSession.clearCache();
    JsonNode expired = stock("P0001", buyer);
    assertThat(expired.path("saleableQty").asLong()).isZero();
    assertThat(expired.path("expiredQty").asLong()).isEqualTo(before);
    assertThat(expired.path("bookQty").asLong()).isEqualTo(before);
  }

  @Test
  void sharedBatchTraceDoesNotGrantAnotherBuyersPurchaseDetail() throws Exception {
    JsonNode own = batch(buyer), foreign = batch(other);
    assertThat(own.path("sourceReadable").asBoolean()).isTrue();
    assertThat(foreign.path("sourceReadable").asBoolean()).isFalse();
    assertThat(foreign.path("purchaseNo").asText()).isEqualTo("CG-DEMO-0001");
    assertThat(foreign.path("unitCost").isTextual()).isTrue();
    assertThat(foreign.has("supplierPhone")).isFalse();
    mvc.perform(get("/api/v1/purchases/1").header("Authorization", "Bearer " + other))
        .andExpect(status().isNotFound());
    JsonNode movement =
        data(mvc.perform(
                    get("/api/v1/inventory/movements")
                        .param("keyword", "CG-DEMO-0001")
                        .param("productId", "7")
                        .param("eventType", "PURCHASE_IN")
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isOk()))
            .path("records")
            .get(0);
    assertThat(movement.path("sourceReadable").asBoolean()).isFalse();
    assertThat(movement.path("deltaQty").asLong())
        .isEqualTo(
            movement.path("productAfter").asLong() - movement.path("productBefore").asLong());
  }

  JsonNode batch(String token) throws Exception {
    return data(mvc.perform(
                get("/api/v1/inventory/products/7/batches")
                    .param("keyword", "CG-DEMO-0001")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()))
        .path("records")
        .get(0);
  }

  @Test
  void cashierCannotReachOperationalStockCostOrMovementEndpoints() throws Exception {
    for (String url :
        new String[] {
          "/api/v1/inventory/products",
          "/api/v1/inventory/products/7/batches",
          "/api/v1/inventory/movements"
        })
      mvc.perform(get(url).header("Authorization", "Bearer " + cashier))
          .andExpect(status().isForbidden());
    String safe =
        mvc.perform(
                get("/api/v1/inventory/saleable-products")
                    .param("keyword", "P0007")
                    .header("Authorization", "Bearer " + cashier))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(safe).doesNotContain("unitCost", "purchaseId", "supplier", "bookQty");
    mvc.perform(
            get("/api/v1/inventory/movements")
                .param("productId", "9223372036854775808")
                .header("Authorization", "Bearer " + buyer))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/inventory/products/999999999/batches")
                .header("Authorization", "Bearer " + buyer))
        .andExpect(status().isNotFound());
  }
}
