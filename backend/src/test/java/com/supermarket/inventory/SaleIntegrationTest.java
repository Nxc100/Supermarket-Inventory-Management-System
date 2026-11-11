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
@Import(SaleIntegrationTest.TimeConfiguration.class)
class SaleIntegrationTest {
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
            .put("code", "QA_XS_" + UUID.randomUUID().toString().substring(0, 12))
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

  @Test
  void cashPaymentSpansFefoBatchesAndOwnResultHasNoCosts() throws Exception {
    String product = fixture(true, 10, 10);
    ObjectNode input = sale(product, 12);
    JsonNode quote =
        data(
            postJson(
                    "/api/v1/sales/quote",
                    cashier,
                    json.createObjectNode().set("items", input.path("items")))
                .andExpect(status().isOk()));
    assertThat(quote.path("canConfirm").asBoolean()).isTrue();
    assertThat(stock(product)).isEqualTo(20);
    JsonNode sold = complete(input);
    assertThat(sold.path("order").path("totalAmount").asText()).isEqualTo("60.00");
    assertThat(sold.path("order").path("changeAmount").asText()).isEqualTo("40.00");
    assertThat(stock(product)).isEqualTo(8);
    assertThat(saleMovements(product)).isEqualTo(2);
    var batches =
        jdbc.queryForList(
            "SELECT stock_qty FROM purchase_item WHERE product_id=? ORDER BY expiry_date,id",
            Long.class,
            product);
    assertThat(batches).containsExactly(0L, 8L);
    String response = sold.toString();
    assertThat(response).doesNotContain("unitCost", "supplier", "batchItemId", "requestHash");
    assertThat(sold.path("order").path("id").isTextual()).isTrue();
    mvc.perform(
            get("/api/v1/sales/by-request/" + input.path("requestKey").asText())
                .header("Authorization", "Bearer " + cashier))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.order.id").value(sold.path("order").path("id").asText()));
    assertThat(stock(product)).isEqualTo(8);
  }

  @Test
  void exactMockPaymentAndInsufficientCashAreChecked() throws Exception {
    String p = fixture(false, 5);
    var input = sale(p, 2).put("receivedAmount", "9.99");
    postJson("/api/v1/sales/complete", cashier, input).andExpect(status().isBadRequest());
    assertThat(stock(p)).isEqualTo(5);
    input.put("paymentMethod", "MOCK_EPAY").put("receivedAmount", "100.00");
    postJson("/api/v1/sales/complete", cashier, input).andExpect(status().isBadRequest());
    JsonNode sold = complete(input.put("receivedAmount", "10.00"));
    assertThat(sold.path("order").path("changeAmount").asText()).isEqualTo("0.00");
  }

  @Test
  void duplicateKeyReturnsSameOrderWhileDifferentContentOrCashierIsRejected() throws Exception {
    String p = fixture(false, 5);
    var input = sale(p, 4);
    var first = complete(input);
    var repeat = complete(input);
    assertThat(repeat.path("order").path("id")).isEqualTo(first.path("order").path("id"));
    assertThat(stock(p)).isEqualTo(1);
    assertThat(saleMovements(p)).isEqualTo(1);
    postJson("/api/v1/sales/complete", other, input).andExpect(status().isConflict());
    var changed = input.deepCopy();
    ((ObjectNode) changed.path("items").get(0)).put("quantity", 1);
    postJson("/api/v1/sales/complete", cashier, changed).andExpect(status().isConflict());
    assertThat(stock(p)).isEqualTo(1);
  }

