package com.supermarket.inventory.security;

/** Persistence-only credential projection, never serialized from a controller. */
public record SessionAccount(
    Long id,
    String username,
    String passwordHash,
    String realName,
    String phone,
    String avatarPath,
    String roleCode,
    String roleName,
    boolean enabled,
    long credentialVersion,
    long authVersion,
    String permissionsJson) {}
