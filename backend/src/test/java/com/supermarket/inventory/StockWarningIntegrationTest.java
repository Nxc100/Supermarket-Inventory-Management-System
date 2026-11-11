package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(StockWarningIntegrationTest.TimeConfiguration.class)
class StockWarningIntegrationTest {
  @TestConfiguration
  static class TimeConfiguration {
    @Bean
    @Primary
    MutableClock saleTestClock() {
      return new MutableClock();
    }
  }

  static class MutableClock extends Clock {
    final AtomicReference<Instant> current =
        new AtomicReference<>(Instant.parse("2026-11-10T02:00:00Z"));

    public ZoneId getZone() {
      return ZoneId.of("Asia/Shanghai");
    }

    public Clock withZone(ZoneId zone) {
      return Clock.fixed(instant(), zone);
    }

    public Instant instant() {
      return current.get();
    }

    void advance(Duration duration) {
      current.updateAndGet(i -> i.plus(duration));
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource source;
  @Autowired MutableClock clock;
  @Autowired PlatformTransactionManager transactions;
  String admin, cashier, other, buyer;
  final List<String> ownProducts = new ArrayList<>();
  final Map<String, JsonNode> receipts = new HashMap<>();

  @BeforeEach
  void setup() throws Exception {
    try (var c = source.getConnection()) {
      assertThat(c.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
    clock.current.set(Instant.parse("2026-11-10T02:00:00Z"));
    admin = login("admin");
    cashier = login("cashier01");
    other = login("cashier02");
    buyer = login("buyer01");
  }

  String login(String name) throws Exception {
    return data(postJson(
                "/api/v1/auth/login",
                null,
                json.createObjectNode().put("username", name).put("password", "Test123456"))
            .andExpect(status().isOk()))
        .path("token")
        .asText();
  }

  ResultActions postJson(String path, String token, JsonNode body) throws Exception {
    var request = post(path).contentType("application/json").content(body.toString());
    if (token != null) request.header("Authorization", "Bearer " + token);
    return mvc.perform(request);
  }

  JsonNode data(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
  }

  String fixture(boolean expiry, int... quantities) throws Exception {
    var p =
        json.createObjectNode()
            .put("code", "QA_YJ_" + UUID.randomUUID().toString().substring(0, 12))
            .put("barcode", Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits()))
            .put("name", "销售集成测试商品")
            .put("categoryId", "1")
            .put("unit", "瓶")
            .put("salePrice", "5.00")
            .put("managesExpiry", expiry)
            .put("nearExpiryDays", 0)
            .put("stockLowerLimit", 0)
            .put("canPurchase", true)
            .put("canSell", true);
    if (expiry) p.put("shelfLifeDays", 30);
    String id =
        data(postJson("/api/v1/products", admin, p).andExpect(status().isOk())).path("id").asText();
    ownProducts.add(id);
    var purchase =
        json.createObjectNode()
            .put("supplierId", "1")
            .put("arrivedAt", OffsetDateTime.now(clock).minusSeconds(1).toString())
            .put("remark", "专属销售事务验证来源");
    var items = purchase.putArray("items");
    for (int i = 0; i < quantities.length; i++) {
      var line =
          items
              .addObject()
              .put("productId", id)
              .put("quantity", quantities[i])
              .put("receivedQty", quantities[i])
              .put("unitCost", "3.00");
      if (expiry)
        line.put("productionDate", LocalDate.now(clock).minusDays(1).toString())
            .put("expiryDate", LocalDate.now(clock).plusDays(i * 10L).toString());
    }
    JsonNode saved =
        data(postJson("/api/v1/purchases", admin, purchase).andExpect(status().isOk()));
    String purchaseId = saved.path("order").path("id").asText();
    JsonNode pending =
        data(
            postJson("/api/v1/purchases/" + purchaseId + "/submit", admin, version(saved))
                .andExpect(status().isOk()));
    JsonNode stocked =
        data(
            postJson("/api/v1/purchases/" + purchaseId + "/approve", admin, version(pending))
                .andExpect(status().isOk()));
    receipts.put(id, stocked);
    return id;
  }

  ObjectNode version(JsonNode detail) {
    return json.createObjectNode()
        .put("expectedVersion", detail.path("order").path("rowVersion").asLong());
  }

  ObjectNode sale(String product, int quantity) {
    var request =
        json.createObjectNode()
            .put("requestKey", UUID.randomUUID().toString())
            .put("paymentMethod", "CASH")
            .put("receivedAmount", "100.00");
    request
        .putArray("items")
        .addObject()
        .put("productId", product)
        .put("quantity", quantity)
        .put("expectedPrice", "5.00");
    return request;
  }

  JsonNode complete(ObjectNode request) throws Exception {
    return data(postJson("/api/v1/sales/complete", cashier, request).andExpect(status().isOk()));
  }

  long stock(String product) {
    return jdbc.queryForObject("SELECT stock_qty FROM product WHERE id=?", Long.class, product);
  }

  long saleMovements(String product) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM stock_movement WHERE product_id=? AND event_type='SALE_OUT'",
        Long.class,
        product);
  }

