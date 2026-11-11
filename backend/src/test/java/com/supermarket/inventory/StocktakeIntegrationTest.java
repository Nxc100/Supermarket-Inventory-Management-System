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
@Import(StocktakeIntegrationTest.TimeConfiguration.class)
class StocktakeIntegrationTest {
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
            .put("code", "QA_PD_" + UUID.randomUUID().toString().substring(0, 12))
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

  @Test
  void startSnapshotIncludesEmptyBatchAndNoUnknownSource() throws Exception {
    String p = fixture(true, 2, 3);
    var sold = complete(sale(p, 2));
    var task = start(buyer, p);
    assertThat(task.path("items").size()).isEqualTo(2);
    assertThat(task.path("items").get(0).path("bookQty").asLong()).isZero();
    assertThat(task.path("items").get(0).path("actualQty").isNull()).isTrue();
    assertThat(task.path("order").path("differenceQty").isNull()).isTrue();
    assertThat(freeze(p)).isPositive();
    assertThat(stock(p)).isEqualTo(3);
    var same = json.createObjectNode();
    same.putArray("productIds").add(p);
    postJson("/api/v1/stocktakes", admin, same).andExpect(status().isConflict());
    String fresh = fixture(false, 1);
    var purchase = receipts.get(fresh);
    postJson(
            "/api/v1/purchases/" + purchase.path("order").path("id").asText() + "/reverse",
            admin,
            version(purchase).put("reason", "撤销尚无后续历史来源"))
        .andExpect(status().isOk());
    var unknown = json.createObjectNode();
    unknown.putArray("productIds").add(fresh);
    postJson("/api/v1/stocktakes", buyer, unknown).andExpect(status().isConflict());
    assertThat(freeze(fresh)).isZero();
  }

