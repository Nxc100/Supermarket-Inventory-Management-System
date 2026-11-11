package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource source;
  @Autowired com.supermarket.inventory.sale.service.SaleCommandService saleCommands;
  @Autowired com.supermarket.inventory.security.AccountMapper accounts;
  @Autowired com.supermarket.inventory.security.AccountPrincipalService principals;

  @BeforeEach
  void isolatedDatabase() throws Exception {
    try (var connection = source.getConnection()) {
      assertThat(connection.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
  }

  JsonNode postJson(String path, String token, ObjectNode body) throws Exception {
    var request = post(path).contentType("application/json").content(body.toString());
    if (token != null) request.header("Authorization", "Bearer " + token);
    return json.readTree(
            mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("data");
  }

  String login(String name) throws Exception {
    return postJson(
            "/api/v1/auth/login",
            null,
            json.createObjectNode().put("username", name).put("password", "Test123456"))
        .path("token")
        .asText();
  }

  JsonNode dashboard(String token) throws Exception {
    return json.readTree(
            mvc.perform(
                    get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + token)
                        .param("userId", "1")
                        .param("cashierId", "1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("data");
  }

  JsonNode metric(JsonNode page, String code) {
    for (var item : page.path("metrics")) if (code.equals(item.path("code").asText())) return item;
    throw new AssertionError("Missing metric " + code);
  }

  @Test
  void purchaserTaskChangesStayOwnedAndTheLinkedListHasTheSameTotal() throws Exception {
    String buyer = login("buyer01"), other = login("buyer02");
    long before = metric(dashboard(buyer), "draftPurchases").path("value").asLong();
    long otherBefore = metric(dashboard(other), "draftPurchases").path("value").asLong();
    var body = json.createObjectNode().put("supplierId", "1").put("remark", "工作台范围测试");
    body.putArray("items")
        .addObject()
        .put("productId", "1")
        .put("quantity", 1)
        .put("unitCost", "3.00");
    postJson("/api/v1/purchases", buyer, body);
    var page = dashboard(buyer);
    var card = metric(page, "draftPurchases");
    assertThat(card.path("value").asLong()).isEqualTo(before + 1);
    assertThat(metric(dashboard(other), "draftPurchases").path("value").asLong())
        .isEqualTo(otherBefore);
    var list =
        json.readTree(
                mvc.perform(
                        get("/api/v1/purchases")
                            .param("status", card.path("query").path("status").asText())
                            .header("Authorization", "Bearer " + buyer))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("data");
    assertThat(list.path("total").asLong()).isEqualTo(before + 1);
    assertThat(page.toString())
        .doesNotContain("todaySales", "pendingStocktakes", "销售经营建议", "password", "supplierPhone");
    mvc.perform(get("/api/v1/dashboard")).andExpect(status().isUnauthorized());
  }

  @Test
  void aRealSaleAndCorrectionUpdateOnlyTheOwnersTodayCardsAndTheStoreTotal() throws Exception {
    String cashier = login("cashier01"), other = login("cashier02"), admin = login("admin");
    var before = dashboard(cashier);
    long count = metric(before, "todayCompleted").path("value").asLong();
    var amount = new BigDecimal(metric(before, "todaySales").path("value").asText());
    var otherBefore = dashboard(other);
    var allBefore = new BigDecimal(metric(dashboard(admin), "todaySales").path("value").asText());
    var body =
        json.createObjectNode()
            .put("requestKey", UUID.randomUUID().toString())
            .put("paymentMethod", "CASH")
            .put("receivedAmount", "100.00");
    body.putArray("items")
        .addObject()
        .put("productId", "1")
        .put("quantity", 1)
        .put("expectedPrice", "5.00");
    // Checkout intentionally suspends a caller transaction. Exercise its real command in this
    // rollback boundary instead, so this test never commits mutations to the seed product.
    var request =
        json.treeToValue(body, com.supermarket.inventory.sale.dto.SaleCompleteRequest.class);
    var actor = principals.principal(accounts.findByUsername("cashier01"));
    JsonNode sale =
        json.valueToTree(
            saleCommands.complete(
                request,
                com.supermarket.inventory.sale.service.SaleInput.hash(request, actor),
                actor));
    var after = dashboard(cashier);
    assertThat(metric(after, "todayCompleted").path("value").asLong()).isEqualTo(count + 1);
    assertThat(new BigDecimal(metric(after, "todaySales").path("value").asText()))
        .isEqualByComparingTo(amount.add(new BigDecimal("5.00")));
    assertThat(metric(dashboard(other), "todaySales")).isEqualTo(metric(otherBefore, "todaySales"));
    assertThat(new BigDecimal(metric(dashboard(admin), "todaySales").path("value").asText()))
        .isEqualByComparingTo(allBefore.add(new BigDecimal("5.00")));
    assertThat(after.toString())
        .doesNotContain("pendingPurchases", "LOW", "供应商", "suggestions", "cost");
    postJson(
        "/api/v1/sales/" + sale.path("order").path("id").asText() + "/correct",
        admin,
        json.createObjectNode()
            .put("expectedVersion", sale.path("order").path("rowVersion").asLong())
            .put("goodsReturned", true)
            .put("reason", "工作台测试整单误录，货物仍完整在店内"));
    assertThat(metric(dashboard(cashier), "todaySales")).isEqualTo(metric(before, "todaySales"));
    assertThat(metric(dashboard(cashier), "todayCompleted"))
        .isEqualTo(metric(before, "todayCompleted"));
  }

  @Test
  void removedPagePermissionsRemoveMetricsAndShortcutDataAtTheServer() throws Exception {
    var permissions =
        json.readTree(
            jdbc.queryForObject(
                "SELECT permissions FROM sys_role WHERE code='PURCHASER'", String.class));
    var remaining = json.createArrayNode();
    for (var p : permissions)
      if (!Set.of("inventory.warning", "ai.inventory", "purchase.order.reconcile")
          .contains(p.asText())) remaining.add(p);
    jdbc.update(
        "UPDATE sys_role SET permissions=?,auth_version=auth_version+1 WHERE code='PURCHASER'",
        remaining.toString());
    var page = dashboard(login("buyer01"));
    assertThat(page.toString())
        .doesNotContain(
            "LOW", "NEAR", "EXPIRED", "suggestions", "unreconciledPurchases", "differentPurchases");
    assertThat(metric(page, "draftPurchases")).isNotNull();
  }
}