  ObjectNode correction(JsonNode detail) {
    return version(detail).put("goodsReturned", true).put("reason", "当天完整货品仍在店内，整单录入错误");
  }

  JsonNode start(String token, String... products) throws Exception {
    var body = json.createObjectNode().put("remark", "专属盘点流程验证");
    var ids = body.putArray("productIds");
    for (String p : products) ids.add(p);
    return data(postJson("/api/v1/stocktakes", token, body).andExpect(status().isOk()));
  }

  JsonNode command(JsonNode detail, String action, String token) throws Exception {
    var body = version(detail);
    if (Set.of("close", "reject").contains(action)) body.put("reason", "核实本次清点结果，保留完整历史");
    return data(
        postJson(
                "/api/v1/stocktakes/" + detail.path("order").path("id").asText() + "/" + action,
                token,
                body)
            .andExpect(status().isOk()));
  }

  ResultActions saveCounts(JsonNode task, Long... actual) throws Exception {
    var body = version(task);
    var lines = body.putArray("items");
    for (int i = 0; i < actual.length; i++) {
      var item = task.path("items").get(i);
      var line = lines.addObject().put("id", item.path("id").asText());
      if (actual[i] == null) line.putNull("actualQty");
      else line.put("actualQty", actual[i]);
      if (actual[i] != null && actual[i] != item.path("bookQty").asLong())
        line.put("reasonType", "COUNT_ERROR").put("reasonNote", "逐批实际清点复核差异");
    }
    return mvc.perform(
        put("/api/v1/stocktakes/" + task.path("order").path("id").asText() + "/counts")
            .header("Authorization", "Bearer " + buyer)
            .contentType("application/json")
            .content(body.toString()));
  }

  long freeze(String p) {
    return jdbc.queryForObject(
        "SELECT COALESCE(frozen_stocktake_id,0) FROM product WHERE id=?", Long.class, p);
  }

  JsonNode warning(String product, String type) throws Exception {
    String code = jdbc.queryForObject("SELECT code FROM product WHERE id=?", String.class, product);
    return data(
        mvc.perform(
                get("/api/v1/inventory/warnings")
                    .param("keyword", code)
                    .param("type", type)
                    .header("Authorization", "Bearer " + buyer))
            .andExpect(status().isOk()));
  }

  @Test
  void lowStockBoundaryAndFreezeUseRealSaleableQuantity() throws Exception {
    String p = fixture(false, 5);
    jdbc.update("UPDATE product SET stock_lower_limit=5 WHERE id=?", p);
    var result = warning(p, "LOW");
    assertThat(result.path("total").asLong()).isEqualTo(1);
    assertThat(result.path("records").get(0).path("quantity").asLong()).isEqualTo(5);
    start(buyer, p);
    result = warning(p, "LOW");
    assertThat(result.path("records").get(0).path("saleableQty").asLong()).isEqualTo(5);
    assertThat(result.path("records").get(0).path("frozen").asBoolean()).isTrue();
    jdbc.update("UPDATE product SET stock_lower_limit=4 WHERE id=?", p);
    assertThat(warning(p, "LOW").path("total").asLong()).isZero();
    jdbc.update("UPDATE product SET enabled=0,stock_lower_limit=99 WHERE id=?", p);
    assertThat(warning(p, "LOW").path("total").asLong()).isZero();
    jdbc.update("UPDATE product SET enabled=1,can_sell=0 WHERE id=?", p);
    assertThat(warning(p, "LOW").path("total").asLong()).isZero();
    assertThat(stock(p)).isEqualTo(5);
  }

  @Test
  void expiryDayAndNextDayAreClockDrivenAndPreserveBookBalance() throws Exception {
    String p = fixture(true, 5);
    assertThat(warning(p, "NEAR").path("total").asLong()).isEqualTo(1);
    assertThat(warning(p, "NEAR").path("records").get(0).path("remainingDays").asInt()).isZero();
    assertThat(warning(p, "EXPIRED").path("total").asLong()).isZero();
    clock.advance(Duration.ofDays(1));
    buyer = login("buyer01");
    admin = login("admin");
    assertThat(warning(p, "NEAR").path("total").asLong()).isZero();
    var expired = warning(p, "EXPIRED");
    assertThat(expired.path("total").asLong()).isEqualTo(1);
    assertThat(expired.path("records").get(0).path("remainingDays").asInt()).isEqualTo(-1);
    assertThat(expired.path("records").get(0).path("saleableQty").asLong()).isZero();
    assertThat(stock(p)).isEqualTo(5);
    var task = start(buyer, p);
    task = data(saveCounts(task, 0L).andExpect(status().isOk()));
    command(command(task, "submit", buyer), "approve", admin);
    assertThat(warning(p, "EXPIRED").path("total").asLong()).isZero();
    assertThat(stock(p)).isZero();
  }

