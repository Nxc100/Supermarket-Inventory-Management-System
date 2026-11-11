package com.supermarket.inventory.system.mapper;

import com.supermarket.inventory.system.entity.RoleState;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface RoleMapper {
  String COLUMNS =
      "code,name,data_scope,CAST(permissions AS CHAR) permissions_json,CAST(default_permissions AS CHAR) default_json,CAST(ceiling_permissions AS CHAR) ceiling_json,auth_version,updated_at";

  @Select(
      "SELECT "
          + COLUMNS
          + " FROM sys_role WHERE code IN ('ADMIN','PURCHASER','CASHIER') ORDER BY FIELD(code,'ADMIN','PURCHASER','CASHIER')")
  List<RoleState> all();

  @Select("SELECT " + COLUMNS + " FROM sys_role WHERE code=#{code}")
  RoleState find(String code);

  @Select("SELECT " + COLUMNS + " FROM sys_role WHERE code=#{code} FOR UPDATE")
  RoleState lock(String code);

  @Select(
      "SELECT CAST(config_value AS CHAR) FROM sys_config WHERE config_key='security.permission_catalog'")
  String catalog();

  @Update(
      "UPDATE sys_role SET permissions=#{permissions},auth_version=auth_version+1,updated_at=#{now} WHERE code=#{code} AND auth_version=#{version}")
  int save(
      @Param("code") String code,
      @Param("permissions") String permissions,
      @Param("version") long version,
      @Param("now") LocalDateTime now);
}
