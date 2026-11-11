package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Requirement 13.1, real committed commands and aggregates, with only dedicated fixture teardown.
 */
@SpringBootTest(properties = {"app.ai.api-key=", "app.ai.key-file="})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClosedLoopIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource source;
  @Autowired Clock clock;
  final Map<String, String> tokens = new HashMap<>();
  final List<String> products = new ArrayList<>();
  final Map<String, List<String>> targets = new HashMap<>();
  final String marker = "QA_CL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
  String category, supplier;

  JsonNode call(String method, String path, JsonNode body, String actor, int expected)
      throws Exception {
    var request = request(org.springframework.http.HttpMethod.valueOf(method), "/api/v1" + path);
    if (actor != null) request.header("Authorization", "Bearer " + tokens.get(actor));
    if (body != null) request.contentType("application/json").content(body.toString());
    return json.readTree(
            mvc.perform(request)
                .andExpect(status().is(expected))
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("data");
  }

  JsonNode call(String method, String path, JsonNode body, String actor) throws Exception {
    return call(method, path, body, actor, 200);
  }

  ObjectNode version(JsonNode document) {
    return json.createObjectNode()
        .put("expectedVersion", document.path("order").path("rowVersion").asLong());
  }

  void track(String type, String id) {
    targets.computeIfAbsent(type, ignored -> new ArrayList<>()).add(id);
  }

  String product(String suffix) throws Exception {
    var body =
        json.createObjectNode()
            .put("code", marker + suffix)
            .put("name", "闭环饮料" + suffix)
            .put("categoryId", category)
            .put("unit", "瓶")
            .put("referenceCost", "3.00")
            .put("salePrice", "5.00")
            .put("managesExpiry", true)
            .put("shelfLifeDays", 30)
            .put("nearExpiryDays", 7)
            .put("stockLowerLimit", 5)
            .put("canPurchase", true)
            .put("canSell", true);
    String id = call("POST", "/products", body, "admin").path("id").asText();
    products.add(id);
    track("PRODUCT", id);
    return id;
  }

  JsonNode receipt(String product, int... quantities) throws Exception {
    var body =
        json.createObjectNode()
            .put("supplierId", supplier)
            .put("remark", marker)
            .put("arrivedAt", OffsetDateTime.now(clock).minusSeconds(1).toString());
    var items = body.putArray("items");
    for (int n = 0; n < quantities.length; n++)
      items
          .addObject()
          .put("productId", product)
          .put("quantity", quantities[n])
          .put("receivedQty", quantities[n])
          .put("unitCost", "3.00")
          .put("productionDate", LocalDate.now(clock).minusDays(1).toString())
          .put("expiryDate", LocalDate.now(clock).plusDays(n == 0 ? 1 : 20).toString());
    var document = call("POST", "/purchases", body, "buyer01");
    String id = document.path("order").path("id").asText();
    track("PURCHASE", id);
    document = call("POST", "/purchases/" + id + "/submit", version(document), "buyer01");
    return call("POST", "/purchases/" + id + "/approve", version(document), "admin");
  }

  ObjectNode sale(String product, int quantity) {
    var body =
        json.createObjectNode()
            .put("requestKey", UUID.randomUUID().toString())
            .put("paymentMethod", "CASH")
            .put("receivedAmount", "100.00");
    body.putArray("items")
        .addObject()
        .put("productId", product)
        .put("quantity", quantity)
        .put("expectedPrice", "5.00");
    return body;
  }

  JsonNode complete(String product, int quantity) throws Exception {
    var result = call("POST", "/sales/complete", sale(product, quantity), "cashier01");
    track("SALE", result.path("order").path("id").asText());
    return result;
  }

  void balance(String product, long expected) {
    assertThat(jdbc.queryForObject("SELECT stock_qty FROM product WHERE id=?", Long.class, product))
        .isEqualTo(expected);
    assertThat(
            jdbc.queryForObject(
                "SELECT SUM(stock_qty) FROM purchase_item WHERE product_id=?", Long.class, product))
        .isEqualTo(expected);
    assertThat(
            jdbc.queryForObject(
                "SELECT SUM(delta_qty) FROM stock_movement WHERE product_id=?",
                Long.class,
                product))
        .isEqualTo(expected);
  }

  @Test
  void twentyMinusTwelveMinusOneMinusTwoPlusTenEqualsFifteenAndSeventyYuanNetSales()
      throws Exception {
    try (var connection = source.getConnection()) {
      assertThat(connection.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
    for (String actor : List.of("admin", "buyer01", "cashier01"))
      tokens.put(
          actor,
          call(
                  "POST",
                  "/auth/login",
                  json.createObjectNode().put("username", actor).put("password", "Test123456"),
                  null)
              .path("token")
              .asText());
    category =
        call(
                "POST",
                "/categories",
                json.createObjectNode()
                    .put("code", marker)
                    .put("name", "闭环饮料" + marker)
                    .put("sortOrder", 0),
                "admin")
            .path("id")
            .asText();
    track("CATEGORY", category);
    supplier =
        call(
                "POST",
                "/suppliers",
                json.createObjectNode()
                    .put("name", "闭环供应商" + marker)
                    .put("contactName", "闭环验证员")
                    .put("contactPhone", "0710-1234567"),
                "buyer01")
            .path("id")
            .asText();
    track("SUPPLIER", supplier);
    String product = product("A");
    assertThat(jdbc.queryForObject("SELECT stock_qty FROM product WHERE id=?", Long.class, product))
        .isZero();
    var purchase = receipt(product, 10, 10);
    balance(product, 20);
    var reconcile =
        version(purchase)
            .put(
                "expectedReconcileVersion", purchase.path("order").path("reconcileVersion").asInt())
            .put("externalTotal", "60.00")
            .put("reference", marker);
    for (var item : purchase.path("items"))
      reconcile
          .withArray("lines")
          .addObject()
          .put("itemId", item.path("id").asText())
          .put("externalQty", 10)
          .put("externalUnitCost", "3.00");
    call(
        "POST",
        "/purchases/" + purchase.path("order").path("id").asText() + "/reconcile-match",
        reconcile,
        "buyer01");
    balance(product, 20);
    var first = complete(product, 12);
    assertThat(first.path("order").path("totalAmount").asText()).isEqualTo("60.00");
    assertThat(first.path("order").path("changeAmount").asText()).isEqualTo("40.00");
    var allocations =
        jdbc.queryForList(
            "SELECT a.quantity FROM sale_allocation a JOIN sale_item s ON s.id=a.sale_item_id JOIN purchase_item i ON i.id=a.batch_item_id WHERE s.sale_id=? ORDER BY i.expiry_date",
            Long.class,
            first.path("order").path("id").asText());
    assertThat(allocations).containsExactly(10L, 2L);
    balance(product, 8);
    var start = json.createObjectNode().put("remark", marker);
    start.putArray("productIds").add(product);
    var stocktake = call("POST", "/stocktakes", start, "buyer01");
    String task = stocktake.path("order").path("id").asText();
    track("STOCKTAKE", task);
    call("POST", "/sales/complete", sale(product, 1), "cashier01", 409);
    var counts = version(stocktake);
    for (var item : stocktake.path("items")) {
      long book = item.path("bookQty").asLong();
      var line =
          counts
              .withArray("items")
              .addObject()
              .put("id", item.path("id").asText())
              .put("actualQty", book == 0 ? 0 : book - 1);
      if (book > 0) line.put("reasonType", "LOSS").put("reasonNote", "实物破损损耗一瓶");
    }
    stocktake = call("PUT", "/stocktakes/" + task + "/counts", counts, "buyer01");
    stocktake = call("POST", "/stocktakes/" + task + "/submit", version(stocktake), "buyer01");
    call("POST", "/stocktakes/" + task + "/approve", version(stocktake), "admin");
    assertThat(
            jdbc.queryForObject(
                """
        SELECT COUNT(*) FROM stock_movement m
        JOIN stocktake_item i ON i.id=m.stocktake_item_id
        JOIN stocktake_order o ON o.id=i.stocktake_id
        WHERE o.id=? AND m.occurred_at<>o.completed_at
        """,
                Long.class,
                task))
        .as("One approved stocktake has one business timestamp")
        .isZero();
    balance(product, 7);
    complete(product, 2);
    balance(product, 5);
    assertThat(
            call("GET", "/inventory/warnings?type=LOW&keyword=" + marker, null, "buyer01")
                .path("total")
                .asLong())
        .isEqualTo(1);
    var stats =
        call(
            "GET",
            "/statistics/sales?productId=" + product + "&year=" + LocalDate.now(clock).getYear(),
            null,
            "admin");
    assertThat(stats.path("totals").path("netQty").asLong()).isEqualTo(14);
    assertThat(stats.path("totals").path("netAmount").asText()).isEqualTo("70.00");
    String key = UUID.randomUUID().toString();
    var preview =
        call(
            "POST",
            "/suggestions/preview",
            json.createObjectNode()
                .put("requestKey", key)
                .put("type", "INVENTORY")
                .put("productId", product),
            "buyer01");
    assertThat(preview.path("summary").path("periodNetQty").asLong()).isEqualTo(14);
    assertThat(preview.path("summary").path("currentInventory").path("bookQty").asLong())
        .isEqualTo(5);
    assertThat(preview.path("summary").toString())
        .doesNotContain("contactPhone", "unitCost", "cashierId");
    var unavailable =
        call(
            "POST",
            "/suggestions/by-request/" + key + "/generate",
            json.createObjectNode(),
            "buyer01");
    track("AI", unavailable.path("record").path("id").asText());
    assertThat(unavailable.path("errorCode").asText()).isEqualTo("NOT_CONFIGURED");
    receipt(product, 10); // A real approval succeeds after the model failure.
    balance(product, 15);
    assertThat(
            call("GET", "/inventory/warnings?type=LOW&keyword=" + marker, null, "buyer01")
                .path("total")
                .asLong())
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT frozen_stocktake_id FROM product WHERE id=?", Long.class, product))
        .isNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_movement WHERE product_id=? AND event_type='STOCKTAKE_LOSS'",
                Long.class,
                product))
        .isEqualTo(1);
  }

  @AfterEach
  void cleanupOnlyDedicatedFixtures() {
    // Teardown is restricted to IDs and unique codes created by this test, never seed products.
    // Clearing a fixture freeze also makes cleanup possible when an assertion fails mid-task.
    for (String suffix : List.of("A"))
      for (var row : jdbc.queryForList("SELECT id FROM product WHERE code=?", marker + suffix)) {
        String id = row.get("id").toString();
        if (!products.contains(id)) products.add(id);
      }
    for (String id : products) {
      var sales =
          jdbc.queryForList("SELECT sale_id FROM sale_item WHERE product_id=?", Long.class, id);
      var receipts =
          jdbc.queryForList(
              "SELECT DISTINCT purchase_id FROM purchase_item WHERE product_id=?", Long.class, id);
      var tasks =
          jdbc.queryForList(
              "SELECT DISTINCT stocktake_id FROM stocktake_item WHERE product_id=?",
              Long.class,
              id);
      jdbc.update("UPDATE product SET frozen_stocktake_id=NULL WHERE id=?", id);
      jdbc.update("DELETE FROM stock_movement WHERE product_id=?", id);
      jdbc.update("DELETE FROM sale_allocation WHERE product_id=?", id);
      jdbc.update("DELETE FROM sale_item WHERE product_id=?", id);
      for (var sale : sales) jdbc.update("DELETE FROM sale_order WHERE id=?", sale);
      jdbc.update("DELETE FROM stocktake_item WHERE product_id=?", id);
      for (var task : tasks) jdbc.update("DELETE FROM stocktake_order WHERE id=?", task);
      jdbc.update("DELETE FROM purchase_item WHERE product_id=?", id);
      for (var receipt : receipts) jdbc.update("DELETE FROM purchase_order WHERE id=?", receipt);
      jdbc.update("DELETE FROM product WHERE id=?", id);
    }
    if (supplier != null) jdbc.update("DELETE FROM supplier WHERE id=?", supplier);
    if (category != null) jdbc.update("DELETE FROM category WHERE id=?", category);
    for (var id : targets.getOrDefault("AI", List.of()))
      jdbc.update("DELETE FROM ai_suggestion WHERE id=?", id);
    for (var entry : targets.entrySet())
      for (var id : entry.getValue())
        jdbc.update(
            "DELETE FROM operation_log WHERE target_type=? AND target_id=?", entry.getKey(), id);
  }
}
