package com.supermarket.inventory.security;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AccountMapper {
  String PROJECTION =
      "SELECT u.id,u.username,u.password_hash,u.real_name,u.phone,u.avatar_path,u.role_code,r.name AS role_name,u.enabled,u.credential_version,r.auth_version,r.permissions AS permissions_json FROM sys_user u JOIN sys_role r ON r.code=u.role_code ";

  @Select(PROJECTION + "WHERE u.username=#{username}")
  @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
  SessionAccount findByUsername(String username);

  @Select(PROJECTION + "WHERE u.id=#{id}")
  @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
  SessionAccount findById(long id);

  @Select("SELECT id FROM sys_user WHERE id=#{id} FOR UPDATE")
  @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
  Long lockUser(long id);

  /**
   * Role metadata is a nonlocking read; account commands never take role locks after user locks.
   */
  default SessionAccount lock(long id) {
    return lockUser(id) == null ? null : findById(id);
  }

  @Update("UPDATE sys_user SET last_login_at=#{now} WHERE id=#{id}")
  int login(@Param("id") long id, @Param("now") LocalDateTime now);

  @Update("UPDATE sys_user SET credential_version=credential_version+1 WHERE id=#{id}")
  int invalidate(long id);

  @Update("UPDATE sys_user SET real_name=#{name},phone=#{phone} WHERE id=#{id}")
  int updateProfile(@Param("id") long id, @Param("name") String name, @Param("phone") String phone);

  @Update(
      "UPDATE sys_user SET password_hash=#{hash},credential_version=credential_version+1 WHERE id=#{id}")
  int password(@Param("id") long id, @Param("hash") String hash);

  @Update("UPDATE sys_user SET avatar_path=#{path},avatar_meta=#{meta} WHERE id=#{id}")
  int avatar(@Param("id") long id, @Param("path") String path, @Param("meta") String meta);
}
