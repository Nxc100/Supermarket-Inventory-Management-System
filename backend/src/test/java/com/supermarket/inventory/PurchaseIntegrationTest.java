package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.math.BigDecimal;
import java.time.*;
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
class PurchaseIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource source;
  @Autowired Clock clock;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
  boolean committedFixture;
  String admin, buyer, productId;

  @BeforeEach
  void setup() throws Exception {
    try (var c = source.getConnection()) {
      assertThat(c.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
    admin = login("admin");
    buyer = login("buyer01");
    productId = product(false);
  }

  String login(String name) throws Exception {
    return data(mvc.perform(
                post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("username", name, "password", "Test123456"))))
            .andExpect(status().isOk()))
        .path("token")
        .asText();
  }

  JsonNode data(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
  }

  String product(boolean expiry) throws Exception {
    ObjectNode p =
        json.createObjectNode()
            .put("code", "QA_CG_" + UUID.randomUUID().toString().substring(0, 8))
            .put("name", "采购集成商品")
            .put("categoryId", "1")
            .put("unit", "瓶")
            .put("salePrice", "5.00")
            .put("managesExpiry", expiry)
            .put("nearExpiryDays", 0)
            .put("stockLowerLimit", 0)
            .put("canPurchase", true)
            .put("canSell", true);
    if (expiry) p.put("shelfLifeDays", 30);
    return data(mvc.perform(
                post("/api/v1/products")
                    .header("Authorization", "Bearer " + admin)
                    .contentType("application/json")
                    .content(p.toString()))
            .andExpect(status().isOk()))
        .path("id")
        .asText();
  }

  ObjectNode draft(boolean arrived) {
    ObjectNode d = json.createObjectNode().put("supplierId", "1").put("remark", "采购集成测试");
    if (arrived) d.put("arrivedAt", OffsetDateTime.now(clock).minusMinutes(1).toString());
    ArrayNode items = d.putArray("items");
    for (int i = 0; i < 2; i++) {
      var line =
          items
              .addObject()
              .put("productId", productId)
              .put("quantity", 10)
              .put("unitCost", "3.00")
              .put("supplierBatchNo", "TEST_BATCH_" + i);
      if (arrived) line.put("receivedQty", 10);
    }
    return d;
  }

  JsonNode create(ObjectNode input) throws Exception {
    return data(
        mvc.perform(
                post("/api/v1/purchases")
                    .header("Authorization", "Bearer " + buyer)
                    .contentType("application/json")
                    .content(input.toString()))
            .andExpect(status().isOk()));
  }

  String id(JsonNode detail) {
    return detail.path("order").path("id").asText();
  }

  ObjectNode version(JsonNode detail) {
    return json.createObjectNode()
        .put("expectedVersion", detail.path("order").path("rowVersion").asLong());
  }

  ResultActions action(String command, JsonNode detail, String token, ObjectNode body)
      throws Exception {
    return mvc.perform(
        post("/api/v1/purchases/" + id(detail) + "/" + command)
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content(body.toString()));
  }

  JsonNode submit(JsonNode detail) throws Exception {
    return data(action("submit", detail, buyer, version(detail)).andExpect(status().isOk()));
  }

  JsonNode approve(JsonNode detail) throws Exception {
    return data(action("approve", detail, admin, version(detail)).andExpect(status().isOk()));
  }

  long stock() {
    return jdbc.queryForObject("SELECT stock_qty FROM product WHERE id=?", Long.class, productId);
  }

  long movements() {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM stock_movement WHERE product_id=?", Long.class, productId);
  }

  ObjectNode reconcile(JsonNode detail) {
    ObjectNode r =
        version(detail)
            .put("expectedReconcileVersion", detail.path("order").path("reconcileVersion").asInt())
            .put("externalTotal", "60.00")
            .put("reference", "TEST_VOUCHER");
    ArrayNode lines = r.putArray("lines");
    for (JsonNode item : detail.path("items"))
      lines
          .addObject()
          .put("itemId", item.path("id").asText())
          .put("externalQty", item.path("quantity").asInt())
          .put("externalUnitCost", item.path("unitCost").asText());
    return r;
  }

  @Test
  void fractionalQuantitiesCannotBeSilentlyTruncated() throws Exception {
    ObjectNode quantity = draft(true);
    ((ObjectNode) quantity.path("items").get(0)).put("quantity", 1.5);
    mvc.perform(
            post("/api/v1/purchases")
                .header("Authorization", "Bearer " + buyer)
                .contentType("application/json")
                .content(quantity.toString()))
        .andExpect(status().isBadRequest());
    ObjectNode received = draft(true);
    ((ObjectNode) received.path("items").get(0)).put("receivedQty", 0.5);
    mvc.perform(
            post("/api/v1/purchases")
                .header("Authorization", "Bearer " + buyer)
                .contentType("application/json")
                .content(received.toString()))
        .andExpect(status().isBadRequest());
    assertThat(stock()).isZero();
    assertThat(movements()).isZero();
  }

  @Test
  void draftSubmitApproveAndRepeatedApprovePreserveStockInvariants() throws Exception {
    JsonNode saved = create(draft(true));
    assertThat(stock()).isZero();
    assertThat(movements()).isZero();
    action("approve", saved, admin, version(saved)).andExpect(status().isConflict());
    JsonNode pending = submit(saved);
    assertThat(stock()).isZero();
    assertThat(pending.path("order").path("status").asText()).isEqualTo("PENDING");
    JsonNode stocked = approve(pending);
    assertThat(stock()).isEqualTo(20);
    assertThat(movements()).isEqualTo(2);
    assertThat(stocked.path("order").path("totalAmount").asText()).isEqualTo("60.00");
    var balances =
        jdbc.queryForList(
            "SELECT batch_before,batch_after,product_before,product_after FROM stock_movement WHERE product_id=? ORDER BY id",
            productId);
    assertThat(((Number) balances.get(0).get("product_before")).longValue()).isZero();
    assertThat(((Number) balances.get(1).get("product_after")).longValue()).isEqualTo(20);
    action("approve", pending, admin, version(pending)).andExpect(status().isConflict());
    assertThat(stock()).isEqualTo(20);
    assertThat(movements()).isEqualTo(2);
  }

  @Test
  void partialArrivalAndMissingDatesCannotSubmitButZeroRemainsDistinctFromMissing()
      throws Exception {
    JsonNode saved = create(draft(false));
    assertThat(saved.path("items").get(0).path("receivedQty").isNull()).isTrue();
    action("submit", saved, buyer, version(saved)).andExpect(status().isConflict());
    ObjectNode input = draft(true);
    ((ObjectNode) input.path("items").get(0)).put("receivedQty", 0);
    JsonNode zero = create(input);
    assertThat(zero.path("items").get(0).path("receivedQty").asInt()).isZero();
    action("submit", zero, buyer, version(zero)).andExpect(status().isConflict());
    productId = product(true);
    JsonNode missing = create(draft(true));
    action("submit", missing, buyer, version(missing)).andExpect(status().isConflict());
    assertThat(stock()).isZero();
  }

  @Test
  void expiryTodayIsAllowedAndFutureProductionIsRejected() throws Exception {
    productId = product(true);
    ObjectNode input = draft(true);
    for (JsonNode line : input.path("items")) {
      ((ObjectNode) line)
          .put("productionDate", LocalDate.now(clock).minusDays(1).toString())
          .put("expiryDate", LocalDate.now(clock).toString());
    }
    approve(submit(create(input)));
    assertThat(stock()).isEqualTo(20);
    ((ObjectNode) input.path("items").get(0))
        .put("productionDate", LocalDate.now(clock).plusDays(1).toString());
    mvc.perform(
            post("/api/v1/purchases")
                .header("Authorization", "Bearer " + buyer)
                .contentType("application/json")
                .content(input.toString()))
        .andExpect(status().isConflict());
    assertThat(stock()).isEqualTo(20);
  }

  @Test
  void ownershipRolesAndForgedFieldsAreEnforced() throws Exception {
    JsonNode saved = create(draft(true));
    String other = login("buyer02"), cashier = login("cashier01");
    mvc.perform(get("/api/v1/purchases/" + id(saved)).header("Authorization", "Bearer " + other))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/v1/purchases")
                .param("keyword", saved.path("order").path("orderNo").asText())
                .header("Authorization", "Bearer " + other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(0));
    mvc.perform(get("/api/v1/purchases").header("Authorization", "Bearer " + cashier))
        .andExpect(status().isForbidden());
    action("approve", saved, buyer, version(saved)).andExpect(status().isForbidden());
    ObjectNode forged = draft(true).put("operatorId", "1");
    mvc.perform(
            post("/api/v1/purchases")
                .header("Authorization", "Bearer " + buyer)
                .contentType("application/json")
                .content(forged.toString()))
        .andExpect(status().isBadRequest());
    assertThat(stock()).isZero();
  }

  @Test
  void submittedHistoryPreventsDeletionAndRejectEditRetainsState() throws Exception {
    JsonNode saved = create(draft(true));
    JsonNode pending = submit(saved);
    JsonNode withdrawn =
        data(action("withdraw", pending, buyer, version(pending)).andExpect(status().isOk()));
    mvc.perform(
            delete("/api/v1/purchases/" + id(withdrawn))
                .header("Authorization", "Bearer " + buyer)
                .contentType("application/json")
                .content(version(withdrawn).toString()))
        .andExpect(status().isConflict());
    pending = submit(withdrawn);
    JsonNode rejected =
        data(
            action("reject", pending, admin, version(pending).put("reason", "到货信息需复核"))
                .andExpect(status().isOk()));
    ObjectNode input =
        draft(true).put("expectedVersion", rejected.path("order").path("rowVersion").asLong());
    JsonNode edited =
        data(
            mvc.perform(
                    patch("/api/v1/purchases/" + id(rejected))
                        .header("Authorization", "Bearer " + buyer)
                        .contentType("application/json")
                        .content(input.toString()))
                .andExpect(status().isOk()));
    assertThat(edited.path("order").path("status").asText()).isEqualTo("REJECTED");
    assertThat(stock()).isZero();
  }

  @Test
  void approvalRechecksSupplierProductAndFreeze() throws Exception {
    JsonNode pending = submit(create(draft(true)));
    jdbc.update("UPDATE supplier SET enabled=0 WHERE id=1");
    action("approve", pending, admin, version(pending)).andExpect(status().isConflict());
    jdbc.update("UPDATE supplier SET enabled=1 WHERE id=1");
    jdbc.update("UPDATE product SET can_purchase=0 WHERE id=?", productId);
    action("approve", pending, admin, version(pending)).andExpect(status().isConflict());
    jdbc.update("UPDATE product SET can_purchase=1,frozen_stocktake_id=2 WHERE id=?", productId);
    action("approve", pending, admin, version(pending)).andExpect(status().isConflict());
    assertThat(stock()).isZero();
    assertThat(movements()).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM purchase_item WHERE purchase_id=? AND batch_no IS NOT NULL",
                Long.class,
                id(pending)))
        .isZero();
  }

  @Test
  void reconciliationDifferenceMatchWithdrawalAndReversalKeepHistoryAndAmounts() throws Exception {
    JsonNode stocked = approve(submit(create(draft(true))));
    ObjectNode difference =
        reconcile(stocked).put("externalTotal", "59.00").put("difference", "外部凭据数量及合计有差异");
    ((ObjectNode) difference.path("lines").get(0)).put("externalQty", 9);
    JsonNode diff =
        data(action("reconcile-difference", stocked, buyer, difference).andExpect(status().isOk()));
    assertThat(stock()).isEqualTo(20);
    assertThat(diff.path("order").path("totalAmount").asText()).isEqualTo("60.00");
    ObjectNode sameTotal = reconcile(diff).put("resolution", "复核后确认");
    ((ObjectNode) sameTotal.path("lines").get(0)).put("externalUnitCost", "2.00");
    ((ObjectNode) sameTotal.path("lines").get(1)).put("externalUnitCost", "4.00");
    action("reconcile-match", diff, buyer, sameTotal).andExpect(status().isConflict());
    JsonNode matched =
        data(
            action("reconcile-match", diff, buyer, reconcile(diff).put("resolution", "复核凭据后已更正"))
                .andExpect(status().isOk()));
    action("reverse", matched, admin, version(matched).put("reason", "错误入库核实"))
        .andExpect(status().isConflict());
    JsonNode unrec =
        data(
            action(
                    "withdraw-reconciliation",
                    matched,
                    buyer,
                    version(matched)
                        .put(
                            "expectedReconcileVersion",
                            matched.path("order").path("reconcileVersion").asInt())
                        .put("reason", "重新核对凭证"))
                .andExpect(status().isOk()));
    assertThat(unrec.path("reconcileHistory").size()).isEqualTo(3);
    assertThat(unrec.path("items").get(0).path("externalQty").isNull()).isTrue();
    assertThat(unrec.path("reconcileHistory").get(0).path("before").path("status").asText())
        .isEqualTo("MATCHED");
    JsonNode reversed =
        data(
            action("reverse", unrec, admin, version(unrec).put("reason", "实物未使用且录入重复"))
                .andExpect(status().isOk()));
    assertThat(stock()).isZero();
    assertThat(movements()).isEqualTo(4);
    assertThat(reversed.path("order").path("reconcileStatus").asText()).isEqualTo("NA");
    action("reverse", reversed, admin, version(reversed).put("reason", "不能重复撤销"))
        .andExpect(status().isConflict());
    assertThat(
            jdbc.queryForObject(
                "SELECT SUM(signed_amount) FROM v_purchase_event WHERE purchase_id=?",
                BigDecimal.class,
                id(reversed)))
        .isEqualByComparingTo("0.00");
  }

  @Test
  void supplierSummaryUsesEventNetAndScopedDrilldown() throws Exception {
    String other = login("buyer02");
    JsonNode context =
        data(
            mvc.perform(get("/api/v1/purchases/context").header("Authorization", "Bearer " + buyer))
                .andExpect(status().isOk()));
    assertThat(context.path("today").asText()).isEqualTo(LocalDate.now(clock).toString());
    mvc.perform(
            get("/api/v1/purchases/supplier-options")
                .param("enabled", "true")
                .header("Authorization", "Bearer " + buyer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[0].id").isString());
    JsonNode stocked = approve(submit(create(draft(true))));
    BigDecimal buyerBefore = summaryAmount(buyer), otherBefore = summaryAmount(other);
    assertThat(buyerBefore).isGreaterThanOrEqualTo(new BigDecimal("60.00"));
    mvc.perform(
            get("/api/v1/purchases")
                .param("keyword", stocked.path("order").path("orderNo").asText())
                .param("dateBasis", "EVENT")
                .param("startDate", LocalDate.now(clock).toString())
                .param("endDate", LocalDate.now(clock).toString())
                .header("Authorization", "Bearer " + buyer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1));
    data(
        action("reverse", stocked, admin, version(stocked).put("reason", "汇总净额验证撤销"))
            .andExpect(status().isOk()));
    assertThat(summaryAmount(buyer))
        .isEqualByComparingTo(buyerBefore.subtract(new BigDecimal("60.00")));
    assertThat(summaryAmount(other)).isEqualByComparingTo(otherBefore);
    mvc.perform(
            get("/api/v1/purchases/supplier-summary")
                .param("startDate", "2026-11-11")
                .param("endDate", "2026-11-10")
                .header("Authorization", "Bearer " + buyer))
        .andExpect(status().isBadRequest());
  }

  BigDecimal summaryAmount(String token) throws Exception {
    JsonNode rows =
        data(mvc.perform(
                    get("/api/v1/purchases/supplier-summary")
                        .param("supplierId", "1")
                        .param("startDate", LocalDate.now(clock).toString())
                        .param("endDate", LocalDate.now(clock).toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()))
            .path("records");
    return rows.isEmpty()
        ? BigDecimal.ZERO
        : new BigDecimal(rows.get(0).path("netAmount").asText());
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void concurrentApproveAndWithdrawOnlyOneTransitionCommits() throws Exception {
    committedFixture = true;
    JsonNode pending = submit(create(draft(true)));
    assertThat(race(pending, "approve", admin, "withdraw", buyer))
        .containsExactlyInAnyOrder(200, 409);
    String state =
        jdbc.queryForObject(
            "SELECT status FROM purchase_order WHERE id=?", String.class, id(pending));
    assertThat(state).isIn("IN_STOCK", "DRAFT");
    assertThat(stock()).isEqualTo(state.equals("IN_STOCK") ? 20 : 0);
    assertThat(movements()).isEqualTo(state.equals("IN_STOCK") ? 2 : 0);
    assertThat(
            jdbc.queryForObject(
                "SELECT row_version FROM purchase_order WHERE id=?", Long.class, id(pending)))
        .isEqualTo(pending.path("order").path("rowVersion").asLong() + 1);
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void concurrentDuplicateApprovalCannotDoubleReceive() throws Exception {
    committedFixture = true;
    JsonNode pending = submit(create(draft(true)));
    assertThat(race(pending, "approve", admin, "approve", admin))
        .containsExactlyInAnyOrder(200, 409);
    assertThat(stock()).isEqualTo(20);
    assertThat(movements()).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT SUM(stock_qty) FROM purchase_item WHERE purchase_id=?",
                Long.class,
                id(pending)))
        .isEqualTo(20);
  }

  List<Integer> race(
      JsonNode detail, String first, String firstToken, String second, String secondToken)
      throws Exception {
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    var ready = new java.util.concurrent.CountDownLatch(2);
    var start = new java.util.concurrent.CountDownLatch(1);
    try {
      var a =
          pool.submit(
              () -> {
                ready.countDown();
                if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS))
                  throw new IllegalStateException("start timeout");
                return action(first, detail, firstToken, version(detail))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      var b =
          pool.submit(
              () -> {
                ready.countDown();
                if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS))
                  throw new IllegalStateException("start timeout");
                return action(second, detail, secondToken, version(detail))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      start.countDown();
      return List.of(
          a.get(15, java.util.concurrent.TimeUnit.SECONDS),
          b.get(15, java.util.concurrent.TimeUnit.SECONDS));
    } finally {
      start.countDown();
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void reversalRejectsFreezeAndExistingDownstreamMovementHistory() throws Exception {
    JsonNode stocked = approve(submit(create(draft(true))));
    jdbc.update("UPDATE product SET frozen_stocktake_id=2 WHERE id=?", productId);
    action("reverse", stocked, admin, version(stocked).put("reason", "冻结时禁止纠错"))
        .andExpect(status().isConflict());
    assertThat(stock()).isEqualTo(20);
    assertThat(movements()).isEqualTo(2);
    JsonNode historical =
        data(
            mvc.perform(get("/api/v1/purchases/1").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()));
    JsonNode unrec =
        data(
            action(
                    "withdraw-reconciliation",
                    historical,
                    admin,
                    version(historical)
                        .put(
                            "expectedReconcileVersion",
                            historical.path("order").path("reconcileVersion").asInt())
                        .put("reason", "验证已有后续流水不可撤销"))
                .andExpect(status().isOk()));
    long oldMovements =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM stock_movement WHERE batch_item_id IN (SELECT id FROM purchase_item WHERE purchase_id=1)",
            Long.class);
    action("reverse", unrec, admin, version(unrec).put("reason", "批次发生过销售及盘点"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("后续流水")));
    assertThat(jdbc.queryForObject("SELECT status FROM purchase_order WHERE id=1", String.class))
        .isEqualTo("IN_STOCK");
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_movement WHERE batch_item_id IN (SELECT id FROM purchase_item WHERE purchase_id=1)",
                Long.class))
        .isEqualTo(oldMovements);
  }

  @AfterEach
  void cleanupCommittedFixture() {
    if (!committedFixture) return;
    new org.springframework.transaction.support.TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              var ids =
                  jdbc.queryForList(
                      "SELECT DISTINCT purchase_id FROM purchase_item WHERE product_id=?",
                      Long.class,
                      productId);
              jdbc.update("DELETE FROM stock_movement WHERE product_id=?", productId);
              for (Long id : ids) {
                jdbc.update(
                    "DELETE FROM operation_log WHERE target_type='PURCHASE' AND target_id=?",
                    id.toString());
                jdbc.update("DELETE FROM purchase_item WHERE purchase_id=?", id);
                jdbc.update("DELETE FROM purchase_order WHERE id=?", id);
              }
              jdbc.update(
                  "DELETE FROM operation_log WHERE target_type='PRODUCT' AND target_id=?",
                  productId);
              jdbc.update("DELETE FROM product WHERE id=?", productId);
            });
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void midTransactionLedgerFailureRollsBackAllStockAndHeaderWrites() throws Exception {
    committedFixture = true;
    JsonNode pending = submit(create(draft(true)));
    String batchId = pending.path("items").get(0).path("id").asText();
    jdbc.update(
        "INSERT INTO stock_movement(event_type,product_id,batch_item_id,business_no,delta_qty,batch_before,batch_after,product_before,product_after,actor_id,actor_name,occurred_at) VALUES('PURCHASE_IN',?,?,'TEST_FAIL_SOURCE',10,0,10,0,10,1,'测试故障',NOW(6))",
        productId,
        batchId);
    action("approve", pending, admin, version(pending)).andExpect(status().isConflict());
    assertThat(stock()).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM purchase_order WHERE id=?", String.class, id(pending)))
        .isEqualTo("PENDING");
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM purchase_item WHERE purchase_id=? AND batch_no IS NOT NULL",
                Long.class,
                id(pending)))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM operation_log WHERE action='PURCHASE_APPROVE' AND target_id=?",
                Long.class,
                id(pending)))
        .isZero();
  }
}
