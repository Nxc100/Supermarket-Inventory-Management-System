package com.supermarket.inventory.ai.client;

import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/** Parses provider SSE, including usage-only chunks. The deadline includes headers and body. */
final class DashScopeStreamReader {
  private static final ScheduledExecutorService DEADLINES =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            var t = new Thread(r, "ai-stream-deadline");
            t.setDaemon(true);
            return t;
          });
  private final ObjectMapper json;
  private final Consumer<String> delta;
  private final StringBuilder content = new StringBuilder();
  private TokenUsage usage = TokenUsage.unknown();
  private String requestId;
  private boolean truncated;

  DashScopeStreamReader(ObjectMapper json, Consumer<String> delta) {
    this.json = json;
    this.delta = delta;
  }

  SuggestionClient.Result read(
      HttpClient http, HttpRequest request, int timeout, SuggestionClient.Identity identity) {
    var body = new AtomicReference<InputStream>();
    var expired = new AtomicBoolean();
    var pending = http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
    var deadline =
        DEADLINES.schedule(
            () -> {
              expired.set(true);
              pending.cancel(true);
              var input = body.get();
              if (input != null)
                try {
                  input.close();
                } catch (IOException ignored) {
                }
            },
            timeout,
            TimeUnit.SECONDS);
    try {
      var response = pending.get();
      body.set(response.body());
      if (expired.get()) throw failure("TIMEOUT", "通义千问调用超时，请稍后重新生成");
      int status = response.statusCode();
      if (status == 401 || status == 403) throw failure("AUTHENTICATION", "通义千问认证失败，请检查密钥、地域或模型授权");
      if (status == 429) throw failure("RATE_LIMITED", "通义千问请求受限，请稍后重新生成");
      if (status < 200 || status >= 300)
        throw failure("PROVIDER_UNAVAILABLE", "通义千问服务暂不可用，请检查模型授权或稍后重试");
      if (!response
          .headers()
          .firstValue("Content-Type")
          .orElse("")
          .toLowerCase(java.util.Locale.ROOT)
          .startsWith("text/event-stream")) throw failure("INVALID_RESPONSE", "模型未返回流式响应，请核对模型能力");
      try (var reader =
          new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
        var line = new StringBuilder();
        var event = new StringBuilder();
        int consumed = 0;
        boolean done = false;
        int ch;
        while ((ch = reader.read()) != -1) {
          if (expired.get()) throw failure("TIMEOUT", "通义千问调用超时，请稍后重新生成");
          if (++consumed > 500000) throw failure("INVALID_RESPONSE", "模型响应过长，请缩小分析范围");
          if (ch == '\n') {
            String text = line.toString();
            if (text.endsWith("\r")) text = text.substring(0, text.length() - 1);
            line.setLength(0);
            if (text.isEmpty()) {
              if (event.length() > 0) {
                done = chunk(event.toString());
                event.setLength(0);
                if (done) break;
              }
            } else if (text.startsWith("data:")) {
              if (event.length() > 0) event.append('\n');
              event.append(text.substring(5).stripLeading());
            }
          } else {
            line.append((char) ch);
            if (line.length() > 200000) throw failure("INVALID_RESPONSE", "模型响应格式异常");
          }
        }
        if (!done) throw failure("STREAM_INTERRUPTED", "模型输出连接中断，请核实原请求结果后重新采集");
      }
      if (truncated) throw failure("TRUNCATED", "模型输出达到长度上限，内容不完整，请缩小分析范围后重试");
      if (content.toString().isBlank()) throw failure("EMPTY_RESULT", "模型未返回建议正文，请稍后重试");
      return new SuggestionClient.Result(content.toString().strip(), identity, usage);
    } catch (SuggestionFailure e) {
      throw e.withUsage(usage);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw failure("INTERRUPTED", "本次模型调用已中断");
    } catch (Exception e) {
      if (expired.get()
          || e instanceof ExecutionException && e.getCause() instanceof HttpTimeoutException)
        throw failure("TIMEOUT", "通义千问调用超时，请稍后重新生成");
      throw failure("NETWORK", "模型连接中断，请核实原请求结果；基础进销存不受影响");
    } finally {
      deadline.cancel(false);
      pending.cancel(true);
      var input = body.get();
      if (input != null)
        try {
          input.close();
        } catch (IOException ignored) {
        }
    }
  }

  private boolean chunk(String data) {
    if (data.strip().equals("[DONE]")) return true;
    try {
      JsonNode node = json.readTree(data);
      if (node.has("error")) throw failure("PROVIDER_UNAVAILABLE", "模型流式输出失败，请检查模型授权或稍后重试");
      String id = node.path("id").asText(null);
      if (id != null && id.length() <= 128) requestId = id;
      JsonNode u = node.path("usage");
      if (u.isObject()
          && u.has("prompt_tokens")
          && u.has("completion_tokens")
          && u.has("total_tokens")) {
        long p = number(u.get("prompt_tokens")),
            c = number(u.get("completion_tokens")),
            t = number(u.get("total_tokens"));
        JsonNode cache = u.path("prompt_tokens_details").path("cached_tokens");
        Long cached = cache.isMissingNode() || cache.isNull() ? null : number(cache);
        if (t != p + c || cached != null && cached > p)
          throw failure("INVALID_RESPONSE", "模型用量数据格式异常");
        usage = new TokenUsage(p, c, t, cached, requestId);
      }
      var choices = node.path("choices");
      if (choices.isArray())
        for (JsonNode choice : choices) {
          if (choice.path("index").asInt(0) != 0) continue;
          if ("length".equals(choice.path("finish_reason").asText())) truncated = true;
          JsonNode text = choice.path("delta").path("content");
          if (text.isTextual() && !text.asText().isEmpty()) {
            content.append(text.asText());
            if (content.length() > 40000) throw failure("INVALID_RESPONSE", "模型正文过长，请缩小分析范围");
            delta.accept(text.asText());
          }
        }
      return false;
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw failure("INVALID_RESPONSE", "模型流式响应格式异常");
    }
  }

  private long number(JsonNode n) {
    if (!n.isIntegralNumber()
        || !n.canConvertToLong()
        || n.longValue() < 0
        || n.longValue() > 1000000000L) throw failure("INVALID_RESPONSE", "模型用量数据格式异常");
    return n.longValue();
  }

  private SuggestionFailure failure(String code, String message) {
    return new SuggestionFailure(code, message).withUsage(usage);
  }
}
