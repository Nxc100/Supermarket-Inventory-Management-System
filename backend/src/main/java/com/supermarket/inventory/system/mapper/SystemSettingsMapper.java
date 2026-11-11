package com.supermarket.inventory.system.mapper;

import com.supermarket.inventory.system.dto.OperationLogQuery;
import com.supermarket.inventory.system.entity.*;
import com.supermarket.inventory.system.vo.OperationLogVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SystemSettingsMapper {
  @Select(
      "SELECT config_key `key`,CAST(config_value AS CHAR) value,editable FROM sys_config WHERE config_key IN ('inventory.default_lower_limit','inventory.default_near_expiry_days') ORDER BY config_key")
  List<ConfigValue> defaults();

  @Select(
      "SELECT config_key `key`,CAST(config_value AS CHAR) value,editable FROM sys_config WHERE config_key IN ('inventory.default_lower_limit','inventory.default_near_expiry_days') ORDER BY config_key FOR UPDATE")
  List<ConfigValue> lockDefaults();

  @Update(
      "UPDATE sys_config SET config_value=#{value},updated_by=#{userId},updated_at=#{now} WHERE config_key=#{key} AND editable=1")
  int updateDefault(
      @Param("key") String key,
      @Param("value") String value,
      @Param("userId") long userId,
      @Param("now") LocalDateTime now);

  List<OperationLogVO> logs(OperationLogQuery query);

  long logCount(OperationLogQuery query);

  @Select(
      "SELECT CAST(id AS CHAR) id,actor_name,action,target_type,target_id,target_no,result,reason,request_id,occurred_at FROM operation_log WHERE id=#{id}")
  OperationLogVO log(long id);

  @Select("SELECT before_data,after_data FROM operation_log WHERE id=#{id}")
  LogSnapshot snapshot(long id);

  @Select("SELECT DISTINCT action FROM operation_log ORDER BY action LIMIT 200")
  List<String> actions();
}
