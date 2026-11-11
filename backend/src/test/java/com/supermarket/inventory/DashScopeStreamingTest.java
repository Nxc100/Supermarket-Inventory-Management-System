package com.supermarket.inventory;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.supermarket.inventory.ai.client.*;
import com.supermarket.inventory.ai.enums.SuggestionType;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;

/** Local provider simulation, never a real call or real-success database record. */
class DashScopeStreamingTest {
  HttpServer server;
  ExecutorService workers;
  volatile String mode = "OK";
  CountDownLatch first = new CountDownLatch(1), release = new CountDownLatch(1);
  AtomicReference<String> request = new AtomicReference<>();
  AtomicInteger calls = new AtomicInteger();

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    workers = Executors.newFixedThreadPool(2);
    server.setExecutor(workers);
    server.createContext(
        "/stream",
        e -> {
          calls.incrementAndGet();
          request.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          e.getResponseHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
          e.sendResponseHeaders(200, 0);
          try {
            byte[] chunk =
                "data: {\"id\":\"provider-test\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"### 库存现象\\n\"}}]}\r\n\r\n"
                    .getBytes(StandardCharsets.UTF_8);
            for (byte b : chunk) {
              e.getResponseBody().write(b);
              e.getResponseBody().flush();
            }
            first.countDown();
            release.await(3, TimeUnit.SECONDS);
            if (mode.equals("STALL")) {
              Thread.sleep(2500);
              return;
            }
            if (mode.equals("BROKEN")) return;
            String end =
                "data: {\"choices\":[{\"delta\":{\"content\":\"请**人工核查**。\"},\"finish_reason\":\"stop\"}]}\n\n";
            if (!mode.equals("UNKNOWN"))
              end +=
                  "data: {\"id\":\"provider-test\",\"choices\":[],\"usage\":{\"prompt_tokens\":20,\"completion_tokens\":10,\"total_tokens\":30,\"prompt_tokens_details\":{\"cached_tokens\":5}}}\n\n";
            end += "data: [DONE]\n\n";
            e.getResponseBody().write(end.getBytes(StandardCharsets.UTF_8));
            e.getResponseBody().flush();
          } catch (Exception ignored) {
          } finally {
            e.close();
          }
        });
    server.start();
  }

  @AfterEach
  void stop() throws Exception {
    release.countDown();
    server.stop(0);
    workers.shutdownNow();
    assertThat(workers.awaitTermination(4, TimeUnit.SECONDS)).isTrue();
  }

  DashScopeClient client(int seconds) {
    return new DashScopeClient(
        new ObjectMapper(),
        "not-a-real-key",
        "",
        "qwen-plus",
        "http://127.0.0.1:" + server.getAddress().getPort() + "/stream",
        seconds);
  }

  @Test
  void forwardsUtf8BeforeCompletionAndParsesUsageOnlyChunk() throws Exception {
    var pieces = new CopyOnWriteArrayList<String>();
    var seen = new CountDownLatch(1);
    var future =
        CompletableFuture.supplyAsync(
            () ->
                client(3).stream(
                    SuggestionType.INVENTORY,
                    "{}",
                    "qwen-flash",
                    s -> {
                      pieces.add(s);
                      seen.countDown();
                    }));
    assertThat(seen.await(2, TimeUnit.SECONDS)).isTrue();
    assertThat(future.isDone()).isFalse();
    assertThat(pieces.get(0)).contains("库存现象");
    release.countDown();
    var result = future.get(3, TimeUnit.SECONDS);
    assertThat(result.content()).contains("人工核查");
    assertThat(result.identity().model()).isEqualTo("qwen-flash");
    assertThat(result.usage()).isEqualTo(new TokenUsage(20L, 10L, 30L, 5L, "provider-test"));
    var payload = new ObjectMapper().readTree(request.get());
    assertThat(payload.path("stream").asBoolean()).isTrue();
    assertThat(payload.path("stream_options").path("include_usage").asBoolean()).isTrue();
    assertThat(calls.get()).isEqualTo(1);
  }

  @Test
  void missingUsageIsUnknownAndMissingDoneIsFailure() {
    release.countDown();
    mode = "UNKNOWN";
    assertThat(
            client(2).stream(SuggestionType.SALES, "{}", "qwen-plus", s -> {}).usage().reported())
        .isFalse();
    mode = "BROKEN";
    assertThatThrownBy(() -> client(2).stream(SuggestionType.SALES, "{}", "qwen-plus", s -> {}))
        .isInstanceOfSatisfying(
            SuggestionFailure.class, e -> assertThat(e.code()).isEqualTo("STREAM_INTERRUPTED"));
    assertThat(calls.get()).isEqualTo(2);
  }

  @Test
  void deadlineIncludesStalledBody() {
    mode = "STALL";
    release.countDown();
    long start = System.nanoTime();
    assertThatThrownBy(() -> client(1).stream(SuggestionType.INVENTORY, "{}", "qwen-plus", s -> {}))
        .isInstanceOfSatisfying(
            SuggestionFailure.class, e -> assertThat(e.code()).isEqualTo("TIMEOUT"));
    assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(2000);
    assertThat(calls.get()).isEqualTo(1);
  }
}