  @Test
  void roleAndOwnershipAreEnforcedOnEveryReadAndCorrection() throws Exception {
    String p = fixture(false, 5);
    var input = sale(p, 1);
    postJson("/api/v1/sales/complete", buyer, input).andExpect(status().isForbidden());
    var sold = complete(input);
    String id = sold.path("order").path("id").asText();
    mvc.perform(get("/api/v1/sales/" + id).header("Authorization", "Bearer " + other))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/v1/sales/by-request/" + input.path("requestKey").asText())
                .header("Authorization", "Bearer " + other))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/v1/sales")
                .param("keyword", sold.path("order").path("orderNo").asText())
                .header("Authorization", "Bearer " + other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(0));
    postJson("/api/v1/sales/" + id + "/correct", cashier, correction(sold))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/sales")
                .param("cashierId", "6")
                .header("Authorization", "Bearer " + cashier))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/sales/cashiers").header("Authorization", "Bearer " + cashier))
        .andExpect(status().isForbidden());
  }

  @Test
  void priceChangeRequiresExplicitNewPriceAndSnapshotsRemainHistorical() throws Exception {
    String p = fixture(false, 5);
    var input = sale(p, 1);
    jdbc.update("UPDATE product SET sale_price=6.25 WHERE id=?", p);
    JsonNode quote =
        data(
            postJson(
                    "/api/v1/sales/quote",
                    cashier,
                    json.createObjectNode().set("items", input.path("items")))
                .andExpect(status().isOk()));
    assertThat(quote.path("canConfirm").asBoolean()).isFalse();
    assertThat(quote.path("items").get(0).path("expectedPrice").asText()).isEqualTo("5.00");
    assertThat(quote.path("items").get(0).path("currentPrice").asText()).isEqualTo("6.25");
    postJson("/api/v1/sales/complete", cashier, input)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PRICE_CHANGED"));
    assertThat(stock(p)).isEqualTo(5);
    ((ObjectNode) input.path("items").get(0)).put("expectedPrice", "6.25");
    var sold = complete(input);
    jdbc.update("UPDATE product SET sale_price=8.00,name='后改商品名称' WHERE id=?", p);
    mvc.perform(
            get("/api/v1/sales/" + sold.path("order").path("id").asText())
                .header("Authorization", "Bearer " + cashier))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].unitPrice").value("6.25"))
        .andExpect(jsonPath("$.data.items[0].productName").value("销售集成测试商品"));
  }

  @Test
  void stoppedOrFrozenProductAndExpiredNextDayCannotSell() throws Exception {
    String p = fixture(true, 5);
    var input = sale(p, 1);
    jdbc.update("UPDATE product SET enabled=0 WHERE id=?", p);
    postJson("/api/v1/sales/complete", cashier, input).andExpect(status().isConflict());
    jdbc.update("UPDATE product SET enabled=1,frozen_stocktake_id=2 WHERE id=?", p);
    postJson("/api/v1/sales/complete", cashier, input).andExpect(status().isConflict());
    jdbc.update("UPDATE product SET frozen_stocktake_id=NULL WHERE id=?", p);
    clock.advance(Duration.ofDays(1));
    cashier = login("cashier01");
    postJson("/api/v1/sales/complete", cashier, input).andExpect(status().isConflict());
    String code = jdbc.queryForObject("SELECT code FROM product WHERE id=?", String.class, p);
    mvc.perform(
            get("/api/v1/product-lookups/sale")
                .param("keyword", code)
                .header("Authorization", "Bearer " + cashier))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[0].saleableQty").value(0));
    mvc.perform(
            get("/api/v1/inventory/saleable-products")
                .param("keyword", code)
                .header("Authorization", "Bearer " + cashier))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[0].saleableQty").value(0));
    assertThat(stock(p)).isEqualTo(5);
  }

  @Test
  void anyMissingStockRollsBackWholeSale() throws Exception {
    String first = fixture(false, 5), second = fixture(false, 1);
    var input = sale(first, 1);
    ((ArrayNode) input.path("items"))
        .addObject()
        .put("productId", second)
        .put("quantity", 2)
        .put("expectedPrice", "5.00");
    postJson("/api/v1/sales/complete", cashier, input).andExpect(status().isConflict());
    assertThat(stock(first)).isEqualTo(5);
    assertThat(saleMovements(first)).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM sale_order WHERE request_key=?",
                Long.class,
                input.path("requestKey").asText()))
        .isZero();
  }

  @Test
  void injectedBalanceGuardFailureRollsBackEarlierBatchHeaderAndLedger() throws Exception {
    String first = fixture(false, 5), second = fixture(false, 5);
    var input = sale(first, 1);
    ((ArrayNode) input.path("items"))
        .addObject()
        .put("productId", second)
        .put("quantity", 1)
        .put("expectedPrice", "5.00");
    jdbc.update("UPDATE product SET stock_qty=0 WHERE id=?", second);
    postJson("/api/v1/sales/complete", cashier, input).andExpect(status().isConflict());
    assertThat(stock(first)).isEqualTo(5);
    assertThat(saleMovements(first)).isZero();
    assertThat(saleMovements(second)).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT SUM(stock_qty) FROM purchase_item WHERE product_id=?", Long.class, first))
        .isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM sale_order WHERE request_key=?",
                Long.class,
                input.path("requestKey").asText()))
        .isZero();
  }

  @Test
  void sameDayCorrectionRestoresOriginalBatchesAndCannotRepeatOrReversePurchase() throws Exception {
    String p = fixture(true, 10, 10);
    var sold = complete(sale(p, 12));
    String id = sold.path("order").path("id").asText();
    postJson(
            "/api/v1/sales/" + id + "/correct", admin, correction(sold).put("goodsReturned", false))
        .andExpect(status().isBadRequest());
    var corrected =
        data(
            postJson("/api/v1/sales/" + id + "/correct", admin, correction(sold))
                .andExpect(status().isOk()));
    assertThat(corrected.path("order").path("status").asText()).isEqualTo("CORRECTED");
    assertThat(stock(p)).isEqualTo(20);
    mvc.perform(
            get("/api/v1/sales")
                .param("keyword", sold.path("order").path("orderNo").asText())
                .header("Authorization", "Bearer " + cashier))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totals.originalAmount").value("60.00"))
        .andExpect(jsonPath("$.data.totals.netAmount").value("0.00"))
        .andExpect(jsonPath("$.data.totals.correctedCount").value(1));
    assertThat(
            jdbc.queryForList(
                "SELECT stock_qty FROM purchase_item WHERE product_id=? ORDER BY id",
                Long.class,
                p))
        .containsExactly(10L, 10L);
    postJson("/api/v1/sales/" + id + "/correct", admin, correction(corrected))
        .andExpect(status().isConflict());
    var purchase = receipts.get(p);
    postJson(
            "/api/v1/purchases/" + purchase.path("order").path("id").asText() + "/reverse",
            admin,
            version(purchase).put("reason", "虽数量恢复相等但已有销售历史"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("后续流水")));
    assertThat(
            jdbc.queryForObject(
                "SELECT SUM(signed_amount) FROM v_sale_event WHERE sale_id=?",
                java.math.BigDecimal.class,
                id))
        .isEqualByComparingTo("0.00");
  }

  @Test
  void correctionRejectsFreezeAndCrossDayButCanRestoreAnExpiredSource() throws Exception {
    String p = fixture(true, 5);
    var sold = complete(sale(p, 1));
    String id = sold.path("order").path("id").asText();
    jdbc.update("UPDATE product SET frozen_stocktake_id=2 WHERE id=?", p);
    postJson("/api/v1/sales/" + id + "/correct", admin, correction(sold))
        .andExpect(status().isConflict());
    assertThat(stock(p)).isEqualTo(4);
    jdbc.update("UPDATE product SET frozen_stocktake_id=NULL WHERE id=?", p);
    jdbc.update(
        "UPDATE purchase_item SET expiry_date=? WHERE product_id=?",
        LocalDate.now(clock).minusDays(1),
        p);
    postJson("/api/v1/sales/" + id + "/correct", admin, correction(sold))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.expiredSourceStock").value(true));
    assertThat(stock(p)).isEqualTo(5);
    String code = jdbc.queryForObject("SELECT code FROM product WHERE id=?", String.class, p);
    mvc.perform(
            get("/api/v1/inventory/saleable-products")
                .param("keyword", code)
                .header("Authorization", "Bearer " + cashier))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[0].saleableQty").value(0));
    String otherProduct = fixture(false, 5);
    var later = complete(sale(otherProduct, 1));
    clock.advance(Duration.ofDays(1));
    admin = login("admin");
    postJson(
            "/api/v1/sales/" + later.path("order").path("id").asText() + "/correct",
            admin,
            correction(later))
        .andExpect(status().isConflict());
    assertThat(stock(otherProduct)).isEqualTo(4);
  }

  @Test
  void forgedAndFractionalCommandsAreRejectedBeforeAnyStockWrite() throws Exception {
    String p = fixture(false, 5);
    var input = sale(p, 1).put("cashierId", "1");
    postJson("/api/v1/sales/complete", cashier, input).andExpect(status().isBadRequest());
    input.remove("cashierId");
    ((ObjectNode) input.path("items").get(0)).put("quantity", 1.5);
    postJson("/api/v1/sales/complete", cashier, input).andExpect(status().isBadRequest());
    assertThat(stock(p)).isEqualTo(5);
  }

  @Test
  void twoCashiersCompeteForFiveOnlyOneCanBuyFour() throws Exception {
    String p = fixture(false, 5);
    var results = race(sale(p, 4), cashier, sale(p, 4), other);
    assertThat(results)
        .extracting(r -> r.getResponse().getStatus())
        .containsExactlyInAnyOrder(200, 409);
    assertThat(stock(p)).isEqualTo(1);
    assertThat(saleMovements(p)).isEqualTo(1);
  }

  @Test
  void concurrentSameKeyReturnsOneResultAndOneStockChange() throws Exception {
    String p = fixture(false, 5);
    var request = sale(p, 4);
    var results = race(request, cashier, request, cashier);
    assertThat(results).extracting(r -> r.getResponse().getStatus()).containsOnly(200);
    String
        a =
            json.readTree(results.get(0).getResponse().getContentAsString())
                .path("data")
                .path("order")
                .path("id")
                .asText(),
        b =
            json.readTree(results.get(1).getResponse().getContentAsString())
                .path("data")
                .path("order")
                .path("id")
                .asText();
    assertThat(a).isEqualTo(b);
    assertThat(stock(p)).isEqualTo(1);
    assertThat(saleMovements(p)).isEqualTo(1);
  }

  List<MvcResult> race(ObjectNode a, String aToken, ObjectNode b, String bToken) throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try {
      var first =
          executor.submit(
              () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS))
                  throw new IllegalStateException("start timeout");
                return postJson("/api/v1/sales/complete", aToken, a).andReturn();
              });
      var second =
          executor.submit(
              () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS))
                  throw new IllegalStateException("start timeout");
                return postJson("/api/v1/sales/complete", bToken, b).andReturn();
              });
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
    } finally {
      start.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
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
