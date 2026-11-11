package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.supermarket.inventory.ai.client.*;
import com.supermarket.inventory.ai.enums.SuggestionType;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;

/** Simulated HTTP responses only; these tests never persist suggestions as real API success. */
class DashScopeClientTest {
  HttpServer server;
  ExecutorService executor;
  final AtomicInteger status = new AtomicInteger(200), requests = new AtomicInteger();
  final AtomicReference<String> body =
      new AtomicReference<>("{\"choices\":[{\"message\":{\"content\":\"请人工核实补货。\"}}]}");
  volatile long delay;

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    executor = Executors.newFixedThreadPool(2);
    server.setExecutor(executor);
    server.createContext(
        "/chat",
        exchange -> {
          requests.incrementAndGet();
          try {
            exchange.getRequestBody().readAllBytes();
            if (delay > 0) Thread.sleep(delay);
            byte[] data = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status.get(), data.length);
            exchange.getResponseBody().write(data);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
  }

  @AfterEach
  void stop() throws Exception {
    server.stop(0);
    executor.shutdownNow();
    assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
  }

  DashScopeClient client(String key, int timeout) {
    return new DashScopeClient(
        new ObjectMapper(),
        key,
        "",
        "qwen-plus",
        "http://127.0.0.1:" + server.getAddress().getPort() + "/chat",
        timeout);
  }

  @Test
  void parsesUtf8TextWithoutCallingBusinessCode() {
    var result = client("unit-test-token", 2).generate(SuggestionType.INVENTORY, "{}");
    assertThat(result.content()).isEqualTo("请人工核实补货。");
    assertThat(requests.get()).isEqualTo(1);
  }

  @Test
  void missingKeyDoesNotSendARequest() {
    assertThatThrownBy(() -> client("", 2).generate(SuggestionType.INVENTORY, "{}"))
        .isInstanceOfSatisfying(
            SuggestionFailure.class, e -> assertThat(e.code()).isEqualTo("NOT_CONFIGURED"));
    assertThat(requests.get()).isZero();
  }

  @Test
  void authenticationAndRateLimitErrorsAreSanitizedAndNotRetried() {
    body.set("{\"error\":{\"message\":\"do not expose provider diagnostic or credentials\"}}");
    status.set(401);
    assertThatThrownBy(() -> client("unit-test-token", 2).generate(SuggestionType.INVENTORY, "{}"))
        .isInstanceOfSatisfying(
            SuggestionFailure.class,
            e -> {
              assertThat(e.code()).isEqualTo("AUTHENTICATION");
              assertThat(e.getMessage()).doesNotContain("provider diagnostic", "credentials");
            });
    status.set(429);
    assertThatThrownBy(() -> client("unit-test-token", 2).generate(SuggestionType.SALES, "{}"))
        .isInstanceOfSatisfying(
            SuggestionFailure.class, e -> assertThat(e.code()).isEqualTo("RATE_LIMITED"));
    assertThat(requests.get()).isEqualTo(2);
  }

  @Test
  void emptyAndMalformedResponsesFailExplicitly() {
    body.set("{\"choices\":[{\"message\":{\"content\":\" \"}}]}");
    assertThatThrownBy(() -> client("unit-test-token", 2).generate(SuggestionType.SALES, "{}"))
        .isInstanceOfSatisfying(
            SuggestionFailure.class, e -> assertThat(e.code()).isEqualTo("EMPTY_RESULT"));
    body.set("not-json");
    assertThatThrownBy(() -> client("unit-test-token", 2).generate(SuggestionType.SALES, "{}"))
        .isInstanceOfSatisfying(
            SuggestionFailure.class, e -> assertThat(e.code()).isEqualTo("INVALID_RESPONSE"));
  }

  @Test
  void independentRequestTimeoutIsBoundedAndDoesNotRetry() {
    delay = 2000;
    assertThatThrownBy(() -> client("unit-test-token", 1).generate(SuggestionType.INVENTORY, "{}"))
        .isInstanceOfSatisfying(
            SuggestionFailure.class, e -> assertThat(e.code()).isEqualTo("TIMEOUT"));
    assertThat(requests.get()).isEqualTo(1);
  }

  @Test
  void unavailableProviderDoesNotReturnItsBody() {
    status.set(503);
    body.set("private internal provider details");
    assertThatThrownBy(() -> client("unit-test-token", 2).generate(SuggestionType.INVENTORY, "{}"))
        .isInstanceOfSatisfying(
            SuggestionFailure.class,
            e -> {
              assertThat(e.code()).isEqualTo("PROVIDER_UNAVAILABLE");
              assertThat(e.getMessage()).doesNotContain("private");
            });
  }
}
