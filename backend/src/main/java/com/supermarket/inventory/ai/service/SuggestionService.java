package com.supermarket.inventory.ai.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.supermarket.inventory.ai.client.*;
import com.supermarket.inventory.ai.dto.*;
import com.supermarket.inventory.ai.entity.*;
import com.supermarket.inventory.ai.enums.*;
import com.supermarket.inventory.ai.mapper.SuggestionMapper;
import com.supermarket.inventory.ai.vo.*;
import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class SuggestionService {
  public List<com.supermarket.inventory.catalog.vo.CategoryOptionVO> categoryOptions(
      CurrentUser user) {
    access.allowed(user);
    return mapper.categoryOptions();
  }

  public PageResult<SuggestionProductOptionVO> productOptions(
      SuggestionProductQuery query, CurrentUser user) {
    access.require(user, query.getType());
    if (query.getProductId() != null) Identifiers.parse(query.getProductId());
    if (query.getCategoryId() != null) Identifiers.parse(query.getCategoryId());
    return new PageResult<>(
        mapper.productOptions(query),
        mapper.productOptionCount(query),
        query.getPageNum(),
        query.getPageSize());
  }

  private final SuggestionAccess access;
  private final SuggestionMapper mapper;
  private final SuggestionSummaryBuilder builder;
  private final SuggestionTransactions transactions;
  private final SuggestionClient client;
  private final ObjectMapper json;
  private final Clock clock;
  private final Map<String, PreparedSuggestion> previews = new HashMap<>();
  private final Semaphore slots = new Semaphore(4);
  private final AiSettingsService settings;

  public SuggestionService(
      SuggestionAccess access,
      SuggestionMapper mapper,
      SuggestionSummaryBuilder builder,
      SuggestionTransactions transactions,
      SuggestionClient client,
      ObjectMapper json,
      Clock clock,
      AiSettingsService settings) {
    this.access = access;
    this.mapper = mapper;
    this.builder = builder;
    this.transactions = transactions;
    this.client = client;
    this.json = json;
    this.clock = clock;
    this.settings = settings;
  }

  public record Context(
      LocalDate startDate,
      LocalDate endDate,
      List<SuggestionType> types,
      String provider,
      String model,
      int waitTimeoutMs,
      List<String> models) {}

  public Context context(CurrentUser user) {
    var today = LocalDate.now(clock);
    return new Context(
        today.minusDays(29),
        today,
        access.allowed(user),
        client.identity().provider(),
        client.identity().model(),
        20000,
        settings.publicSettings().models());
  }

  private SuggestionScope scope(SuggestionPreviewRequest r) {
    if (r.productId() != null) Identifiers.parse(r.productId());
    if (r.categoryId() != null) Identifiers.parse(r.categoryId());
    var end = r.endDate() == null ? LocalDate.now(clock) : r.endDate();
    var start = r.startDate() == null ? end.minusDays(29) : r.startDate();
    if (start.getYear() < 1
        || end.getYear() > 9998
        || end.isBefore(start)
        || ChronoUnit.DAYS.between(start, end) > 3649) throw bad("请选择有效的1～3650天分析区间");
    return new SuggestionScope(r.type(), start, end, r.categoryId(), r.productId());
  }

  public SuggestionPreviewVO preview(SuggestionPreviewRequest request, CurrentUser user) {
    access.require(user, request.type());
    var scope = scope(request);
    String selectedModel = settings.choose(request.model());
    var now = LocalDateTime.now(clock);
    synchronized (previews) {
      previews.values().removeIf(p -> !p.preview().expiresAt().isAfter(now));
      var existing = previews.get(request.requestKey());
      if (existing != null) {
        if (existing.ownerId() != user.id()
            || !existing.scope().equals(scope)
            || !existing.preview().model().equals(selectedModel))
          throw BusinessException.conflict("本次摘要标识已被使用，请重新采集摘要");
        return existing.preview();
      }
    }
    if (mapper.byKey(request.requestKey()) != null)
      throw BusinessException.conflict("本次生成标识已有记录，请查看原结果或重新采集摘要");
    var summary = builder.build(scope);
    var identity = client.identity();
    var result =
        new SuggestionPreviewVO(
            request.requestKey(),
            scope.type(),
            scope.start(),
            scope.end(),
            scope.categoryId(),
            scope.productId(),
            now,
            now.plusMinutes(10),
            identity.provider(),
            selectedModel,
            summary);
    Map<String, String> filters = new LinkedHashMap<>();
    if (scope.categoryId() != null) filters.put("categoryId", scope.categoryId());
    if (scope.productId() != null) filters.put("productId", scope.productId());
    try {
      var prepared =
          new PreparedSuggestion(
              user.id(),
              scope,
              result,
              json.writeValueAsString(filters),
              json.writeValueAsString(summary));
      synchronized (previews) {
        var existing = previews.get(request.requestKey());
        if (existing != null) {
          if (existing.ownerId() != user.id()
              || !existing.scope().equals(scope)
              || !existing.preview().model().equals(selectedModel))
            throw BusinessException.conflict("本次摘要标识已被使用，请重新采集摘要");
          return existing.preview();
        }
        if (previews.size() >= 256)
          throw new BusinessException(
              HttpStatus.SERVICE_UNAVAILABLE, "PREVIEW_BUSY", "当前待确认摘要较多，请稍后重试");
        previews.put(request.requestKey(), prepared);
      }
      return result;
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Summary serialization failed", e);
    }
  }

  public SuggestionDetailVO generate(String key, CurrentUser user) {
    return generate(key, user, delta -> {});
  }

  public void checkGenerate(String key, CurrentUser user) {
    key(key);
    access.allowed(user);
    var record = mapper.byKey(key);
    if (record != null) {
      access.own(user, record);
      return;
    }
    synchronized (previews) {
      var p = previews.get(key);
      if (p == null || p.ownerId() != user.id())
        throw BusinessException.conflict("摘要不存在或已失效，请重新采集");
      if (!p.preview().expiresAt().isAfter(LocalDateTime.now(clock)))
        throw new BusinessException(HttpStatus.CONFLICT, "PREVIEW_EXPIRED", "摘要已超过10分钟，请重新采集并核对");
      access.require(user, p.scope().type());
    }
  }

  public SuggestionDetailVO generate(
      String key, CurrentUser user, java.util.function.Consumer<String> delta) {
    key(key);
    access.allowed(user);
    var existing = mapper.byKey(key);
    if (existing != null) {
      access.own(user, existing);
      return detailRecord(expire(existing, user));
    }
    PreparedSuggestion prepared;
    synchronized (previews) {
      prepared = previews.get(key);
    }
    if (prepared == null || prepared.ownerId() != user.id())
      throw new BusinessException(HttpStatus.CONFLICT, "PREVIEW_EXPIRED", "摘要不存在或已失效，请重新查看数据摘要后生成");
    access.require(user, prepared.scope().type());
    if (!prepared.preview().expiresAt().isAfter(LocalDateTime.now(clock)))
      throw new BusinessException(HttpStatus.CONFLICT, "PREVIEW_EXPIRED", "摘要已超过10分钟，请重新采集并核对");
    SuggestionRecord record;
    try {
      var identity = client.identity();
      record =
          transactions.begin(
              prepared,
              user,
              new SuggestionClient.Identity(
                  identity.provider(), prepared.preview().model(), identity.fixture()));
    } catch (DuplicateKeyException e) {
      var original = mapper.byKey(key);
      access.own(user, original);
      return detailRecord(original);
    }
    synchronized (previews) {
      previews.remove(key);
    }
    long started = System.nanoTime();
    SuggestionClient.Result result = null;
    SuggestionFailure failure = null;
    boolean acquired = slots.tryAcquire();
    try {
      if (!acquired) throw new SuggestionFailure("BUSY", "当前智能调用较多，请稍后重新采集摘要生成");
      result =
          client.stream(
              prepared.scope().type(), prepared.summaryJson(), prepared.preview().model(), delta);
      if (result == null || result.content() == null || result.content().isBlank())
        throw new SuggestionFailure("EMPTY_RESULT", "模型未返回建议正文，请稍后重试");
      if (result.identity() == null || result.content().length() > 40000)
        throw new SuggestionFailure("INVALID_RESPONSE", "模型返回格式异常，请稍后重试");
    } catch (SuggestionFailure e) {
      failure = e;
    } catch (RuntimeException e) {
      failure = new SuggestionFailure("MODEL_UNAVAILABLE", "智能服务暂不可用，请稍后重试");
    } finally {
      if (acquired) slots.release();
    }
    int duration = (int) Math.min(Integer.MAX_VALUE, (System.nanoTime() - started) / 1000000);
    return detailRecord(transactions.finish(record, user, result, failure, duration));
  }

  public PageResult<SuggestionSummaryVO> page(SuggestionQuery query, CurrentUser user) {
    var types = access.allowed(user);
    if (query.getType() != null) access.require(user, query.getType());
    return new PageResult<>(
        mapper.page(query, user, types),
        mapper.count(query, user, types),
        query.getPageNum(),
        query.getPageSize());
  }

  public SuggestionDetailVO detail(long id, CurrentUser user) {
    var record = mapper.find(id);
    access.read(user, record);
    return detailRecord(expire(record, user));
  }

  public SuggestionDetailVO byKey(String key, CurrentUser user) {
    key(key);
    var record = mapper.byKey(key);
    access.own(user, record);
    return detailRecord(expire(record, user));
  }

  private SuggestionRecord expire(SuggestionRecord record, CurrentUser user) {
    if (record.status() == SuggestionStatus.PENDING
        && record.requestedAt().plusSeconds(45).isBefore(LocalDateTime.now(clock)))
      return transactions.finish(
          record,
          user,
          null,
          new SuggestionFailure("INTERRUPTED", "上次调用未能完成，请重新采集摘要生成；基础进销存不受影响"),
          45000);
    return record;
  }

  private SuggestionDetailVO detailRecord(SuggestionRecord r) {
    try {
      return new SuggestionDetailVO(
          new SuggestionSummaryVO(
              r.id().toString(),
              r.suggestionType(),
              r.operatorName(),
              r.periodStart(),
              r.periodEnd(),
              r.snapshotAt(),
              r.provider(),
              r.modelName(),
              r.isFixture(),
              r.status(),
              r.requestedAt(),
              r.finishedAt(),
              r.errorMessage()),
          safeIds(json.readTree(r.filterData())),
          safeIds(json.readTree(r.inputSummary())),
          r.content(),
          r.errorCode(),
          r.durationMs(),
          r.status() == SuggestionStatus.PENDING,
          new TokenUsage(
              r.promptTokens(),
              r.completionTokens(),
              r.totalTokens(),
              r.cachedTokens(),
              r.providerRequestId()));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Stored summary is invalid", e);
    }
  }

  private JsonNode safeIds(JsonNode tree) {
    if (tree instanceof ObjectNode object) {
      var fields = new ArrayList<String>();
      object.fieldNames().forEachRemaining(fields::add);
      for (String field : fields) {
        var value = object.get(field);
        if (field.endsWith("Id") && value.isNumber()) object.put(field, value.asText());
        else if (field.endsWith("Ids") && value instanceof ArrayNode array) {
          for (int i = 0; i < array.size(); i++)
            if (array.get(i).isNumber()) array.set(i, TextNode.valueOf(array.get(i).asText()));
        } else safeIds(value);
      }
    } else if (tree instanceof ArrayNode array) array.forEach(this::safeIds);
    return tree;
  }

  private void key(String key) {
    if (!key.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"))
      throw bad("生成标识格式不合法");
  }

  private BusinessException bad(String message) {
    return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
  }
}
