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
@Import({
  SuggestionIntegrationTest.TimeConfiguration.class,
  SuggestionIntegrationTest.ClientConfiguration.class
})
class SuggestionIntegrationTest {
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
            .put("code", "QA_AI_" + UUID.randomUUID().toString().substring(0, 12))
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

  @TestConfiguration
  static class ClientConfiguration {
    @Bean
    @Primary
    FakeClient suggestionFakeClient() {
      return new FakeClient();
    }
  }

  static class FakeClient implements com.supermarket.inventory.ai.client.SuggestionClient {
    final java.util.concurrent.atomic.AtomicInteger calls =
        new java.util.concurrent.atomic.AtomicInteger();
    volatile String mode = "SUCCESS", lastSummary = "";
    volatile CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);

    public Identity identity() {
      return new Identity("FIXTURE", "integration-only-not-real-api", true);
    }

    public Result generate(com.supermarket.inventory.ai.enums.SuggestionType type, String summary) {
      assertThat(
              org.springframework.transaction.support.TransactionSynchronizationManager
                  .isActualTransactionActive())
          .isFalse();
      calls.incrementAndGet();
      lastSummary = summary;
      entered.countDown();
      if (mode.equals("GATE"))
        try {
          if (!release.await(8, TimeUnit.SECONDS))
            throw new IllegalStateException("test gate timeout");
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException(e);
        }
      if (mode.equals("FAILURE"))
        throw new com.supermarket.inventory.ai.client.SuggestionFailure(
            "TIMEOUT", "测试夹具模拟模型超时，基础业务可继续");
      return new Result(
          mode.equals("EMPTY") ? " " : "【集成测试夹具，非真实模型调用】请根据快照人工核实补货、临期和经营情况。", identity());
    }
  }

  @Autowired FakeClient model;
  final List<String> ownKeys = new ArrayList<>();

  @BeforeEach
  void resetModel() {
    model.calls.set(0);
    model.mode = "SUCCESS";
    model.lastSummary = "";
    model.entered = new CountDownLatch(1);
    model.release = new CountDownLatch(1);
  }

  ObjectNode previewBody(String type, String product) {
    String key = UUID.randomUUID().toString();
    ownKeys.add(key);
    var body =
        json.createObjectNode()
            .put("requestKey", key)
            .put("type", type)
            .put("startDate", "2026-10-12")
            .put("endDate", "2026-11-10");
    if (product != null) body.put("productId", product);
    return body;
  }

  JsonNode preview(ObjectNode body, String token) throws Exception {
    return data(postJson("/api/v1/suggestions/preview", token, body).andExpect(status().isOk()));
  }

  JsonNode generate(String key, String token) throws Exception {
    return data(
        postJson(
                "/api/v1/suggestions/by-request/" + key + "/generate",
                token,
                json.createObjectNode())
            .andExpect(status().isOk()));
  }

  @Test
  void previewIsWhitelistedAndConfirmedSnapshotDoesNotChangeWithLaterBusiness() throws Exception {
    String p = fixture(false, 5);
    complete(sale(p, 2));
    var input = previewBody("INVENTORY", p);
    var snapshot = preview(input, buyer);
    var categoryOptions =
        data(
            mvc.perform(
                    get("/api/v1/suggestions/category-options")
                        .header("Authorization", "Bearer " + buyer))
                .andExpect(status().isOk()));
    assertThat(categoryOptions.get(0).path("id").isTextual()).isTrue();
    assertThat(categoryOptions.get(0).path("code").asText()).isNotBlank();
    var options =
        data(
            mvc.perform(
                    get("/api/v1/suggestions/product-options")
                        .param("type", "INVENTORY")
                        .param("productId", p)
                        .header("Authorization", "Bearer " + buyer))
                .andExpect(status().isOk()));
    assertThat(options.path("records").get(0).path("id").asText()).isEqualTo(p);
    assertThat(options.toString()).doesNotContain("unitCost", "referenceCost", "supplier");
    postJson(
            "/api/v1/suggestions/by-request/" + input.path("requestKey").asText() + "/generate",
            buyer,
            json.createObjectNode().put("summary", "forged replacement"))
        .andExpect(status().isBadRequest());
    assertThat(model.calls.get()).isZero();
    assertThat(snapshot.path("summary").path("products").get(0).path("bookQty").asLong())
        .isEqualTo(3);
    assertThat(snapshot.path("summary").path("products").get(0).path("recentNetQty").asLong())
        .isEqualTo(2);
    assertThat(snapshot.toString())
        .doesNotContain("unitCost", "phone", "supplier", "password", "cashierId");
    complete(sale(p, 1));
    var result = generate(input.path("requestKey").asText(), buyer);
    assertThat(result.path("record").path("status").asText()).isEqualTo("SUCCESS");
    assertThat(result.path("record").path("fixture").asBoolean()).isTrue();
    assertThat(result.path("record").path("provider").asText()).isEqualTo("FIXTURE");
    assertThat(json.readTree(model.lastSummary).path("products").get(0).path("bookQty").asLong())
        .isEqualTo(3);
    assertThat(result.path("summary")).isEqualTo(snapshot.path("summary"));
    assertThat(stock(p)).isEqualTo(2);
  }

  @Test
  void salesAndInventoryTypePermissionsApplyToPreviewHistoryAndDetails() throws Exception {
    String p = fixture(false, 5);
    complete(sale(p, 1));
    var body = previewBody("SALES", p);
    postJson("/api/v1/suggestions/preview", buyer, body).andExpect(status().isForbidden());
    postJson("/api/v1/suggestions/preview", cashier, body).andExpect(status().isForbidden());
    preview(body, admin);
    var result = generate(body.path("requestKey").asText(), admin);
    String id = result.path("record").path("id").asText();
    mvc.perform(get("/api/v1/suggestions/" + id).header("Authorization", "Bearer " + buyer))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/suggestions")
                .param("type", "SALES")
                .header("Authorization", "Bearer " + buyer))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/suggestions").header("Authorization", "Bearer " + cashier))
        .andExpect(status().isForbidden());
    assertThat(result.path("summary").path("salesTotals").path("netAmount").asText())
        .isEqualTo("5.00");
  }

  @Test
  void inventorySuccessIsSharedButFailureAndRequestRecoveryStayOwned() throws Exception {
    String p = fixture(false, 5);
    var a = previewBody("INVENTORY", p);
    preview(a, buyer);
    var success = generate(a.path("requestKey").asText(), buyer);
    String otherBuyer = login("buyer02");
    mvc.perform(
            get("/api/v1/suggestions/" + success.path("record").path("id").asText())
                .header("Authorization", "Bearer " + otherBuyer))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/suggestions/by-request/" + a.path("requestKey").asText())
                .header("Authorization", "Bearer " + otherBuyer))
        .andExpect(status().isNotFound());
    model.mode = "FAILURE";
    var b = previewBody("INVENTORY", p);
    preview(b, buyer);
    var failure = generate(b.path("requestKey").asText(), buyer);
    String failedId = failure.path("record").path("id").asText();
    mvc.perform(
            get("/api/v1/suggestions/" + failedId).header("Authorization", "Bearer " + otherBuyer))
        .andExpect(status().isNotFound());
    var history =
        data(
            mvc.perform(
                    get("/api/v1/suggestions")
                        .param("type", "INVENTORY")
                        .header("Authorization", "Bearer " + otherBuyer))
                .andExpect(status().isOk()));
    for (var record : history.path("records"))
      assertThat(record.path("id").asText()).isNotEqualTo(failedId);
    assertThat(failure.path("content").isNull()).isTrue();
    assertThat(stock(p)).isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "SELECT result FROM operation_log WHERE target_type='AI' AND target_id=?",
                String.class,
                failedId))
        .isEqualTo("FAILURE");
  }

  @Test
  void emptyUnknownAndForgedPreviewNeverCallTheModel() throws Exception {
    String p = fixture(false, 1);
    var receipt = receipts.get(p);
    postJson(
            "/api/v1/purchases/" + receipt.path("order").path("id").asText() + "/reverse",
            admin,
            version(receipt).put("reason", "撤销未使用来源供空摘要验证"))
        .andExpect(status().isOk());
    postJson("/api/v1/suggestions/preview", buyer, previewBody("INVENTORY", p))
        .andExpect(status().isBadRequest());
    var body = previewBody("INVENTORY", p).put("operatorId", "1");
    postJson("/api/v1/suggestions/preview", buyer, body).andExpect(status().isBadRequest());
    postJson(
            "/api/v1/suggestions/by-request/" + UUID.randomUUID() + "/generate",
            buyer,
            json.createObjectNode())
        .andExpect(status().isConflict());
    assertThat(model.calls.get()).isZero();
  }

  @Test
  void sameKeyConcurrentGenerateCallsModelOnceAndBasicSaleIsIndependent() throws Exception {
    String p = fixture(false, 5);
    var body = previewBody("INVENTORY", p);
    preview(body, buyer);
    String key = body.path("requestKey").asText();
    model.mode = "GATE";
    var pool = Executors.newSingleThreadExecutor();
    try {
      var first = pool.submit(() -> generate(key, buyer));
      assertThat(model.entered.await(5, TimeUnit.SECONDS)).isTrue();
      var pending = generate(key, buyer);
      assertThat(pending.path("record").path("status").asText()).isEqualTo("PENDING");
      assertThat(model.calls.get()).isEqualTo(1);
      complete(sale(p, 1));
      assertThat(stock(p)).isEqualTo(4);
      model.release.countDown();
      var done = first.get(10, TimeUnit.SECONDS);
      var repeat = generate(key, buyer);
      assertThat(repeat.path("record").path("id")).isEqualTo(done.path("record").path("id"));
      assertThat(model.calls.get()).isEqualTo(1);
    } finally {
      model.release.countDown();
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void expiredPreviewAndOtherOwnersCannotGenerateIt() throws Exception {
    String p = fixture(false, 5);
    var body = previewBody("INVENTORY", p);
    preview(body, buyer);
    String otherBuyer = login("buyer02"), key = body.path("requestKey").asText();
    postJson(
            "/api/v1/suggestions/by-request/" + key + "/generate",
            otherBuyer,
            json.createObjectNode())
        .andExpect(status().isConflict());
    clock.advance(Duration.ofMinutes(11));
    postJson("/api/v1/suggestions/by-request/" + key + "/generate", buyer, json.createObjectNode())
        .andExpect(status().isConflict());
    assertThat(model.calls.get()).isZero();
  }

  @Test
  void emptyResultIsFailureAndInventoryWithoutSalesIsExplicit() throws Exception {
    String p = fixture(false, 5);
    var body = previewBody("INVENTORY", p);
    var summary = preview(body, buyer);
    assertThat(summary.path("summary").path("salesHistory").asText()).contains("无销售历史");
    model.mode = "EMPTY";
    var result = generate(body.path("requestKey").asText(), buyer);
    assertThat(result.path("record").path("status").asText()).isEqualTo("FAILURE");
    assertThat(result.path("errorCode").asText()).isEqualTo("EMPTY_RESULT");
    assertThat(result.path("content").isNull()).isTrue();
    complete(sale(p, 1));
    assertThat(stock(p)).isEqualTo(4);
  }

  @Test
  void originalSeedFixtureIsClearlyMarkedAndLegacyIdsAreStrings() throws Exception {
    var result =
        data(
            mvc.perform(get("/api/v1/suggestions/1").header("Authorization", "Bearer " + buyer))
                .andExpect(status().isOk()));
    assertThat(result.path("record").path("fixture").asBoolean()).isTrue();
    assertThat(result.path("filters").path("productIds").get(0).isTextual()).isTrue();
  }

  @AfterEach
  void cleanup() {
    if (!ownKeys.isEmpty())
      new TransactionTemplate(transactions)
          .executeWithoutResult(
              state -> {
                for (String key : ownKeys) {
                  jdbc.update(
                      "DELETE FROM operation_log WHERE target_type='AI' AND target_no=?", key);
                  jdbc.update("DELETE FROM ai_suggestion WHERE request_key=?", key);
                }
              });
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
