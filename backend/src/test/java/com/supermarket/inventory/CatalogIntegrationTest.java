package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
class CatalogIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource dataSource;
  String admin;

  @BeforeEach
  void isolated() throws Exception {
    try (var c = dataSource.getConnection()) {
      assertThat(c.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
    admin = login("admin");
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

  ObjectNode product(String code) {
    ObjectNode p = json.createObjectNode();
    p.put("code", code);
    p.putNull("barcode");
    p.put("name", "商品验证" + code);
    p.put("categoryId", "1");
    p.put("unit", "瓶");
    p.put("salePrice", "5.00");
    p.put("referenceCost", "3.00");
    p.put("managesExpiry", false);
    p.putNull("shelfLifeDays");
    p.put("nearExpiryDays", 0);
    p.put("stockLowerLimit", 0);
    p.put("canPurchase", true);
    p.put("canSell", true);
    return p;
  }

  ResultActions create(ObjectNode p) throws Exception {
    return mvc.perform(
        post("/api/v1/products")
            .header("Authorization", "Bearer " + admin)
            .contentType("application/json")
            .content(p.toString()));
  }

  ResultActions update(String id, ObjectNode p) throws Exception {
    return mvc.perform(
        patch("/api/v1/products/" + id)
            .header("Authorization", "Bearer " + admin)
            .contentType("application/json")
            .content(p.toString()));
  }

  ObjectNode fromExisting(String id) throws Exception {
    JsonNode vo =
        data(
            mvc.perform(get("/api/v1/products/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()));
    ObjectNode input = json.createObjectNode();
    for (String field :
        List.of(
            "code",
            "barcode",
            "name",
            "categoryId",
            "specification",
            "unit",
            "referenceCost",
            "salePrice",
            "managesExpiry",
            "shelfLifeDays",
            "nearExpiryDays",
            "stockLowerLimit",
            "canPurchase",
            "canSell",
            "remark")) input.set(field, vo.get(field));
    return input;
  }

  @Test
  void categoryRenameKeepsPurchaseAndSaleCategorySnapshots() throws Exception {
    var purchases =
        jdbc.queryForList(
            "SELECT category_name FROM purchase_item WHERE category_id=1 ORDER BY id");
    var sales =
        jdbc.queryForList("SELECT category_name FROM sale_item WHERE category_id=1 ORDER BY id");
    mvc.perform(
            patch("/api/v1/categories/1")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content("{\"code\":\"C001\",\"name\":\"分类当前更名验证\",\"sortOrder\":1}"))
        .andExpect(status().isOk());
    assertThat(
            jdbc.queryForList(
                "SELECT category_name FROM purchase_item WHERE category_id=1 ORDER BY id"))
        .isEqualTo(purchases);
    assertThat(
            jdbc.queryForList(
                "SELECT category_name FROM sale_item WHERE category_id=1 ORDER BY id"))
        .isEqualTo(sales);
  }

  @Test
  void chineseSearchWorksAcrossAsciiCodesAndUtf8Names() throws Exception {
    mvc.perform(
            get("/api/v1/categories")
                .param("keyword", "饮料")
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1));
    mvc.perform(
            get("/api/v1/products")
                .param("keyword", "纯牛奶")
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1));
    mvc.perform(
            get("/api/v1/product-lookups/sale")
                .param("keyword", "纯牛奶")
                .header("Authorization", "Bearer " + login("cashier01")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1));
  }

  @Test
  void newProductStockIsZeroAndCannotBeForged() throws Exception {
    ObjectNode p = product("QA_NEW");
    p.put("stockQty", 99);
    create(p).andExpect(status().isBadRequest());
    p.remove("stockQty");
    JsonNode saved =
        data(
            create(p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQty").value(0))
                .andExpect(jsonPath("$.data.salePrice").value("5.00")));
    String id = saved.path("id").asText();
    assertThat(saved.path("barcode").isNull()).isTrue();
    assertThat(jdbc.queryForObject("SELECT stock_qty FROM product WHERE id=?", Long.class, id))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_movement WHERE product_id=?", Long.class, id))
        .isZero();
    mvc.perform(delete("/api/v1/products/" + id).header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk());
  }

  @Test
  void disabledCategoryCannotBeSelectedAndDoesNotDisableExistingProducts() throws Exception {
    jdbc.update("UPDATE category SET enabled=0 WHERE id=1");
    create(product("QA_DISABLED_CATEGORY")).andExpect(status().isConflict());
    ObjectNode existing = fromExisting("1");
    existing.put("name", "分类停用后可维护原商品");
    update("1", existing).andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT enabled FROM product WHERE id=1", Boolean.class))
        .isTrue();
  }

  @Test
  void referencedProductIdentityAndExpiryModeAreProtected() throws Exception {
    for (String field : List.of("code", "categoryId", "unit", "managesExpiry")) {
      ObjectNode input = fromExisting("1");
      switch (field) {
        case "code" -> input.put(field, "QA_CHANGED");
        case "categoryId" -> input.put(field, "2");
        case "unit" -> input.put(field, "件");
        default -> {
          input.put(field, false);
          input.putNull("shelfLifeDays");
        }
      }
      update("1", input).andExpect(status().isConflict());
    }
    mvc.perform(delete("/api/v1/products/1").header("Authorization", "Bearer " + admin))
        .andExpect(status().isConflict());
  }

  @Test
  void archiveEditNeverRewritesSnapshotsOrBalances() throws Exception {
    var old =
        jdbc.queryForList(
            "SELECT product_name,unit_price,category_name FROM sale_item WHERE product_id=23");
    long stock = jdbc.queryForObject("SELECT stock_qty FROM product WHERE id=23", Long.class);
    ObjectNode input = fromExisting("23");
    input.put("salePrice", "19.99");
    input.put("name", "当前档案变更");
    update("23", input).andExpect(status().isOk());
    assertThat(
            jdbc.queryForList(
                "SELECT product_name,unit_price,category_name FROM sale_item WHERE product_id=23"))
        .isEqualTo(old);
    assertThat(jdbc.queryForObject("SELECT stock_qty FROM product WHERE id=23", Long.class))
        .isEqualTo(stock);
  }

  @Test
  void uniqueBarcodeCodeAndMoneyContractAreEnforced() throws Exception {
    ObjectNode numericId = product("QA_NUMERIC_ID");
    numericId.put("categoryId", 1);
    create(numericId).andExpect(status().isBadRequest());
    ObjectNode p = product("P0001");
    create(p).andExpect(status().isConflict());
    p = product("QA_BARCODE");
    p.put("barcode", "6900000000001");
    create(p).andExpect(status().isConflict());
    p = product("QA_MONEY");
    p.put("salePrice", 5.5);
    create(p).andExpect(status().isBadRequest());
    p.put("salePrice", "5.001");
    create(p).andExpect(status().isBadRequest());
    p.put("salePrice", "0.00");
    create(p).andExpect(status().isBadRequest());
    p = product("QA_EXPIRY");
    p.put("managesExpiry", true);
    create(p).andExpect(status().isBadRequest());
    p.put("shelfLifeDays", 1);
    create(p).andExpect(status().isOk());
  }

  @Test
  void lookupsArePermissionBoundAndCashierNeverReceivesCost() throws Exception {
    String cashier = login("cashier01"), buyer = login("buyer01");
    mvc.perform(get("/api/v1/products").header("Authorization", "Bearer " + cashier))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/products/1").header("Authorization", "Bearer " + buyer))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/product-lookups/purchase").header("Authorization", "Bearer " + cashier))
        .andExpect(status().isForbidden());
    JsonNode sale =
        data(
            mvc.perform(
                    get("/api/v1/product-lookups/sale")
                        .param("keyword", "P0001")
                        .header("Authorization", "Bearer " + cashier))
                .andExpect(status().isOk()));
    JsonNode row = sale.path("records").get(0);
    assertThat(row.has("referenceCost")).isFalse();
    assertThat(row.has("unitCost")).isFalse();
    assertThat(row.has("supplierId")).isFalse();
    assertThat(row.path("salePrice").asText()).isEqualTo("5.00");
    mvc.perform(get("/api/v1/product-lookups/sale").header("Authorization", "Bearer " + buyer))
        .andExpect(status().isForbidden());
    assertThat(
            data(mvc.perform(
                        get("/api/v1/product-lookups/purchase")
                            .param("keyword", "P0014")
                            .header("Authorization", "Bearer " + buyer))
                    .andExpect(status().isOk()))
                .path("total")
                .asLong())
        .isZero();
  }

  @Test
  void supplierIsSharedButDeletionRemainsAdminOnlyAndHistoricalSnapshotsStay() throws Exception {
    String buyer = login("buyer01"), otherBuyer = login("buyer02"), cashier = login("cashier01");
    ObjectNode input =
        json.createObjectNode()
            .put("name", "验证供应商")
            .put("contactName", "验证联系人")
            .put("contactPhone", "0710-1234567")
            .put("address", "测试地址")
            .put("remark", "测试备注");
    JsonNode saved =
        data(
            mvc.perform(
                    post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + buyer)
                        .contentType("application/json")
                        .content(input.toString()))
                .andExpect(status().isOk()));
    String id = saved.path("id").asText();
    assertThat(saved.path("code").asText()).startsWith("SUP");
    mvc.perform(get("/api/v1/suppliers/" + id).header("Authorization", "Bearer " + otherBuyer))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/v1/suppliers/" + id).header("Authorization", "Bearer " + buyer))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/suppliers").header("Authorization", "Bearer " + cashier))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/v1/suppliers/1").header("Authorization", "Bearer " + admin))
        .andExpect(status().isConflict());
    var snapshot =
        jdbc.queryForList("SELECT supplier_name FROM purchase_order WHERE supplier_id=1");
    mvc.perform(
            patch("/api/v1/suppliers/1")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(input.toString().replace("验证供应商", "历史供应商当前改名")))
        .andExpect(status().isOk());
    assertThat(jdbc.queryForList("SELECT supplier_name FROM purchase_order WHERE supplier_id=1"))
        .isEqualTo(snapshot);
    mvc.perform(delete("/api/v1/suppliers/" + id).header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk());
  }

  @Test
  void supplierValidationUniqueNameAndStatusFilters() throws Exception {
    ObjectNode input =
        json.createObjectNode()
            .put("name", "唯一供应商验证")
            .put("contactName", "合法联系人")
            .put("contactPhone", "not-phone");
    mvc.perform(
            post("/api/v1/suppliers")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(input.toString()))
        .andExpect(status().isBadRequest());
    input.put("contactPhone", "13812345678");
    JsonNode saved =
        data(
            mvc.perform(
                    post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + admin)
                        .contentType("application/json")
                        .content(input.toString()))
                .andExpect(status().isOk()));
    input.put("name", " 唯一供应商验证 ");
    mvc.perform(
            post("/api/v1/suppliers")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(input.toString()))
        .andExpect(status().isConflict());
    String id = saved.path("id").asText();
    mvc.perform(
            post("/api/v1/suppliers/" + id + "/status")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content("{\"enabled\":false}"))
        .andExpect(status().isOk());
    assertThat(
            data(mvc.perform(
                        get("/api/v1/suppliers")
                            .param("keyword", "唯一供应商验证")
                            .param("enabled", "true")
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk()))
                .path("total")
                .asLong())
        .isZero();
  }
}
