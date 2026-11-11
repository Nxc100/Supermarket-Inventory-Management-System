package com.supermarket.inventory.ai.controller;

import com.supermarket.inventory.ai.dto.SuggestionConfirmRequest;
import com.supermarket.inventory.ai.service.SuggestionService;
import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionStreamController {
  @Configuration
  static class Workers {
    @Bean(destroyMethod = "shutdownNow")
    ThreadPoolExecutor suggestionStreamExecutor() {
      return new ThreadPoolExecutor(
          4,
          4,
          0,
          TimeUnit.SECONDS,
          new SynchronousQueue<>(),
          r -> {
            var t = new Thread(r, "suggestion-stream");
            t.setDaemon(true);
            return t;
          },
          new ThreadPoolExecutor.AbortPolicy());
    }
  }

  private final SuggestionService service;
  private final ThreadPoolExecutor executor;

  public SuggestionStreamController(SuggestionService service, ThreadPoolExecutor executor) {
    this.service = service;
    this.executor = executor;
  }

  @PostMapping(value = "/by-request/{key}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("hasAnyAuthority('ai.inventory','ai.sales')")
  public ResponseEntity<SseEmitter> stream(
      @PathVariable String key,
      @Valid @RequestBody SuggestionConfirmRequest request,
      @AuthenticationPrincipal CurrentUser user) {
    service.checkGenerate(
        key, user); // Fail authorization/validation before committing SSE headers.
    var emitter = new SseEmitter(35000L);
    var closed = new AtomicBoolean();
    emitter.onCompletion(() -> closed.set(true));
    emitter.onTimeout(() -> closed.set(true));
    emitter.onError(e -> closed.set(true));
    java.util.function.BiConsumer<String, Object> send =
        (event, data) -> {
          if (!closed.get())
            try {
              emitter.send(SseEmitter.event().name(event).data(data));
            } catch (Exception ignored) {
              closed.set(true);
            }
        };
    try {
      executor.execute(
          () -> {
            try {
              send.accept("started", Map.of("requestKey", key));
              var result =
                  service.generate(key, user, text -> send.accept("delta", Map.of("text", text)));
              send.accept("result", result);
            } catch (BusinessException e) {
              send.accept(
                  "error",
                  Map.of(
                      "message", e.getMessage(), "status", e.status().value(), "code", e.code()));
            } catch (Exception e) {
              send.accept("error", Map.of("message", "结果暂未核实，请按原请求标识查询；不要重复生成"));
            } finally {
              if (!closed.get()) emitter.complete();
            }
          });
    } catch (RejectedExecutionException e) {
      throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI_BUSY", "当前流式调用较多，请稍后重试原请求");
    }
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("X-Accel-Buffering", "no")
        .body(emitter);
  }
}
