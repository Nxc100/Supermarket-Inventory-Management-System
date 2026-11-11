package com.supermarket.inventory.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.inventory.ai.client.SuggestionFailure;
import com.supermarket.inventory.ai.mapper.AiSettingsMapper;
import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.mapper.RoleMapper;
import com.supermarket.inventory.system.service.AuditService;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AiSettingsService {
  public enum KeyAction {
    KEEP,
    REPLACE,
    USE_LOCAL
  }

  public record SaveRequest(
      @Min(0) long expectedVersion,
      @NotBlank @Pattern(regexp = "qwen[a-zA-Z0-9.:-]{0,95}") String defaultModel,
      @NotEmpty @Size(max = 12)
          List<@NotBlank @Pattern(regexp = "qwen[a-zA-Z0-9.:-]{0,95}") String> models,
      @NotNull KeyAction keyAction,
      @Size(max = 4096) String apiKey) {
    @Override
    public String toString() {
      return "AiSettingsSave[credentials hidden]";
    }
  }

  public record SettingsVO(
      long version,
      String defaultModel,
      List<String> models,
      boolean keyConfigured,
      String keySource) {}

  public record Stored(
      long version, String defaultModel, List<String> models, String encryptedKey) {}

  public record CallConfiguration(String model, String apiKey) {
    @Override
    public String toString() {
      return "AiCallConfiguration[model=" + model + ", credentials hidden]";
    }
  }

  private final AiSettingsMapper mapper;
  private final RoleMapper roles;
  private final AuditService audit;
  private final AiSecretCipher cipher;
  private final ObjectMapper json;
  private final Clock clock;
  private final String localKey, keyFile, defaultModel;

  public AiSettingsService(
      AiSettingsMapper mapper,
      RoleMapper roles,
      AuditService audit,
      AiSecretCipher cipher,
      ObjectMapper json,
      Clock clock,
      @Value("${app.ai.api-key:}") String key,
      @Value("${app.ai.key-file:}") String file,
      @Value("${app.ai.model:qwen-plus}") String model) {
    this.mapper = mapper;
    this.roles = roles;
    this.audit = audit;
    this.cipher = cipher;
    this.json = json;
    this.clock = clock;
    localKey = key;
    keyFile = file;
    defaultModel = model;
  }

  private Stored stored() {
    String raw = mapper.settings();
    if (raw == null) {
      var models =
          new LinkedHashSet<>(List.of(defaultModel, "qwen-plus", "qwen-flash", "qwen-max"));
      return new Stored(0, defaultModel, List.copyOf(models), null);
    }
    try {
      return json.readValue(raw, Stored.class);
    } catch (Exception ignored) {
      throw new SuggestionFailure("CONFIGURATION", "模型设置格式异常，请联系管理员检查配置");
    }
  }

  private SettingsVO safe(Stored s) {
    boolean managed = s.encryptedKey() != null;
    return new SettingsVO(
        s.version(),
        s.defaultModel(),
        s.models(),
        managed || !localKey.isBlank() || !keyFile.isBlank(),
        managed ? "MANAGED" : "LOCAL");
  }

  public SettingsVO publicSettings() {
    return safe(stored());
  }

  private void require(CurrentUser u, String permission) {
    if (!"ADMIN".equals(u.roleCode()) || !u.permissions().contains(permission))
      throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "仅管理员可管理模型配置或查看用量");
  }

  public SettingsVO get(CurrentUser user) {
    require(user, "system.config");
    return safe(stored());
  }

  public String choose(String selected) {
    var s = stored();
    String model = selected == null ? s.defaultModel() : selected;
    if (!s.models().contains(model)) throw BusinessException.conflict("所选模型已不在可用列表，请刷新设置后重新采集摘要");
    return model;
  }

  public CallConfiguration call(String selected) {
    var s =
        stored(); // One snapshot, including the key, for this call. No transaction crosses HTTP.
    if (!s.models().contains(selected))
      throw new SuggestionFailure("MODEL_DISABLED", "所选模型已被管理员移除，请重新采集摘要并选择模型");
    if (s.encryptedKey() != null)
      return new CallConfiguration(selected, cipher.decrypt(s.encryptedKey()));
    if (!localKey.isBlank()) return new CallConfiguration(selected, localKey.strip());
    if (!keyFile.isBlank())
      try {
        var path = Path.of(keyFile);
        if (Files.size(path) > 4096) throw new IllegalArgumentException();
        var key = Files.readString(path, StandardCharsets.UTF_8).replace("\uFEFF", "").strip();
        if (key.isEmpty() || key.chars().anyMatch(Character::isWhitespace))
          throw new IllegalArgumentException();
        return new CallConfiguration(selected, key);
      } catch (Exception ignored) {
        throw new SuggestionFailure("CONFIGURATION", "无法读取本地模型密钥，请检查后端配置");
      }
    throw new SuggestionFailure("NOT_CONFIGURED", "未配置通义千问密钥，请联系管理员配置后重试");
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public SettingsVO save(SaveRequest r, CurrentUser user) {
    require(user, "system.config");
    require(user, "system.config.save");
    roles.lock(
        "ADMIN"); // Same management anchor as other settings; serializes first-time creation too.
    var before = stored();
    if (before.version() != r.expectedVersion())
      throw BusinessException.conflict("模型配置已由其他管理员修改，请刷新核对后保存");
    if (new HashSet<>(r.models()).size() != r.models().size()
        || !r.models().contains(r.defaultModel()))
      throw new BusinessException(
          HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "模型列表不得重复，默认模型必须在可选列表中");
    String encrypted = before.encryptedKey();
    if (r.keyAction() == KeyAction.REPLACE) {
      if (r.apiKey() == null
          || r.apiKey().isBlank()
          || r.apiKey().length() < 8
          || r.apiKey().chars().anyMatch(Character::isWhitespace))
        throw new BusinessException(
            HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "请输入有效API Key，不得包含空格或换行");
      encrypted = cipher.encrypt(r.apiKey());
    } else if (r.apiKey() != null && !r.apiKey().isEmpty())
      throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "替换密钥时请选择设置新API Key");
    if (r.keyAction() == KeyAction.USE_LOCAL) encrypted = null;
    var after =
        new Stored(before.version() + 1, r.defaultModel(), List.copyOf(r.models()), encrypted);
    try {
      mapper.save(json.writeValueAsString(after), user.id(), LocalDateTime.now(clock));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Cannot serialize model settings");
    }
    audit.snapshot(
        user,
        "AI_CONFIG_UPDATE",
        "CONFIG",
        "ai.provider_settings",
        "ai.provider_settings",
        null,
        safe(before),
        safe(after));
    return safe(after);
  }
}
