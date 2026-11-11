package com.supermarket.inventory.security;

import com.supermarket.inventory.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** New unfinished tasks and account disabling contend on the same account row. */
@Service
public class BusinessActorGuard {
  private final BusinessActorMapper mapper;

  public BusinessActorGuard(BusinessActorMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void newTask(CurrentUser user) {
    var state = mapper.lockShared(user.id());
    if (state == null
        || !state.enabled()
        || !state.roleCode().equals(user.roleCode())
        || state.credentialVersion() != user.credentialVersion())
      throw new BusinessException(HttpStatus.UNAUTHORIZED, "SESSION_CHANGED", "账号状态已变更，请重新登录后操作");
  }
}
