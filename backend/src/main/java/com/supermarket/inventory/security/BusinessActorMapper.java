package com.supermarket.inventory.security;

import org.apache.ibatis.annotations.*;

@Mapper
public interface BusinessActorMapper {
  record State(long id, String roleCode, boolean enabled, long credentialVersion) {}

  @Select("SELECT id,role_code,enabled,credential_version FROM sys_user WHERE id=#{id} FOR SHARE")
  State lockShared(long id);
}
