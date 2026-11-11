package com.supermarket.inventory.security;

import java.util.List;

public record UserVO(
    String id,
    String username,
    String realName,
    String phone,
    String avatarPath,
    String roleCode,
    String roleName,
    boolean enabled,
    List<String> permissions) {
  public static UserVO from(SessionAccount account, CurrentUser principal) {
    return new UserVO(
        account.id().toString(),
        account.username(),
        account.realName(),
        account.phone(),
        account.avatarPath(),
        account.roleCode(),
        account.roleName(),
        account.enabled(),
        principal.permissions());
  }
}
