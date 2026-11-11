package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.security.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StaffIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource source;
  @Autowired PasswordEncoder passwords;
  @Autowired AccountMapper accounts;
  @Autowired AccountPrincipalService principals;
  @Autowired com.supermarket.inventory.purchase.service.PurchaseDraftService drafts;
  @Autowired com.supermarket.inventory.stocktake.service.StocktakeService stocktakes;
  @Autowired com.supermarket.inventory.system.service.StaffService staffCommands;
  @Autowired com.supermarket.inventory.system.service.RoleService roleCommands;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
  String admin;

  @BeforeEach
  void setup() throws Exception {
    try (var c = source.getConnection()) {
      assertThat(c.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
    admin = login("admin", "Test123456");
  }

  JsonNode data(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
  }

  ResultActions write(String method, String path, String token, JsonNode body) throws Exception {
    var request =
        request(org.springframework.http.HttpMethod.valueOf(method), path)
            .contentType("application/json")
            .content(body.toString());
    if (token != null) request.header("Authorization", "Bearer " + token);
    return mvc.perform(request);
  }

  String login(String username, String password) throws Exception {
    return data(write(
                "POST",
                "/api/v1/auth/login",
                null,
                json.createObjectNode().put("username", username).put("password", password))
            .andExpect(status().isOk()))
        .path("token")
        .asText();
  }

  ObjectNode createBody(String role) {
    return json.createObjectNode()
        .put("username", "qa_" + UUID.randomUUID().toString().replace("-", "").substring(0, 15))
        .put("realName", "账号验证员")
        .put("phone", "13800009999")
        .put("role", role)
        .put("password", "Test123456")
        .put("confirmPassword", "Test123456");
  }

  JsonNode create(String role) throws Exception {
    return data(
        write("POST", "/api/v1/system/users", admin, createBody(role)).andExpect(status().isOk()));
  }

  JsonNode staff(String id) throws Exception {
    return data(
        mvc.perform(get("/api/v1/system/users/" + id).header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk()));
  }

  ObjectNode version(JsonNode row) {
    return json.createObjectNode().put("expectedUpdatedAt", row.path("updatedAt").asText());
  }

  ObjectNode edit(JsonNode row, String role) {
    return version(row).put("realName", "更新验证员").put("phone", "13800008888").put("role", role);
  }

  ResultActions state(JsonNode row, boolean enabled) throws Exception {
    return write(
        "PATCH",
        "/api/v1/system/users/" + row.path("id").asText() + "/state",
        admin,
        version(row).put("enabled", enabled));
  }

  @Test
  void accountCreateWhitelistDuplicateAndStaleEditNeverLeakCredentials() throws Exception {
    var request = createBody("CASHIER");
    var row =
        data(write("POST", "/api/v1/system/users", admin, request).andExpect(status().isOk()));
    String id = row.path("id").asText();
    assertThat(row.toString()).doesNotContain("password", "credentialVersion", "Test123456");
    assertThat(row.path("enabled").asBoolean()).isTrue();
    assertThat(jdbc.queryForObject("SELECT avatar_path FROM sys_user WHERE id=?", String.class, id))
        .isNull();
    write("POST", "/api/v1/system/users", admin, request).andExpect(status().isConflict());
    write("POST", "/api/v1/system/users", admin, createBody("MANAGER"))
        .andExpect(status().isBadRequest());
    write("POST", "/api/v1/system/users", admin, createBody("CASHIER").put("enabled", false))
        .andExpect(status().isBadRequest());
    write(
            "POST",
            "/api/v1/system/users",
            admin,
            createBody("CASHIER").put("confirmPassword", "Wrong12345"))
        .andExpect(status().isBadRequest());
    var updated =
        data(
            write("PUT", "/api/v1/system/users/" + id, admin, edit(row, "CASHIER"))
                .andExpect(status().isOk()));
    assertThat(updated.path("realName").asText()).isEqualTo("更新验证员");
    write("PUT", "/api/v1/system/users/" + id, admin, edit(row, "CASHIER"))
        .andExpect(status().isConflict());
    write(
            "PUT",
            "/api/v1/system/users/" + id,
            admin,
            edit(updated, "CASHIER").put("username", "forged_user"))
        .andExpect(status().isBadRequest());
    var list =
        data(
            mvc.perform(
                    get("/api/v1/system/users")
                        .param("keyword", request.path("username").asText())
                        .param("role", "CASHIER")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()));
    assertThat(list.path("total").asLong()).isEqualTo(1);
    assertThat(list.toString()).doesNotContain("passwordHash", "credentialVersion", "authVersion");
    var audit =
        jdbc.queryForList(
                "SELECT before_data,after_data FROM operation_log WHERE target_type='USER' AND target_id=?",
                id)
            .toString();
    assertThat(audit).doesNotContain("13800009999", "13800008888", "Test123456").contains("****");
  }

  @Test
  void resetRoleAndDisableInvalidateOldTokensAndUnusedAccountsCanBeDeleted() throws Exception {
    var row = create("CASHIER");
    String id = row.path("id").asText(), username = row.path("username").asText();
    String token = login(username, "Test123456");
    row = staff(id);
    row =
        data(
            write(
                    "POST",
                    "/api/v1/system/users/" + id + "/reset-password",
                    admin,
                    version(row)
                        .put("password", "Updated123456")
                        .put("confirmPassword", "Updated123456"))
                .andExpect(status().isOk()));
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
    String hash =
        jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE id=?", String.class, id);
    assertThat(passwords.matches("Updated123456", hash)).isTrue();
    assertThat(
            jdbc.queryForList(
                    "SELECT reason,before_data,after_data FROM operation_log WHERE target_type='USER' AND target_id=?",
                    id)
                .toString())
        .doesNotContain("Updated123456", hash);
    token = login(username, "Updated123456");
    row = staff(id);
    row =
        data(
            write("PUT", "/api/v1/system/users/" + id, admin, edit(row, "PURCHASER"))
                .andExpect(status().isOk()));
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
    token = login(username, "Updated123456");
    row = staff(id);
    row = data(state(row, false).andExpect(status().isOk()));
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
    write(
            "POST",
            "/api/v1/auth/login",
            null,
            json.createObjectNode().put("username", username).put("password", "Updated123456"))
        .andExpect(status().isUnauthorized());
    row = data(state(row, true).andExpect(status().isOk()));
    write("DELETE", "/api/v1/system/users/" + id, admin, version(row)).andExpect(status().isOk());
    mvc.perform(get("/api/v1/system/users/" + id).header("Authorization", "Bearer " + admin))
        .andExpect(status().isNotFound());
  }

  @Test
  void unfinishedPurchasesMustBeHandledBeforeRoleChangeOrDisablingAndHistoryPreventsDeletion()
      throws Exception {
    var row = create("PURCHASER");
    String id = row.path("id").asText();
    String buyer = login(row.path("username").asText(), "Test123456");
    var purchase = json.createObjectNode().put("supplierId", "1");
    purchase
        .putArray("items")
        .addObject()
        .put("productId", "1")
        .put("quantity", 1)
        .put("unitCost", "3.00");
    var doc = data(write("POST", "/api/v1/purchases", buyer, purchase).andExpect(status().isOk()));
    row = staff(id);
    state(row, false).andExpect(status().isConflict());
    write("PUT", "/api/v1/system/users/" + id, admin, edit(row, "CASHIER"))
        .andExpect(status().isConflict());
    var tasks =
        data(
            mvc.perform(
                    get("/api/v1/system/users/" + id + "/tasks")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()));
    assertThat(tasks.path("total").asLong()).isEqualTo(1);
    assertThat(tasks.path("records").get(0).path("type").asText()).isEqualTo("PURCHASE");
    write(
            "POST",
            "/api/v1/purchases/" + doc.path("order").path("id").asText() + "/close",
            admin,
            json.createObjectNode()
                .put("expectedVersion", doc.path("order").path("rowVersion").asLong())
                .put("reason", "处理账号遗留草稿测试"))
        .andExpect(status().isOk());
    row = staff(id);
    state(row, false).andExpect(status().isOk());
    row = staff(id);
    write("DELETE", "/api/v1/system/users/" + id, admin, version(row))
        .andExpect(status().isConflict());
  }

  @Test
  void anActiveStocktakeBlocksDisablingAndCanBeClosedByTheAdministrator() throws Exception {
    var row = create("PURCHASER");
    String id = row.path("id").asText();
    String buyer = login(row.path("username").asText(), "Test123456");
    var request = json.createObjectNode().put("remark", "账号停用冻结联动测试");
    request.putArray("productIds").add("1");
    var doc = data(write("POST", "/api/v1/stocktakes", buyer, request).andExpect(status().isOk()));
    row = staff(id);
    state(row, false).andExpect(status().isConflict());
    String task = doc.path("order").path("id").asText();
    write(
            "POST",
            "/api/v1/stocktakes/" + task + "/close",
            admin,
            json.createObjectNode()
                .put("expectedVersion", doc.path("order").path("rowVersion").asLong())
                .put("reason", "关闭测试活动盘点以处理账号"))
        .andExpect(status().isOk());
    assertThat(
            jdbc.queryForObject("SELECT frozen_stocktake_id FROM product WHERE id=1", Long.class))
        .isNull();
    state(staff(id), false).andExpect(status().isOk());
  }

  @Test
  void lastAdministratorAndRoleBoundariesAreEnforcedIndependentlyOfTheUi() throws Exception {
    jdbc.update("UPDATE sys_user SET enabled=0 WHERE role_code='ADMIN' AND id<>1");
    var row = staff("1");
    write("PUT", "/api/v1/system/users/1", admin, edit(row, "PURCHASER"))
        .andExpect(status().isConflict());
    state(row, false).andExpect(status().isConflict());
    write("DELETE", "/api/v1/system/users/1", admin, version(row)).andExpect(status().isConflict());
    for (String name : List.of("buyer01", "cashier01")) {
      String token = login(name, "Test123456");
      for (String path :
          List.of("/api/v1/system/users", "/api/v1/system/users/1", "/api/v1/system/users/1/tasks"))
        mvc.perform(get(path).header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
      write("POST", "/api/v1/system/users", token, createBody("ADMIN"))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void concurrentRemovalOfTheOnlyTwoEnabledAdministratorsKeepsExactlyOne() throws Exception {
    // This committed fixture is confined to the isolated test database. Restore every existing
    // enabled administrator before deleting the two accounts created solely for this race.
    var originals =
        jdbc.queryForList(
            "SELECT id FROM sys_user WHERE role_code='ADMIN' AND enabled=1", Long.class);
    var actor = principals.principal(accounts.findByUsername("admin"));
    var own = new ArrayList<JsonNode>();
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
    try {
      own.add(create("ADMIN"));
      own.add(create("ADMIN"));
      tx.executeWithoutResult(
          status -> {
            for (var id : originals) jdbc.update("UPDATE sys_user SET enabled=0 WHERE id=?", id);
          });
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM sys_user WHERE role_code='ADMIN' AND enabled=1",
                  Long.class))
          .isEqualTo(2);
      var gate = new java.util.concurrent.CountDownLatch(1);
      var results = new ArrayList<java.util.concurrent.Future<Integer>>();
      for (var row : own)
        results.add(
            pool.submit(
                () -> {
                  gate.await();
                  try {
                    staffCommands.state(
                        Long.parseLong(row.path("id").asText()),
                        new com.supermarket.inventory.system.dto.StaffStateRequest(
                            false, java.time.OffsetDateTime.parse(row.path("updatedAt").asText())),
                        actor);
                    return 200;
                  } catch (BusinessException e) {
                    return e.status().value();
                  }
                }));
      gate.countDown();
      var statuses = new ArrayList<Integer>();
      for (var future : results)
        statuses.add(future.get(15, java.util.concurrent.TimeUnit.SECONDS));
      assertThat(statuses).containsExactlyInAnyOrder(200, 409);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM sys_user WHERE role_code='ADMIN' AND enabled=1",
                  Long.class))
          .isEqualTo(1);
    } finally {
      pool.shutdown();
      // Restore pre-existing accounts in a separate transaction even if a worker cannot finish.
      tx.executeWithoutResult(
          status -> {
            for (var id : originals) jdbc.update("UPDATE sys_user SET enabled=1 WHERE id=?", id);
          });
      if (!pool.awaitTermination(20, java.util.concurrent.TimeUnit.SECONDS)) {
        pool.shutdownNow();
        throw new IllegalStateException("Account race worker did not stop");
      }
      tx.executeWithoutResult(
          status -> {
            for (var row : own) {
              jdbc.update(
                  "DELETE FROM operation_log WHERE target_type='USER' AND target_id=?",
                  row.path("id").asText());
              jdbc.update("DELETE FROM sys_user WHERE id=?", row.path("id").asText());
            }
          });
    }
  }

  @Test
  void anAlreadyAuthenticatedRequestCannotCreateAnUnfinishedTaskAfterItsAccountIsDisabled()
      throws Exception {
    var row = create("PURCHASER");
    String username = row.path("username").asText();
    var actor = principals.principal(accounts.findByUsername(username));
    jdbc.update(
        "UPDATE sys_user SET enabled=0,credential_version=credential_version+1 WHERE id=?",
        actor.id());
    var body = json.createObjectNode().put("supplierId", "1");
    body.putArray("items")
        .addObject()
        .put("productId", "1")
        .put("quantity", 1)
        .put("unitCost", "3.00");
    var purchase =
        json.treeToValue(body, com.supermarket.inventory.purchase.dto.PurchaseSaveRequest.class);
    assertThatThrownBy(() -> drafts.create(purchase, actor))
        .isInstanceOfSatisfying(
            BusinessException.class, e -> assertThat(e.status().value()).isEqualTo(401));
    var body2 = json.createObjectNode();
    body2.putArray("productIds").add("1");
    var start =
        json.treeToValue(
            body2, com.supermarket.inventory.stocktake.dto.StocktakeStartRequest.class);
    assertThatThrownBy(() -> stocktakes.start(start, actor))
        .isInstanceOfSatisfying(
            BusinessException.class, e -> assertThat(e.status().value()).isEqualTo(401));
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM purchase_order WHERE operator_id=?", Long.class, actor.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM stocktake_order WHERE operator_id=?", Long.class, actor.id()))
        .isZero();
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void newPurchaseAndAccountDisablingSerializeInBothCommitOrders() throws Exception {
    var row = create("PURCHASER");
    long id = row.path("id").asLong();
    var owner = principals.principal(accounts.findById(id));
    var administrator = principals.principal(accounts.findByUsername("admin"));
    var body = json.createObjectNode().put("supplierId", "1");
    body.putArray("items")
        .addObject()
        .put("productId", "1")
        .put("quantity", 1)
        .put("unitCost", "3.00");
    var request =
        json.treeToValue(body, com.supermarket.inventory.purchase.dto.PurchaseSaveRequest.class);
    var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    var created = new java.util.concurrent.CountDownLatch(1);
    var commit = new java.util.concurrent.CountDownLatch(1);
    var pendingCreation =
        new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<Integer>>();
    var original = row;
    try {
      // The real create command holds the actor's shared row lock until its outer transaction
      // commits.
      var creation =
          pool.submit(
              () ->
                  tx.execute(
                      status -> {
                        var result = drafts.create(request, owner);
                        created.countDown();
                        try {
                          if (!commit.await(8, java.util.concurrent.TimeUnit.SECONDS))
                            throw new IllegalStateException(
                                "Timed out waiting to commit purchase fixture");
                        } catch (InterruptedException e) {
                          Thread.currentThread().interrupt();
                          throw new IllegalStateException(e);
                        }
                        return result;
                      }));
      assertThat(created.await(8, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      var disabling =
          pool.submit(
              () -> {
                try {
                  staffCommands.state(
                      id,
                      new com.supermarket.inventory.system.dto.StaffStateRequest(
                          false,
                          java.time.OffsetDateTime.parse(original.path("updatedAt").asText())),
                      administrator);
                  return 200;
                } catch (BusinessException e) {
                  return e.status().value();
                }
              });
      awaitUserLockWait(id);
      assertThat(disabling.isDone()).isFalse();
      commit.countDown();
      var purchase = creation.get(10, java.util.concurrent.TimeUnit.SECONDS);
      assertThat(disabling.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(409);
      assertThat(jdbc.queryForObject("SELECT enabled FROM sys_user WHERE id=?", Boolean.class, id))
          .isTrue();
      // A never-submitted draft can be removed through the normal command before the reverse race.
      drafts.delete(
          Long.parseLong(purchase.order().id()), purchase.order().rowVersion(), administrator);

      // Now disabling wins: a request authorized before it must wait and then reject its stale
      // actor.
      tx.executeWithoutResult(
          status -> {
            staffCommands.state(
                id,
                new com.supermarket.inventory.system.dto.StaffStateRequest(
                    false, java.time.OffsetDateTime.parse(original.path("updatedAt").asText())),
                administrator);
            var lateCreation =
                pool.submit(
                    () -> {
                      try {
                        drafts.create(request, owner);
                        return 200;
                      } catch (BusinessException e) {
                        return e.status().value();
                      }
                    });
            try {
              awaitUserLockWait(id);
              assertThat(lateCreation.isDone()).isFalse();
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
            // Wait only after this transaction commits and releases its exclusive actor lock.
            pendingCreation.set(lateCreation);
          });
      assertThat(pendingCreation.get().get(10, java.util.concurrent.TimeUnit.SECONDS))
          .isEqualTo(401);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM purchase_order WHERE operator_id=?", Long.class, id))
          .isZero();
    } finally {
      commit.countDown();
      pool.shutdown();
      if (!pool.awaitTermination(20, java.util.concurrent.TimeUnit.SECONDS)) {
        pool.shutdownNow();
        throw new IllegalStateException("Purchase/account race worker did not stop");
      }
      tx.executeWithoutResult(
          status -> {
            jdbc.update(
                "DELETE pi FROM purchase_item pi JOIN purchase_order po ON po.id=pi.purchase_id WHERE po.operator_id=?",
                id);
            jdbc.update("DELETE FROM purchase_order WHERE operator_id=? AND status='DRAFT'", id);
            jdbc.update(
                "DELETE FROM operation_log WHERE actor_id=? OR (target_type='USER' AND target_id=?)",
                id,
                Long.toString(id));
            jdbc.update("DELETE FROM sys_user WHERE id=?", id);
          });
    }
  }

  private void awaitUserLockWait(long id) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < deadline) {
      var waits =
          jdbc.queryForObject(
              """
          SELECT COUNT(*) FROM performance_schema.data_lock_waits w
          JOIN performance_schema.data_locks l ON l.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID
          WHERE l.OBJECT_SCHEMA=DATABASE() AND l.OBJECT_NAME='sys_user'
            AND l.INDEX_NAME='PRIMARY' AND l.LOCK_DATA=?
          """,
              Long.class,
              Long.toString(id));
      if (waits != null && waits > 0) return;
      Thread.sleep(20);
    }
    fail("Expected a real account row lock wait before releasing the winning transaction");
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void aLockedAccountDoesNotPreventAnUnrelatedRoleSave() throws Exception {
    var own = create("ADMIN");
    var administrator = principals.principal(accounts.findByUsername("admin"));
    var before =
        roleCommands.detail(
            com.supermarket.inventory.system.enums.StaffRole.PURCHASER, administrator);
    var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
    var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
    try {
      tx.executeWithoutResult(
          status -> {
            // The same lock is used by login, password changes and avatar binding.
            assertThat(accounts.lock(own.path("id").asLong())).isNotNull();
            var save =
                pool.submit(
                    () ->
                        tx.execute(
                            inner -> {
                              inner.setRollbackOnly();
                              return roleCommands.save(
                                  com.supermarket.inventory.system.enums.StaffRole.PURCHASER,
                                  new com.supermarket.inventory.system.dto.RoleSaveRequest(
                                      before.role().version(), before.permissions()),
                                  administrator);
                            }));
            try {
              assertThat(save.get(5, java.util.concurrent.TimeUnit.SECONDS).role().version())
                  .isEqualTo(before.role().version() + 1);
            } catch (Exception e) {
              throw new IllegalStateException("Account lock blocked independent role save", e);
            }
          });
      assertThat(
              roleCommands
                  .detail(com.supermarket.inventory.system.enums.StaffRole.PURCHASER, administrator)
                  .role()
                  .version())
          .isEqualTo(before.role().version());
    } finally {
      pool.shutdown();
      if (!pool.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS)) {
        pool.shutdownNow();
        throw new IllegalStateException("Role save worker did not stop");
      }
      tx.executeWithoutResult(
          status -> {
            jdbc.update(
                "DELETE FROM operation_log WHERE target_type='USER' AND target_id=?",
                own.path("id").asText());
            jdbc.update("DELETE FROM sys_user WHERE id=?", own.path("id").asText());
          });
    }
  }
}
