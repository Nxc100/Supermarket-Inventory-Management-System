package com.supermarket.inventory.system.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.common.files.ImageLifecycle;
import com.supermarket.inventory.config.CoreConfiguration;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.dto.*;
import com.supermarket.inventory.system.entity.StaffAccount;
import com.supermarket.inventory.system.enums.StaffRole;
import com.supermarket.inventory.system.mapper.StaffMapper;
import com.supermarket.inventory.system.vo.*;
import java.time.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class StaffService {
  private final StaffMapper mapper;
  private final AuditService audit;
  private final PasswordEncoder passwords;
  private final ImageLifecycle images;
  private final Clock clock;

  public StaffService(
      StaffMapper mapper,
      AuditService audit,
      PasswordEncoder passwords,
      ImageLifecycle images,
      Clock clock) {
    this.mapper = mapper;
    this.audit = audit;
    this.passwords = passwords;
    this.images = images;
    this.clock = clock;
  }

  private void require(CurrentUser user, String action) {
    if (!"ADMIN".equals(user.roleCode())
        || !user.permissions().contains("system.user")
        || !user.permissions().contains(action))
      throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "仅已授权管理员可管理账号");
  }

  public PageResult<StaffVO> page(StaffQuery q, CurrentUser user) {
    require(user, "system.user");
    return new PageResult<>(mapper.page(q), mapper.count(q), q.getPageNum(), q.getPageSize());
  }

  public StaffVO detail(long id, CurrentUser user) {
    require(user, "system.user");
    return detail(id);
  }

  private StaffVO detail(long id) {
    var found = mapper.find(id);
    if (found == null) throw BusinessException.notFound();
    return found;
  }

  public PageResult<StaffTaskVO> tasks(long id, PageQuery q, CurrentUser user) {
    require(user, "system.user");
    detail(id);
    return new PageResult<>(
        mapper.tasks(id, q),
        mapper.unfinishedPurchases(id) + mapper.activeStocktakes(id),
        q.getPageNum(),
        q.getPageSize());
  }

  private StaffAccount lock(long id, OffsetDateTime expected) {
    if (mapper.lockAdministration() == null)
      throw new IllegalStateException("Missing administrator role");
    var account = mapper.lock(id);
    if (account == null) throw BusinessException.notFound();
    if (!account
        .updatedAt()
        .equals(expected.atZoneSameInstant(CoreConfiguration.ZONE).toLocalDateTime()))
      throw BusinessException.conflict("账号资料或登录状态已更新，请刷新目标账号并核对后重试");
    return account;
  }

  private void pending(long id) {
    long purchase = mapper.unfinishedPurchases(id), stocktake = mapper.activeStocktakes(id);
    if (purchase > 0 || stocktake > 0)
      throw BusinessException.conflict(
          "该账号还有" + purchase + "张未完成采购、" + stocktake + "张活动盘点。请先通过未完成任务入口由管理员处理或关闭，再变更岗位/停用。");
  }

  private void lastAdministrator(StaffAccount account, boolean removing) {
    if (removing
        && account.enabled()
        && "ADMIN".equals(account.roleCode())
        && mapper.enabledAdminCount() <= 1)
      throw BusinessException.conflict("必须保留至少一个启用管理员，不能停用、删除或变更最后管理员岗位");
  }

  private LocalDateTime changedAt(StaffAccount account) {
    var now = LocalDateTime.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    return now.isAfter(account.updatedAt()) ? now : account.updatedAt().plusNanos(1000);
  }

  /** Explicit safe audit projection; passwords, hashes, tokens and versions never enter it. */
  private record Snapshot(
      String id,
      String username,
      String realName,
      String roleCode,
      boolean enabled,
      String phoneMasked) {}

  private Snapshot snapshot(StaffVO account) {
    String phone = account.phone();
    return new Snapshot(
        account.id(),
        account.username(),
        account.realName(),
        account.roleCode(),
        account.enabled(),
        phone == null ? null : phone.substring(0, 3) + "****" + phone.substring(7));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public StaffVO create(StaffCreateRequest request, CurrentUser user) {
    require(user, "system.user.create");
    if (mapper.lockAdministration() == null)
      throw new IllegalStateException("Missing administrator role");
    if (mapper.idByUsername(request.username()) != null)
      throw BusinessException.conflict("用户名已存在，请更换用户名后保存");
    if (mapper.create(request, passwords.encode(request.password()), LocalDateTime.now(clock)) != 1)
      throw new IllegalStateException("Account insert failed");
    var result = detail(mapper.idByUsername(request.username()));
    audit.snapshot(
        user, "USER_CREATE", "USER", result.id(), result.username(), null, null, snapshot(result));
    return result;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public StaffVO update(long id, StaffUpdateRequest request, CurrentUser user) {
    require(user, "system.user.edit");
    var old = lock(id, request.expectedUpdatedAt());
    boolean changedRole = !old.roleCode().equals(request.role().name());
    if (changedRole) {
      lastAdministrator(old, request.role() != StaffRole.ADMIN);
      pending(id);
    }
    var before = detail(id);
    if (mapper.update(id, request, changedRole ? 1 : 0, changedAt(old)) != 1)
      throw new IllegalStateException("Account update failed");
    var after = detail(id);
    audit.snapshot(
        user,
        "USER_EDIT",
        "USER",
        after.id(),
        after.username(),
        changedRole ? "岗位变更，原登录凭证已失效" : null,
        snapshot(before),
        snapshot(after));
    return after;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public StaffVO state(long id, StaffStateRequest request, CurrentUser user) {
    require(user, "system.user.enable");
    var old = lock(id, request.expectedUpdatedAt());
    if (!request.enabled() && id == user.id()) throw BusinessException.conflict("不能停用当前登录账号");
    if (old.enabled() == request.enabled()) return detail(id);
    if (!request.enabled()) {
      lastAdministrator(old, true);
      pending(id);
    }
    var before = detail(id);
    if (mapper.state(id, request.enabled(), changedAt(old)) != 1)
      throw new IllegalStateException("Account state failed");
    var after = detail(id);
    audit.snapshot(
        user,
        "USER_STATUS",
        "USER",
        after.id(),
        after.username(),
        request.enabled() ? "启用账号" : "停用账号，原登录凭证已失效",
        snapshot(before),
        snapshot(after));
    return after;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public StaffVO reset(long id, StaffResetPasswordRequest request, CurrentUser user) {
    require(user, "system.user.reset_password");
    var old = lock(id, request.expectedUpdatedAt());
    if (mapper.resetPassword(id, passwords.encode(request.password()), changedAt(old)) != 1)
      throw new IllegalStateException("Password reset failed");
    audit.record(
        user,
        "USER_RESET_PASSWORD",
        "USER",
        Long.toString(id),
        old.username(),
        "管理员重置密码，原登录凭证已失效；请线下告知使用者");
    return detail(id);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void delete(long id, StaffVersionRequest request, CurrentUser user) {
    require(user, "system.user.delete");
    var old = lock(id, request.expectedUpdatedAt());
    if (id == user.id()) throw BusinessException.conflict("不能删除当前登录账号");
    lastAdministrator(old, true);
    pending(id);
    if (mapper.referenced(id)) throw BusinessException.conflict("该账号已有业务关联，请使用停用以保留历史");
    var before = detail(id);
    if (mapper.delete(id) != 1) throw new IllegalStateException("Account delete failed");
    audit.snapshot(
        user,
        "USER_DELETE",
        "USER",
        Long.toString(id),
        old.username(),
        "删除从未产生业务关联的账号",
        snapshot(before),
        null);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            images.cleanup(old.avatarPath());
          }
        });
  }
}
