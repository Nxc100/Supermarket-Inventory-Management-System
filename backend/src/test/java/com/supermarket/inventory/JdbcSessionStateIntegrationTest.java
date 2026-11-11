package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;

import java.sql.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** JDBC session-state optimizations must preserve the server's actual transaction modes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JdbcSessionStateIntegrationTest {
  @Autowired DataSource source;

  private String scalar(Connection connection, String sql) throws SQLException {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      assertThat(result.next()).isTrue();
      return result.getString(1);
    }
  }

  @Test
  void readOnlyIsolationAndRollbackRemainCorrectAcrossModesOnTheSamePooledConnection()
      throws Exception {
    try (var connection = source.getConnection()) {
      assertThat(connection.getCatalog()).startsWith("supermarket_inventory_").endsWith("test");
      assertThat(connection.getAutoCommit()).isTrue();
      assertThat(connection.isReadOnly()).isFalse();
      int originalIsolation = connection.getTransactionIsolation();
      String original =
          scalar(
              connection,
              "SELECT CAST(config_value AS CHAR) FROM sys_config WHERE config_key='inventory.default_lower_limit'");
      try {
        connection.setReadOnly(true);
        connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        connection.setAutoCommit(false);
        assertThat(connection.isReadOnly()).isTrue();
        assertThat(scalar(connection, "SELECT @@session.transaction_read_only")).isEqualTo("1");
        assertThat(scalar(connection, "SELECT @@session.transaction_isolation"))
            .isEqualTo("REPEATABLE-READ");
        assertThat(scalar(connection, "SELECT @@session.autocommit")).isEqualTo("0");
        try (var statement = connection.createStatement()) {
          assertThatThrownBy(
                  () ->
                      statement.executeUpdate(
                          "UPDATE sys_config SET config_value=config_value WHERE config_key='inventory.default_lower_limit'"))
              .isInstanceOf(SQLException.class);
        }
        connection.rollback();
        connection.setAutoCommit(true);
        connection.setReadOnly(false);
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        connection.setAutoCommit(false);
        assertThat(connection.isReadOnly()).isFalse();
        assertThat(scalar(connection, "SELECT @@session.transaction_read_only")).isEqualTo("0");
        assertThat(scalar(connection, "SELECT @@session.transaction_isolation"))
            .isEqualTo("READ-COMMITTED");
        try (var statement = connection.createStatement()) {
          // This is a no-op write in an isolated test database, and is explicitly rolled back.
          assertThat(
                  statement.executeUpdate(
                      "UPDATE sys_config SET config_value=config_value WHERE config_key='inventory.default_lower_limit'"))
              .isEqualTo(1);
        }
        connection.rollback();
        assertThat(
                scalar(
                    connection,
                    "SELECT CAST(config_value AS CHAR) FROM sys_config WHERE config_key='inventory.default_lower_limit'"))
            .isEqualTo(original);
      } finally {
        connection.rollback();
        connection.setAutoCommit(true);
        connection.setReadOnly(false);
        connection.setTransactionIsolation(originalIsolation);
      }
    }
    // The pool must also hand the next borrower a usable, writable, auto-commit connection.
    try (var next = source.getConnection()) {
      assertThat(next.getAutoCommit()).isTrue();
      assertThat(next.isReadOnly()).isFalse();
      assertThat(scalar(next, "SELECT @@session.transaction_read_only")).isEqualTo("0");
    }
  }
}
