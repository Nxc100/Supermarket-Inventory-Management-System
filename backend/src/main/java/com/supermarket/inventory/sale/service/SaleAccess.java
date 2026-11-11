package com.supermarket.inventory.sale.service;

import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.sale.entity.SaleOrder;
import com.supermarket.inventory.security.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class SaleAccess {
  public BusinessScope require(CurrentUser user, String permission) {
    if (!user.permissions().contains(permission)
        || !java.util.Set.of("ADMIN", "CASHIER").contains(user.roleCode()))
      throw new AccessDeniedException("Sale access denied");
    return BusinessScope.of(user);
  }

  public void requireAdmin(CurrentUser user, String permission) {
    require(user, permission);
    if (!"ADMIN".equals(user.roleCode()))
      throw new AccessDeniedException("Correction requires administrator");
  }

  public void sameRequest(SaleOrder order, CurrentUser user, String hash) {
    if (order.cashierId() != user.id() || !order.requestHash().equals(hash))
      throw BusinessException.conflict("该提交标识已被使用，请查询原结果；不同开单必须使用新标识");
  }
}
