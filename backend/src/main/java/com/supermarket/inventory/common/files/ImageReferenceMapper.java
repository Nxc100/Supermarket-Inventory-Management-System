package com.supermarket.inventory.common.files;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ImageReferenceMapper {
  @Select(
      "SELECT (SELECT COUNT(*) FROM sys_user WHERE avatar_path=#{path})+(SELECT COUNT(*) FROM product WHERE image_path=#{path})")
  long count(String path);
}
