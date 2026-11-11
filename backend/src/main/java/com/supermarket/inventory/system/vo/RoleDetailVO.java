package com.supermarket.inventory.system.vo;

import java.util.List;

public record RoleDetailVO(
    RoleSummaryVO role,
    List<Node> nodes,
    List<String> permissions,
    List<String> protectedPermissions) {
  public record Node(String code, String label, String kind, String parent) {}
}
