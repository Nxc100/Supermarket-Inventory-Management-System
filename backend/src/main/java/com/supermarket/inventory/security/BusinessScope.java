package com.supermarket.inventory.security;

/** Scope is derived only from the authenticated account, never request parameters. */
public record BusinessScope(long userId, boolean admin) {
  public static BusinessScope of(CurrentUser user) {
    return new BusinessScope(user.id(), "ADMIN".equals(user.roleCode()));
  }
}
