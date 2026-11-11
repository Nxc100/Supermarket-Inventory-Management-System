package com.supermarket.inventory.system.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.dto.*;
import com.supermarket.inventory.system.entity.ConfigValue;
import com.supermarket.inventory.system.mapper.SystemSettingsMapper;
import com.supermarket.inventory.system.vo.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class SystemSettingsService {
  private static final String LOWER = "inventory.default_lower_limit",
      NEAR = "inventory.default_near_expiry_days";
  private final SystemSettingsMapper mapper;
  private final AuditService audit;
  private final ObjectMapper json;
  private final Clock clock;

  public SystemSettingsService(
      SystemSettingsMapper mapper, AuditService audit, ObjectMapper json, Clock clock) {
    this.mapper = mapper;
    this.audit = audit;
    this.json = json;
    this.clock = clock;
  }

  private void require(CurrentUser user, String permission) {
    if (!"ADMIN".equals(user.roleCode()) || !user.permissions().contains(permission))
      throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "仅管理员可执行此系统管理操作");
  }

  private SystemDefaultsVO values(List<ConfigValue> rows) {
    var values = new HashMap<String, Long>();
    for (var r : rows) {
      if (!r.editable()) throw new IllegalStateException("Business defaults are not editable");
      try {
        values.put(r.key(), Long.parseLong(r.value()));
      } catch (NumberFormatException e) {
        throw new IllegalStateException("Invalid business default");
      }
    }
    if (!values.containsKey(LOWER) || !values.containsKey(NEAR))
      throw new IllegalStateException("Missing business defaults");
    return new SystemDefaultsVO(values.get(LOWER), Math.toIntExact(values.get(NEAR)));
  }

  public SystemDefaultsVO defaults(CurrentUser user) {
    require(user, "system.config");
    return values(mapper.defaults());
  }

  @Transactional
  public SystemDefaultsVO save(DefaultsUpdateRequest r, CurrentUser user) {
    require(user, "system.config");
    require(user, "system.config.save");
    var before = values(mapper.lockDefaults());
    if (before.stockLowerLimit() != r.expectedStockLowerLimit()
        || before.nearExpiryDays() != r.expectedNearExpiryDays())
      throw BusinessException.conflict("默认参数已由其他管理员修改，请先刷新并核对后保存");
    var after = new SystemDefaultsVO(r.stockLowerLimit(), r.nearExpiryDays());
    if (after.equals(before)) return before;
    var now = LocalDateTime.now(clock);
    if (mapper.updateDefault(LOWER, Long.toString(after.stockLowerLimit()), user.id(), now) != 1
        || mapper.updateDefault(NEAR, Integer.toString(after.nearExpiryDays()), user.id(), now)
            != 1) throw new IllegalStateException("Defaults update failed");
    audit.snapshot(
        user,
        "CONFIG_UPDATE",
        "CONFIG",
        "inventory.defaults",
        "inventory.defaults",
        null,
        before,
        after);
    return after;
  }

  public PageResult<OperationLogVO> logs(OperationLogQuery q, CurrentUser user) {
    require(user, "system.log");
    return new PageResult<>(mapper.logs(q), mapper.logCount(q), q.getPageNum(), q.getPageSize());
  }

  public List<String> actions(CurrentUser user) {
    require(user, "system.log");
    return mapper.actions();
  }

  public OperationLogDetailVO detail(long id, CurrentUser user) {
    require(user, "system.log");
    var record = mapper.log(id);
    if (record == null) throw BusinessException.notFound();
    var snapshot = mapper.snapshot(id);
    return new OperationLogDetailVO(
        record,
        safe(snapshot == null ? null : snapshot.beforeData()),
        safe(snapshot == null ? null : snapshot.afterData()));
  }

  private JsonNode safe(String input) {
    if (input == null) return NullNode.instance;
    try {
      var tree = json.readTree(input);
      redact(tree);
      return tree;
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Invalid audit snapshot");
    }
  }

  private void redact(JsonNode node) {
    if (node instanceof ObjectNode obj) {
      var names = new ArrayList<String>();
      obj.fieldNames().forEachRemaining(names::add);
      for (var name : names) {
        if (name.toLowerCase(Locale.ROOT)
            .matches(".*(password|secret|token|authorization|credential|api.?key).*"))
          obj.put(name, "[已隐藏]");
        else if ((name.equals("id") || name.endsWith("Id")) && obj.get(name).isNumber())
          obj.put(name, obj.get(name).asText());
        else if (name.endsWith("Ids") && obj.get(name) instanceof ArrayNode ids) {
          for (int i = 0; i < ids.size(); i++)
            if (ids.get(i).isNumber()) ids.set(i, TextNode.valueOf(ids.get(i).asText()));
        } else redact(obj.get(name));
      }
    } else if (node instanceof ArrayNode array) array.forEach(this::redact);
  }
}
