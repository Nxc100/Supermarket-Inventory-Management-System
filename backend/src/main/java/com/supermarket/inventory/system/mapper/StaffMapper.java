package com.supermarket.inventory.system.mapper;

import com.supermarket.inventory.system.dto.*;
import com.supermarket.inventory.system.entity.StaffAccount;
import com.supermarket.inventory.system.vo.*;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StaffMapper {
  List<StaffVO> page(StaffQuery query);

  long count(StaffQuery query);

  StaffVO find(long id);

  @Select(
      "SELECT id,username,real_name,phone,role_code,enabled,avatar_path,created_at,updated_at FROM sys_user WHERE id=#{id} FOR UPDATE")
  StaffAccount lock(long id);

  // One stable row serializes changes that can affect the last enabled administrator.
  @Select("SELECT code FROM sys_role WHERE code='ADMIN' FOR UPDATE")
  String lockAdministration();

  @Select("SELECT COUNT(*) FROM sys_user WHERE role_code='ADMIN' AND enabled=1")
  long enabledAdminCount();

  @Select(
      "SELECT COUNT(*) FROM purchase_order WHERE operator_id=#{id} AND status IN ('DRAFT','PENDING','REJECTED')")
  long unfinishedPurchases(long id);

  @Select(
      "SELECT COUNT(*) FROM stocktake_order WHERE operator_id=#{id} AND status IN ('COUNTING','PENDING','REJECTED')")
  long activeStocktakes(long id);

  boolean referenced(long id);

  @Insert(
      "INSERT INTO sys_user(username,password_hash,real_name,phone,role_code,avatar_path,created_at,updated_at) VALUES(#{r.username},#{hash},#{r.realName},#{r.phone},#{r.role},NULL,#{now},#{now})")
  int create(
      @Param("r") StaffCreateRequest request,
      @Param("hash") String hash,
      @Param("now") LocalDateTime now);

  @Select("SELECT id FROM sys_user WHERE username=#{username}")
  Long idByUsername(String username);

  @Update(
      "UPDATE sys_user SET real_name=#{r.realName},phone=#{r.phone},role_code=#{r.role},credential_version=credential_version+#{invalidate},updated_at=#{now} WHERE id=#{id}")
  int update(
      @Param("id") long id,
      @Param("r") StaffUpdateRequest request,
      @Param("invalidate") int invalidate,
      @Param("now") LocalDateTime now);

  @Update(
      "UPDATE sys_user SET enabled=#{enabled},credential_version=credential_version+1,updated_at=#{now} WHERE id=#{id}")
  int state(
      @Param("id") long id, @Param("enabled") boolean enabled, @Param("now") LocalDateTime now);

  @Update(
      "UPDATE sys_user SET password_hash=#{hash},credential_version=credential_version+1,updated_at=#{now} WHERE id=#{id}")
  int resetPassword(
      @Param("id") long id, @Param("hash") String hash, @Param("now") LocalDateTime now);

  @Delete("DELETE FROM sys_user WHERE id=#{id}")
  int delete(long id);

  List<StaffTaskVO> tasks(
      @Param("id") long id, @Param("q") com.supermarket.inventory.common.PageQuery query);
}
