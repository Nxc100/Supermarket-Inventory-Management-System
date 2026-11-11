package com.supermarket.inventory.ai.mapper;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AiSettingsMapper {
  @Select(
      "SELECT CAST(config_value AS CHAR) FROM sys_config WHERE config_key='ai.provider_settings'")
  String settings();

  @Insert(
      "INSERT INTO sys_config(config_key,config_value,description,editable,updated_by,updated_at) VALUES('ai.provider_settings',#{value},'模型配置；密钥仅保存AES-GCM密文',1,#{user},#{now}) ON DUPLICATE KEY UPDATE config_value=#{value},updated_by=#{user},updated_at=#{now}")
  int save(@Param("value") String value, @Param("user") long user, @Param("now") LocalDateTime now);
}
