package com.supermarket.inventory.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.dto.*;
import com.supermarket.inventory.system.entity.RoleState;
import com.supermarket.inventory.system.enums.StaffRole;
import com.supermarket.inventory.system.mapper.*;
import com.supermarket.inventory.system.vo.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class RoleService {
  private static final Set<String> COMMON =
      Set.of("common", "common.dashboard", "common.profile", "common.password", "common.logout");
  private static final Set<String> ADMIN_CORE =
      Set.of(
          "system",
          "system.user",
          "system.user.create",
          "system.user.edit",
          "system.user.enable",
          "system.user.reset_password",
          "system.role",
          "system.role.save",
          "system.role.restore");
  private final RoleMapper mapper;
  private final StaffMapper staff;
  private final AuditService audit;
  private final ObjectMapper json;
  private final Clock clock;

  public RoleService(
      RoleMapper mapper, StaffMapper staff, AuditService audit, ObjectMapper json, Clock clock) {
    this.mapper = mapper;
    this.staff = staff;
    this.audit = audit;
    this.json = json;
    this.clock = clock;
  }

  private void require(CurrentUser user, String action) {
    if (!"ADMIN".equals(user.roleCode())
        || !user.permissions().contains("system.role")
        || !user.permissions().contains(action))
      throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "仅已授权管理员可配置岗位权限");
  }

  private record Definition(
      String code, String label, String kind, String parent, List<String> roles) {}

  private List<Definition> catalog() {
    try {
      return json.readValue(mapper.catalog(), new TypeReference<List<Definition>>() {});
    } catch (Exception e) {
      throw new IllegalStateException("Invalid permission catalog");
    }
  }

  private List<String> codes(String input) {
    try {
      return json.readValue(input, new TypeReference<List<String>>() {});
    } catch (Exception e) {
      throw new IllegalStateException("Invalid stored permission set");
    }
  }

  private Set<String> protectedCodes(String role) {
    var result = new TreeSet<>(COMMON);
    if (role.equals("ADMIN")) result.addAll(ADMIN_CORE);
    return result;
  }

  private RoleState find(StaffRole role) {
    var result = mapper.find(role.name());
    if (result == null) throw BusinessException.notFound();
    return result;
  }

  private RoleSummaryVO summary(RoleState role) {
    var note =
        switch (role.code()) {
          case "ADMIN" -> "全店记录和审核统筹；仍受单据状态、库存冻结及历史约束。";
          case "PURCHASER" -> "本人采购、核对和盘点；共享供应商、库存、库存统计及库存成功建议。无销售和系统管理权限。";
          case "CASHIER" -> "本人销售记录和开单；精简只读库存及不含成本的全店销售聚合。无采购、审核或建议权限。";
          default -> throw new IllegalStateException("Unsupported stored role");
        };
    return new RoleSummaryVO(
        role.code(),
        role.name(),
        role.dataScope(),
        note,
        codes(role.permissionsJson()).size(),
        role.authVersion(),
        role.updatedAt());
  }

  public List<RoleSummaryVO> all(CurrentUser user) {
    require(user, "system.role");
    return mapper.all().stream().map(this::summary).toList();
  }

  private RoleDetailVO detail(RoleState role) {
    var ceiling = new HashSet<>(codes(role.ceilingJson()));
    var nodes =
        catalog().stream()
            .filter(n -> ceiling.contains(n.code()) && n.roles().contains(role.code()))
            .map(n -> new RoleDetailVO.Node(n.code(), label(n.label()), n.kind(), n.parent()))
            .toList();
    return new RoleDetailVO(
        summary(role),
        nodes,
        List.copyOf(codes(role.permissionsJson())),
        List.copyOf(protectedCodes(role.code())));
  }

  private String label(String label) {
    return switch (label) {
      case "create" -> "新增";
      case "edit" -> "编辑";
      case "enable" -> "启用 / 停用";
      case "delete" -> "删除";
      case "reset_password" -> "重置密码";
      case "delete_draft" -> "删除未提交草稿";
      case "submit" -> "提交审核";
      case "withdraw" -> "撤回提交";
      case "close" -> "关闭单据或任务";
      case "reconcile" -> "采购对账";
      case "review" -> "审核通过";
      case "reject" -> "驳回";
      case "reverse" -> "撤销入库";
      case "start" -> "开始盘点";
      case "save" -> "暂存实盘";
      default -> label;
    };
  }

  public RoleDetailVO detail(StaffRole role, CurrentUser user) {
    require(user, "system.role");
    return detail(find(role));
  }

  private List<String> validate(RoleState role, List<String> input) {
    var requested = new HashSet<>(input);
    if (requested.size() != input.size())
      throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "权限代码不能重复");
    var ceiling = new HashSet<>(codes(role.ceilingJson()));
    var definitions = new LinkedHashMap<String, Definition>();
    for (var node : catalog()) definitions.put(node.code(), node);
    for (var code : requested) {
      var definition = definitions.get(code);
      if (!ceiling.contains(code)
          || definition == null
          || !definition.roles().contains(role.code()))
        throw new BusinessException(HttpStatus.FORBIDDEN, "ROLE_CEILING", "不能授予超出该岗位上限的权限：" + code);
      if (definition.parent() != null && !requested.contains(definition.parent()))
        throw new BusinessException(
            HttpStatus.BAD_REQUEST, "PERMISSION_PARENT", "勾选页面和操作前须保留其所属模块及页面权限");
    }
    if (!requested.containsAll(protectedCodes(role.code())))
      throw BusinessException.conflict("必须保留岗位基础功能和管理员的账号/权限恢复入口，不能移除关键管理权限");
    return definitions.keySet().stream().filter(requested::contains).toList();
  }

  private RoleState locked(StaffRole role, long version) {
    if (staff.lockAdministration() == null)
      throw new IllegalStateException("Missing administrator role");
    var state = mapper.lock(role.name());
    if (state == null) throw BusinessException.notFound();
    if (state.authVersion() != version) throw BusinessException.conflict("岗位权限已被修改，请刷新权限树核对后再保存");
    return state;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RoleDetailVO save(StaffRole role, RoleSaveRequest request, CurrentUser user) {
    require(user, "system.role.save");
    var old = locked(role, request.expectedVersion());
    return apply(old, validate(old, request.permissions()), user, "ROLE_SAVE");
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RoleDetailVO restore(StaffRole role, RoleRestoreRequest request, CurrentUser user) {
    require(user, "system.role.restore");
    var old = locked(role, request.expectedVersion());
    return apply(old, validate(old, codes(old.defaultJson())), user, "ROLE_RESTORE");
  }

  private RoleDetailVO apply(
      RoleState old, List<String> permissions, CurrentUser user, String action) {
    try {
      if (mapper.save(
              old.code(),
              json.writeValueAsString(permissions),
              old.authVersion(),
              LocalDateTime.now(clock))
          != 1) throw BusinessException.conflict("岗位权限版本已变化，请刷新");
      audit.snapshot(
          user,
          action,
          "ROLE",
          old.code(),
          old.code(),
          "岗位授权版本已更新，相关账号原登录凭证失效",
          Map.of("role", old.code(), "permissions", codes(old.permissionsJson())),
          Map.of("role", old.code(), "permissions", permissions));
      return detail(mapper.find(old.code()));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Permission serialization failed");
    }
  }
}
