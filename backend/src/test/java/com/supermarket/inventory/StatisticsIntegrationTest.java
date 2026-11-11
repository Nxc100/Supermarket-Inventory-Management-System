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
@Import(StatisticsIntegrationTest.TimeConfiguration.class)
class StatisticsIntegrationTest {
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
  String fixtureCategory = "1";

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
            .put("code", "QA_TJ_" + UUID.randomUUID().toString().substring(0, 12))
            .put("barcode", Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits()))
            .put("name", "销售集成测试商品")
            .put("categoryId", fixtureCategory)
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

  JsonNode report(String product, String token) throws Exception {
    return data(
        mvc.perform(
                get("/api/v1/statistics/sales")
                    .param("productId", product)
                    .param("startDate", "2026-10-01")
                    .param("endDate", "2026-11-10")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()));
  }

  void time(String instant) throws Exception {
    clock.current.set(Instant.parse(instant));
    admin = login("admin");
    buyer = login("buyer01");
    cashier = login("cashier01");
    other = login("cashier02");
  }

  JsonNode slow(String p) throws Exception {
    return data(
        mvc.perform(
                get("/api/v1/statistics/sales/slow-products")
                    .param("productId", p)
                    .header("Authorization", "Bearer " + cashier))
            .andExpect(status().isOk()));
  }

  @Test
  void crossBatchSalesCountOnceAndCorrectionIsANegativeEvent() throws Exception {
    String p = fixture(true, 10, 10);
    var sold = complete(sale(p, 12));
    var input = sale(p, 2);
    data(postJson("/api/v1/sales/complete", other, input).andExpect(status().isOk()));
    var r = report(p, cashier);
    assertThat(r.path("totals").path("netQty").asLong()).isEqualTo(14);
    assertThat(r.path("totals").path("netAmount").asText()).isEqualTo("70.00");
    assertThat(r.path("totals").path("completedCount").asLong()).isEqualTo(2);
    assertThat(r.path("monthly").size()).isEqualTo(12);
    assertThat(r.path("monthly").get(0).path("netAmount").asText()).isEqualTo("0.00");
    assertThat(r.path("monthly").get(10).path("netQty").asLong()).isEqualTo(14);
    assertThat(r.path("records").size()).isEqualTo(1);
    assertThat(r.toString())
        .doesNotContain("cashierId", "orderNo", "unitCost", "supplier", "saleId");
    postJson(
            "/api/v1/sales/" + sold.path("order").path("id").asText() + "/correct",
            admin,
            correction(sold))
        .andExpect(status().isOk());
    r = report(p, cashier);
    assertThat(r.path("totals").path("netQty").asLong()).isEqualTo(2);
    assertThat(r.path("totals").path("netAmount").asText()).isEqualTo("10.00");
    assertThat(r.path("totals").path("correctedCount").asLong()).isEqualTo(1);
    assertThat(r.path("monthly").get(10).path("netAmount").asText()).isEqualTo("10.00");
    assertThat(stock(p)).isEqualTo(18);
  }

  @Test
  void twoNaturalMonthsAndPriceRenameSnapshotsRemainAggregatedByProduct() throws Exception {
    time("2026-10-01T02:00:00Z");
    String p = fixture(false, 10);
    complete(sale(p, 2));
    time("2026-11-10T02:00:00Z");
    jdbc.update("UPDATE product SET sale_price=6.25,name='统计改名快照' WHERE id=?", p);
    var request = sale(p, 1);
    ((ObjectNode) request.path("items").get(0)).put("expectedPrice", "6.25");
    complete(request);
    var r = report(p, cashier);
    assertThat(r.path("records").size()).isEqualTo(1);
    assertThat(r.path("records").get(0).path("netQty").asLong()).isEqualTo(3);
    assertThat(r.path("totals").path("netAmount").asText()).isEqualTo("16.25");
    assertThat(r.path("monthly").get(9).path("netAmount").asText()).isEqualTo("10.00");
    assertThat(r.path("monthly").get(10).path("netAmount").asText()).isEqualTo("6.25");
    assertThat(r.path("daily").size()).isEqualTo(2);
    jdbc.update("UPDATE product SET sale_price=9.00 WHERE id=?", p);
    assertThat(report(p, admin).path("totals").path("netAmount").asText()).isEqualTo("16.25");
  }

  @Test
  void slowWindowSeparatesNewReceiptsAndExcludesSoldOutOrSoldProducts() throws Exception {
    time("2026-09-01T02:00:00Z");
    String old = fixture(false, 5), sold = fixture(false, 5);
    time("2026-11-10T02:00:00Z");
    String fresh = fixture(false, 5);
    complete(sale(sold, 1));
    assertThat(slow(old).path("records").get(0).path("observation").asText()).isEqualTo("STAGNANT");
    assertThat(slow(fresh).path("records").get(0).path("observation").asText())
        .isEqualTo("INSUFFICIENT_HISTORY");
    assertThat(slow(sold).path("total").asLong()).isZero();
    complete(sale(old, 5));
    assertThat(slow(old).path("total").asLong()).isZero();
    assertThat(slow(fresh).path("windowStart").asText()).isEqualTo("2026-10-12");
  }

  @Test
  void reversedOldReceiptCannotMakeNewStockAppearOldEnough() throws Exception {
    time("2026-09-01T02:00:00Z");
    String p = fixture(false, 5);
    var old = receipts.get(p);
    time("2026-11-10T02:00:00Z");
    var body =
        json.createObjectNode()
            .put("supplierId", "1")
            .put("arrivedAt", OffsetDateTime.now(clock).minusSeconds(1).toString());
    body.putArray("items")
        .addObject()
        .put("productId", p)
        .put("quantity", 5)
        .put("receivedQty", 5)
        .put("unitCost", "3.00");
    var draft = data(postJson("/api/v1/purchases", admin, body).andExpect(status().isOk()));
    var id = draft.path("order").path("id").asText();
    var pending =
        data(
            postJson("/api/v1/purchases/" + id + "/submit", admin, version(draft))
                .andExpect(status().isOk()));
    postJson("/api/v1/purchases/" + id + "/approve", admin, version(pending))
        .andExpect(status().isOk());
    assertThat(slow(p).path("records").get(0).path("observation").asText()).isEqualTo("STAGNANT");
    postJson(
            "/api/v1/purchases/" + old.path("order").path("id").asText() + "/reverse",
            admin,
            version(old).put("reason", "撤销未使用旧来源，保留本月新来源"))
        .andExpect(status().isOk());
    assertThat(slow(p).path("records").get(0).path("observation").asText())
        .isEqualTo("INSUFFICIENT_HISTORY");
    assertThat(stock(p)).isEqualTo(5);
  }

  @Test
  void inventoryUsesBookDenominatorAndRetainsDisabledExpiredPhysicalStock() throws Exception {
    String p = fixture(false, 5);
    fixtureCategory = "2";
    String expired = fixture(true, 6);
    clock.advance(Duration.ofDays(1));
    buyer = login("buyer01");
    jdbc.update("UPDATE product SET stock_lower_limit=5 WHERE id=?", p);
    jdbc.update("UPDATE product SET enabled=0 WHERE id=?", expired);
    var r =
        data(
            mvc.perform(
                    get("/api/v1/statistics/inventory")
                        .param("keyword", "QA_TJ_")
                        .header("Authorization", "Bearer " + buyer))
                .andExpect(status().isOk()));
    assertThat(r.path("total").asLong()).isEqualTo(2);
    assertThat(r.path("totals").path("bookQty").asLong()).isEqualTo(11);
    assertThat(r.path("totals").path("saleableQty").asLong()).isEqualTo(5);
    assertThat(r.path("totals").path("expiredQty").asLong()).isEqualTo(6);
    assertThat(r.path("totals").path("lowProductCount").asLong()).isEqualTo(1);
    assertThat(r.path("totals").path("expiredBatchCount").asLong()).isEqualTo(1);
    assertThat(r.path("categories").get(0).path("percentage").asText()).isEqualTo("54.55");
    assertThat(r.path("categories").get(1).path("percentage").asText()).isEqualTo("45.45");
    assertThat(r.path("filters").path("keyword").asText()).isEqualTo("QA_TJ_");
    var warning =
        data(
            mvc.perform(
                    get("/api/v1/inventory/warnings")
                        .param("productId", p)
                        .param("enabled", "true")
                        .header("Authorization", "Bearer " + buyer))
                .andExpect(status().isOk()));
    assertThat(warning.path("total").asLong()).isEqualTo(1);
    assertThat(warning.path("records").get(0).path("productId").asText()).isEqualTo(p);
    warning =
        data(
            mvc.perform(
                    get("/api/v1/inventory/warnings")
                        .param("productId", p)
                        .param("enabled", "false")
                        .header("Authorization", "Bearer " + buyer))
                .andExpect(status().isOk()));
    assertThat(warning.path("total").asLong()).isZero();
    var empty =
        data(
            mvc.perform(
                    get("/api/v1/statistics/inventory")
                        .param("keyword", "QA-NO-STATISTICS")
                        .header("Authorization", "Bearer " + buyer))
                .andExpect(status().isOk()));
    assertThat(empty.path("totals").path("bookQty").asLong()).isZero();
    assertThat(empty.path("categories").size()).isZero();
    assertThat(stock(p)).isEqualTo(5);
  }

  @Test
  void rolesInvalidFiltersAndEmptyMonthsRemainExplicit() throws Exception {
    mvc.perform(get("/api/v1/statistics/sales").header("Authorization", "Bearer " + buyer))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/statistics/sales/slow-products")
                .header("Authorization", "Bearer " + buyer))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/statistics/inventory").header("Authorization", "Bearer " + cashier))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/statistics/sales")
                .param("year", "9999")
                .header("Authorization", "Bearer " + cashier))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/statistics/sales")
                .param("startDate", "2026-11-10")
                .param("endDate", "2026-11-01")
                .header("Authorization", "Bearer " + cashier))
        .andExpect(status().isBadRequest());
    var r =
        data(
            mvc.perform(
                    get("/api/v1/statistics/sales")
                        .param("keyword", "QA-NO-SALES")
                        .header("Authorization", "Bearer " + cashier))
                .andExpect(status().isOk()));
    assertThat(r.path("total").asLong()).isZero();
    assertThat(r.path("monthly").size()).isEqualTo(12);
    assertThat(r.path("totals").path("netAmount").asText()).isEqualTo("0.00");
    assertThat(r.path("hotProducts").size()).isZero();
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
