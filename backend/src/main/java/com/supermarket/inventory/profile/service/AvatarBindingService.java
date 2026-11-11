package com.supermarket.inventory.profile.service;

import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.security.AccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvatarBindingService {
  private final AccountMapper accounts;

  public AvatarBindingService(AccountMapper accounts) {
    this.accounts = accounts;
  }

  /** Short transaction after image IO; a failed binding preserves the previous avatar. */
  @Transactional
  public String bind(long id, String path, String meta) {
    var account = accounts.lock(id);
    if (account == null) throw BusinessException.notFound();
    if (accounts.avatar(id, path, meta) != 1) throw BusinessException.notFound();
    return account.avatarPath();
  }
}
