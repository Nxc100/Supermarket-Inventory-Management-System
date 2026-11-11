package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Faults affect only connections created here, in the guarded integration-test database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatabaseConnectionRecoveryIntegrationTest {
  @Autowired HikariDataSource source;

  private HikariDataSource isolatedPool() throws SQLException {
    try (var guard = source.getConnection()) {
      assertThat(guard.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
    }
    var config = new HikariConfig();
    source.copyStateTo(config);
    config.setPoolName("connection-recovery-" + UUID.randomUUID());
    config.setMinimumIdle(0);
    config.setMaximumPoolSize(1);
    return new HikariDataSource(config);
  }

  private long number(Connection connection, String sql) throws SQLException {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      assertThat(result.next()).isTrue();
      return result.getLong(1);
    }
  }

  @Test
  void serverExpiredIdleConnectionIsReplacedWithInitializedSession() throws Exception {
    try (var pool = isolatedPool();
        var observer = source.getConnection()) {
      long expiredId;
      try (var connection = pool.getConnection()) {
        expiredId = number(connection, "SELECT CONNECTION_ID()");
        try (var statement = connection.createStatement()) {
          statement.execute("SET SESSION wait_timeout=1");
        }
      }
      // Observe the actual server-side disconnect, rather than assuming a sleep is long enough.
      await()
          .atMost(Duration.ofSeconds(8))
          .pollInterval(Duration.ofMillis(100))
          .until(
              () ->
                  number(
                          observer,
                          "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE ID="
                              + expiredId)
                      == 0);
      try (var replacement = pool.getConnection()) {
        assertThat(number(replacement, "SELECT CONNECTION_ID()")).isNotEqualTo(expiredId);
        assertThat(number(replacement, "SELECT @@session.wait_timeout")).isEqualTo(120);
        assertThat(number(replacement, "SELECT 1")).isEqualTo(1);
      }
    }
  }

  @Test
  void disconnectedTransactionRollsBackAndBrokenConnectionIsNotReused() throws Exception {
    String key = "qa.connection." + UUID.randomUUID();
    try (var pool = isolatedPool();
        var control = source.getConnection()) {
      long disconnectedId;
      try (var connection = pool.getConnection()) {
        disconnectedId = number(connection, "SELECT CONNECTION_ID()");
        connection.setAutoCommit(false);
        try (var insert =
            connection.prepareStatement(
                "INSERT INTO sys_config(config_key,config_value,description,editable) VALUES (?, '1', 'Connection rollback fixture', 0)")) {
          insert.setString(1, key);
          assertThat(insert.executeUpdate()).isEqualTo(1);
        }
        try (var statement = control.createStatement()) {
          statement.execute("KILL CONNECTION " + disconnectedId);
        }
        assertThatThrownBy(() -> number(connection, "SELECT 1")).isInstanceOf(SQLException.class);
      }
      try (var replacement = pool.getConnection()) {
        assertThat(number(replacement, "SELECT CONNECTION_ID()")).isNotEqualTo(disconnectedId);
        assertThat(replacement.getAutoCommit()).isTrue();
        assertThat(replacement.isReadOnly()).isFalse();
        try (var query =
            replacement.prepareStatement("SELECT COUNT(*) FROM sys_config WHERE config_key=?")) {
          query.setString(1, key);
          try (var result = query.executeQuery()) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isZero();
          }
        }
      }
    }
  }
}
