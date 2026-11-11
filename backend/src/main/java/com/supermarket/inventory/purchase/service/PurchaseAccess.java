package com.supermarket.inventory.purchase.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.purchase.entity.PurchaseOrder;
import com.supermarket.inventory.purchase.mapper.PurchaseMapper;
import com.supermarket.inventory.security.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PurchaseAccess {
  private final PurchaseMapper mapper;

  public PurchaseAccess(PurchaseMapper mapper) {
    this.mapper = mapper;
  }

  public BusinessScope require(CurrentUser user, String permission) {
    if (!user.permissions().contains(permission)
        || (!"ADMIN".equals(user.roleCode()) && !"PURCHASER".equals(user.roleCode())))
      throw new org.springframework.security.access.AccessDeniedException("Purchase access denied");
    return BusinessScope.of(user);
  }

  public void requireAdmin(CurrentUser user, String permission) {
    require(user, permission);
    if (!"ADMIN".equals(user.roleCode()))
      throw new org.springframework.security.access.AccessDeniedException(
          "Review requires administrator");
  }

  public PurchaseOrder lock(long id, CurrentUser user, String permission) {
    var scope = require(user, permission);
    var order = mapper.lock(id, scope);
    if (order == null) throw BusinessException.notFound();
    return order;
  }

  public void version(PurchaseOrder order, Long expected) {
    if (expected == null)
      throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "修改或处理单据必须提供当前版本");
    if (order.rowVersion() != expected)
      throw new BusinessException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "单据已被其他操作修改，请刷新详情后重试");
  }
}