  @Test
  void nullZeroAndDifferenceValidationPreserveAtomicSnapshot() throws Exception {
    String p = fixture(false, 5, 5);
    var task = start(buyer, p);
    postJson(
            "/api/v1/stocktakes/" + task.path("order").path("id").asText() + "/submit",
            buyer,
            version(task))
        .andExpect(status().isConflict());
    var partial = data(saveCounts(task, null, 5L).andExpect(status().isOk()));
    assertThat(partial.path("order").path("countedCount").asInt()).isEqualTo(1);
    assertThat(partial.path("order").path("actualQty").isNull()).isTrue();
    var filled = data(saveCounts(partial, 0L, 7L).andExpect(status().isOk()));
    assertThat(filled.path("items").get(0).path("actualQty").asLong()).isZero();
    assertThat(stock(p)).isEqualTo(10);
    var pending = command(filled, "submit", buyer);
    assertThat(freeze(p)).isPositive();
    var done = command(pending, "approve", admin);
    assertThat(done.path("order").path("status").asText()).isEqualTo("COMPLETED");
    assertThat(stock(p)).isEqualTo(7);
    assertThat(freeze(p)).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_movement WHERE product_id=? AND event_type IN ('STOCKTAKE_GAIN','STOCKTAKE_LOSS')",
                Long.class,
                p))
        .isEqualTo(2);
    postJson(
            "/api/v1/stocktakes/" + task.path("order").path("id").asText() + "/approve",
            admin,
            version(done))
        .andExpect(status().isConflict());
    assertThat(stock(p)).isEqualTo(7);
  }

  @Test
  void roleOwnershipAndForgedInputsAreRejected() throws Exception {
    String p = fixture(false, 5);
    var task = start(buyer, p);
    String id = task.path("order").path("id").asText(), otherBuyer = login("buyer02");
    mvc.perform(get("/api/v1/stocktakes/" + id).header("Authorization", "Bearer " + otherBuyer))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/v1/stocktakes")
                .param("keyword", task.path("order").path("orderNo").asText())
                .header("Authorization", "Bearer " + otherBuyer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(0));
    mvc.perform(get("/api/v1/stocktakes/" + id).header("Authorization", "Bearer " + cashier))
        .andExpect(status().isForbidden());
    postJson("/api/v1/stocktakes/" + id + "/approve", buyer, version(task))
        .andExpect(status().isForbidden());
    var input = json.createObjectNode().put("operatorId", "1");
    input.putArray("productIds").add(p);
    postJson("/api/v1/stocktakes", buyer, input).andExpect(status().isBadRequest());
    var body = version(task);
    body.putArray("items")
        .addObject()
        .put("id", task.path("items").get(0).path("id").asText())
        .put("actualQty", 1.5);
    mvc.perform(
            put("/api/v1/stocktakes/" + id + "/counts")
                .header("Authorization", "Bearer " + buyer)
                .contentType("application/json")
                .content(body.toString()))
        .andExpect(status().isBadRequest());
    ((ObjectNode) body.path("items").get(0)).put("actualQty", 4);
    mvc.perform(
            put("/api/v1/stocktakes/" + id + "/counts")
                .header("Authorization", "Bearer " + buyer)
                .contentType("application/json")
                .content(body.toString()))
        .andExpect(status().isBadRequest());
    assertThat(stock(p)).isEqualTo(5);
    assertThat(freeze(p)).isPositive();
  }

  @Test
  void freezeBlocksSalesCorrectionAndPurchaseCommandsWithoutZeroingSaleable() throws Exception {
    String p = fixture(false, 5);
    var sold = complete(sale(p, 1));
    var task = start(buyer, p);
    postJson("/api/v1/sales/complete", cashier, sale(p, 1)).andExpect(status().isConflict());
    postJson(
            "/api/v1/sales/" + sold.path("order").path("id").asText() + "/correct",
            admin,
            correction(sold))
        .andExpect(status().isConflict());
    var receipt = receipts.get(p);
    postJson(
            "/api/v1/purchases/" + receipt.path("order").path("id").asText() + "/reverse",
            admin,
            version(receipt).put("reason", "冻结时尝试撤销来源采购"))
        .andExpect(status().isConflict());
    var inventory =
        data(
            mvc.perform(
                    get("/api/v1/inventory/saleable-products")
                        .param(
                            "keyword",
                            jdbc.queryForObject(
                                "SELECT code FROM product WHERE id=?", String.class, p))
                        .header("Authorization", "Bearer " + cashier))
                .andExpect(status().isOk()));
    assertThat(inventory.path("records").get(0).path("saleableQty").asLong()).isEqualTo(4);
    assertThat(inventory.path("records").get(0).path("frozen").asBoolean()).isTrue();
    var closed = command(task, "close", buyer);
    assertThat(stock(p)).isEqualTo(4);
    assertThat(freeze(p)).isZero();
    complete(sale(p, 1));
    assertThat(stock(p)).isEqualTo(3);
  }

  @Test
  void rejectionWithdrawalAndCloseRetainThenReleaseFreeze() throws Exception {
    String p = fixture(false, 5);
    var task = start(buyer, p);
    task = data(saveCounts(task, 4L).andExpect(status().isOk()));
    task = command(task, "submit", buyer);
    task = command(task, "reject", admin);
    assertThat(freeze(p)).isPositive();
    assertThat(task.path("rejectionReason").asText()).isNotBlank();
    task = data(saveCounts(task, 5L).andExpect(status().isOk()));
    task = command(task, "submit", buyer);
    postJson(
            "/api/v1/stocktakes/" + task.path("order").path("id").asText() + "/close",
            buyer,
            version(task).put("reason", "未撤回不能关闭"))
        .andExpect(status().isConflict());
    task = command(task, "withdraw", buyer);
    assertThat(freeze(p)).isPositive();
    command(task, "close", buyer);
    assertThat(freeze(p)).isZero();
    assertThat(stock(p)).isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_movement WHERE product_id=? AND stocktake_item_id IS NOT NULL",
                Long.class,
                p))
        .isZero();
  }

  @Test
  void zeroDifferenceHasNoLedgerAndDisabledExpiredBatchCanBeCleared() throws Exception {
    String p = fixture(true, 5);
    var task = start(buyer, p);
    task = data(saveCounts(task, 5L).andExpect(status().isOk()));
    command(command(task, "submit", buyer), "approve", admin);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_movement WHERE product_id=? AND stocktake_item_id IS NOT NULL",
                Long.class,
                p))
        .isZero();
    clock.advance(Duration.ofDays(1));
    buyer = login("buyer01");
    admin = login("admin");
    jdbc.update("UPDATE product SET enabled=0 WHERE id=?", p);
    task = start(buyer, p);
    task = data(saveCounts(task, 0L).andExpect(status().isOk()));
    command(command(task, "submit", buyer), "approve", admin);
    assertThat(stock(p)).isZero();
    assertThat(freeze(p)).isZero();
  }

  @Test
  void staleSnapshotRejectsApprovalAndAdministratorCanCloseAbnormalTask() throws Exception {
    String p = fixture(false, 5);
    var task = start(buyer, p);
    task = data(saveCounts(task, 4L).andExpect(status().isOk()));
    task = command(task, "submit", buyer);
    jdbc.update("UPDATE purchase_item SET stock_qty=6 WHERE product_id=?", p);
    postJson(
            "/api/v1/stocktakes/" + task.path("order").path("id").asText() + "/approve",
            admin,
            version(task))
        .andExpect(status().isConflict());
    assertThat(stock(p)).isEqualTo(5);
    assertThat(freeze(p)).isPositive();
    command(task, "close", admin);
    assertThat(freeze(p)).isZero();
  }

  @Test
  void injectedSecondMovementFailureRollsBackAllBalancesStatusAndFreeze() throws Exception {
    String p = fixture(false, 5, 5);
    var task = start(buyer, p);
    task = data(saveCounts(task, 4L, 4L).andExpect(status().isOk()));
    task = command(task, "submit", buyer);
    var item = task.path("items").get(1);
    long batch = Long.parseLong(item.path("batchItemId").asText());
    jdbc.update(
        "INSERT INTO stock_movement(event_type,product_id,batch_item_id,stocktake_item_id,business_no,delta_qty,batch_before,batch_after,product_before,product_after,actor_id,actor_name,reason,occurred_at) VALUES('STOCKTAKE_LOSS',?,?,?,'QA-CONFLICT',-1,5,4,10,9,1,'test','injected unique source',?)",
        p,
        batch,
        item.path("id").asText(),
        LocalDateTime.now(clock));
    postJson(
            "/api/v1/stocktakes/" + task.path("order").path("id").asText() + "/approve",
            admin,
            version(task))
        .andExpect(status().isConflict());
    assertThat(stock(p)).isEqualTo(10);
    assertThat(freeze(p)).isPositive();
    assertThat(
            jdbc.queryForList(
                "SELECT stock_qty FROM purchase_item WHERE product_id=? ORDER BY id",
                Long.class,
                p))
        .containsExactly(5L, 5L);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM stocktake_order WHERE id=?",
                String.class,
                task.path("order").path("id").asText()))
        .isEqualTo("PENDING");
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_movement WHERE product_id=? AND stocktake_item_id IS NOT NULL",
                Long.class,
                p))
        .isEqualTo(1);
  }

  @Test
  void concurrentApproveAndCloseOnlyOneStateTransitionWins() throws Exception {
    String p = fixture(false, 5);
    var task = start(buyer, p);
    task = data(saveCounts(task, 4L).andExpect(status().isOk()));
    task = command(task, "submit", buyer);
    final var captured = task;
    var ready = new CountDownLatch(2);
    var go = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var a =
          executor.submit(
              () -> {
                ready.countDown();
                go.await();
                return postJson(
                        "/api/v1/stocktakes/"
                            + captured.path("order").path("id").asText()
                            + "/approve",
                        admin,
                        version(captured))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      var b =
          executor.submit(
              () -> {
                ready.countDown();
                go.await();
                return postJson(
                        "/api/v1/stocktakes/"
                            + captured.path("order").path("id").asText()
                            + "/close",
                        admin,
                        version(captured).put("reason", "竞争关闭任务"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      go.countDown();
      assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
      assertThat(freeze(p)).isZero();
      assertThat(stock(p)).isIn(4L, 5L);
    } finally {
      go.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void startAndSaleRaceSnapshotContainsExactlyTheWinningStockWrite() throws Exception {
    String p = fixture(false, 5);
    var startBody = json.createObjectNode();
    startBody.putArray("productIds").add(p);
    var saleBody = sale(p, 2);
    var outcomes =
        race(
            () ->
                postJson("/api/v1/stocktakes", buyer, startBody)
                    .andReturn()
                    .getResponse()
                    .getStatus(),
            () ->
                postJson("/api/v1/sales/complete", cashier, saleBody)
                    .andReturn()
                    .getResponse()
                    .getStatus());
    assertThat(outcomes.get(0)).isEqualTo(200);
    assertThat(outcomes.get(1)).isIn(200, 409);
    long expected = outcomes.get(1) == 200 ? 3 : 5;
    assertThat(stock(p)).isEqualTo(expected);
    assertThat(
            jdbc.queryForObject(
                "SELECT book_qty FROM stocktake_item WHERE product_id=?", Long.class, p))
        .isEqualTo(expected);
    assertThat(freeze(p)).isPositive();
  }

  @Test
  void purchaseApprovalAndStartRaceDoesNotAddBatchesAfterFreeze() throws Exception {
    String p = fixture(false, 5);
    var body =
        json.createObjectNode()
            .put("supplierId", "1")
            .put("arrivedAt", OffsetDateTime.now(clock).minusSeconds(1).toString());
    body.putArray("items")
        .addObject()
        .put("productId", p)
        .put("quantity", 2)
        .put("receivedQty", 2)
        .put("unitCost", "3.00");
    var draft = data(postJson("/api/v1/purchases", admin, body).andExpect(status().isOk()));
    var pending =
        data(
            postJson(
                    "/api/v1/purchases/" + draft.path("order").path("id").asText() + "/submit",
                    admin,
                    version(draft))
                .andExpect(status().isOk()));
    var startBody = json.createObjectNode();
    startBody.putArray("productIds").add(p);
    var outcomes =
        race(
            () ->
                postJson("/api/v1/stocktakes", buyer, startBody)
                    .andReturn()
                    .getResponse()
                    .getStatus(),
            () ->
                postJson(
                        "/api/v1/purchases/"
                            + pending.path("order").path("id").asText()
                            + "/approve",
                        admin,
                        version(pending))
                    .andReturn()
                    .getResponse()
                    .getStatus());
    assertThat(outcomes.get(0)).isEqualTo(200);
    assertThat(outcomes.get(1)).isIn(200, 409);
    long expected = outcomes.get(1) == 200 ? 7 : 5;
    assertThat(stock(p)).isEqualTo(expected);
    assertThat(freeze(p)).isPositive();
    assertThat(
            jdbc.queryForObject(
                "SELECT SUM(book_qty) FROM stocktake_item WHERE product_id=?", Long.class, p))
        .isEqualTo(expected);
  }

  @Test
  void competingStartsAndStaleSaveCannotOverwriteActiveSnapshot() throws Exception {
    String p = fixture(false, 5);
    var body = json.createObjectNode();
    body.putArray("productIds").add(p);
    var outcomes =
        race(
            () -> postJson("/api/v1/stocktakes", buyer, body).andReturn().getResponse().getStatus(),
            () ->
                postJson("/api/v1/stocktakes", admin, body).andReturn().getResponse().getStatus());
    assertThat(outcomes).containsExactlyInAnyOrder(200, 409);
    var task =
        data(
            mvc.perform(
                    get("/api/v1/stocktakes/" + freeze(p))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()));
    // The administrator may continue an owner's task; an old version cannot overwrite its saved
    // counts.
    String originalBuyer = buyer;
    buyer = admin;
    try {
      saveCounts(task, 5L).andExpect(status().isOk());
      saveCounts(task, 4L).andExpect(status().isConflict());
    } finally {
      buyer = originalBuyer;
    }
    assertThat(stock(p)).isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "SELECT actual_qty FROM stocktake_item WHERE product_id=?", Long.class, p))
        .isEqualTo(5);
  }

  List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
    var ready = new CountDownLatch(2);
    var go = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var a =
          pool.submit(
              () -> {
                ready.countDown();
                if (!go.await(5, TimeUnit.SECONDS))
                  throw new IllegalStateException("race start timeout");
                return first.call();
              });
      var b =
          pool.submit(
              () -> {
                ready.countDown();
                if (!go.await(5, TimeUnit.SECONDS))
                  throw new IllegalStateException("race start timeout");
                return second.call();
              });
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      go.countDown();
      return List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
    } finally {
      go.countDown();
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
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
