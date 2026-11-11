package com.supermarket.inventory.security;

import java.util.List;

public record CurrentUser(
    long id,
    String username,
    String realName,
    String roleCode,
    long credentialVersion,
    long authVersion,
    List<String> permissions) {}