  @Test
  void nearThresholdChangesImmediatelyAndZeroBatchesAreExcluded() throws Exception {
    String p = fixture(true, 2, 3);
    var today = LocalDate.now(clock);
    jdbc.update("UPDATE purchase_item SET expiry_date=? WHERE product_id=?", today.plusDays(1), p);
    assertThat(warning(p, "NEAR").path("total").asLong()).isZero();
    jdbc.update("UPDATE product SET near_expiry_days=1 WHERE id=?", p);
    assertThat(warning(p, "NEAR").path("total").asLong()).isEqualTo(2);
    complete(sale(p, 5));
    assertThat(warning(p, "NEAR").path("total").asLong()).isZero();
    String normal = fixture(false, 5);
    jdbc.update("UPDATE product SET near_expiry_days=999 WHERE id=?", normal);
    assertThat(warning(normal, "NEAR").path("total").asLong()).isZero();
  }

  @Test
  void inactiveProductsStillExposeExpiredPhysicalStockWithoutCostsOrOwnerData() throws Exception {
    String p = fixture(true, 5);
    clock.advance(Duration.ofDays(1));
    buyer = login("buyer01");
    jdbc.update("UPDATE product SET enabled=0 WHERE id=?", p);
    var result = warning(p, "EXPIRED");
    assertThat(result.path("total").asLong()).isEqualTo(1);
    String response = result.toString();
    assertThat(response).doesNotContain("unitCost", "supplier", "operator", "frozenStocktakeId");
    assertThat(result.path("records").get(0).path("productId").isTextual()).isTrue();
    assertThat(result.path("businessDate").asText()).isEqualTo(LocalDate.now(clock).toString());
  }

  @Test
  void roleScopeAndInvalidTypeAreCheckedByBackend() throws Exception {
    mvc.perform(get("/api/v1/inventory/warnings").header("Authorization", "Bearer " + cashier))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/inventory/warnings")
                .header("Authorization", "Bearer " + buyer)
                .param("type", "NOT_A_TYPE"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/inventory/warnings")
                .header("Authorization", "Bearer " + buyer)
                .param("pageSize", "999"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/inventory/warnings")
                .header("Authorization", "Bearer " + buyer)
                .param("categoryId", "-1"))
        .andExpect(status().isBadRequest());
    var result =
        data(
            mvc.perform(
                    get("/api/v1/inventory/warnings")
                        .header("Authorization", "Bearer " + admin)
                        .param("keyword", "QA-NO-SUCH-WARNING"))
                .andExpect(status().isOk()));
    assertThat(result.path("records").size()).isZero();
    assertThat(result.path("total").asLong()).isZero();
  }

  @AfterEach
  void cleanup() {
    if (ownProducts.isEmpty()) return;
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            state -> {
              String placeholders = String.join(",", Collections.nCopies(ownProducts.size(), "?"));
              Object[] ids = ownProducts.toArray();
              var saleIds =
                  jdbc.queryForList(
                      "SELECT DISTINCT sale_id FROM sale_item WHERE product_id IN ("
                          + placeholders
                          + ")",
                      Long.class,
                      ids);
              var purchaseIds =
                  jdbc.queryForList(
                      "SELECT DISTINCT purchase_id FROM purchase_item WHERE product_id IN ("
                          + placeholders
                          + ")",
                      Long.class,
                      ids);
              jdbc.update(
                  "DELETE FROM stock_movement WHERE product_id IN (" + placeholders + ")", ids);
              for (Long id : saleIds) {
                jdbc.update(
                    "DELETE a FROM sale_allocation a JOIN sale_item i ON i.id=a.sale_item_id WHERE i.sale_id=?",
                    id);
                jdbc.update(
                    "DELETE FROM operation_log WHERE target_type='SALE' AND target_id=?",
                    id.toString());
                jdbc.update("DELETE FROM sale_item WHERE sale_id=?", id);
                jdbc.update("DELETE FROM sale_order WHERE id=?", id);
              }
              var stocktakeIds =
                  jdbc.queryForList(
                      "SELECT DISTINCT stocktake_id FROM stocktake_item WHERE product_id IN ("
                          + placeholders
                          + ")",
                      Long.class,
                      ids);
              jdbc.update(
                  "UPDATE product SET frozen_stocktake_id=NULL WHERE id IN (" + placeholders + ")",
                  ids);
              for (Long id : stocktakeIds) {
                jdbc.update(
                    "DELETE FROM operation_log WHERE target_type='STOCKTAKE' AND target_id=?",
                    id.toString());
                jdbc.update("DELETE FROM stocktake_item WHERE stocktake_id=?", id);
                jdbc.update("DELETE FROM stocktake_order WHERE id=?", id);
              }
              for (Long id : purchaseIds) {
                jdbc.update(
                    "DELETE FROM operation_log WHERE target_type='PURCHASE' AND target_id=?",
                    id.toString());
                jdbc.update("DELETE FROM purchase_item WHERE purchase_id=?", id);
                jdbc.update("DELETE FROM purchase_order WHERE id=?", id);
              }
              for (String id : ownProducts) {
                jdbc.update(
                    "DELETE FROM operation_log WHERE target_type='PRODUCT' AND target_id=?", id);
                jdbc.update("DELETE FROM product WHERE id=?", id);
              }
            });
  }
}
