package com.supermarket.inventory.catalog.mapper;

import com.supermarket.inventory.catalog.dto.CategoryRequest;
import com.supermarket.inventory.catalog.entity.Category;
import com.supermarket.inventory.catalog.vo.CategoryVO;
import com.supermarket.inventory.common.PageQuery;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CategoryMapper {
  List<CategoryVO> page(PageQuery query);

  long count(PageQuery query);

  @Select(
      "SELECT CAST(c.id AS CHAR) id,c.code,c.name,c.sort_order,c.enabled,(SELECT COUNT(*) FROM product p WHERE p.category_id=c.id) product_count,c.created_at FROM category c WHERE c.id=#{id}")
  CategoryVO find(long id);

  @Select("SELECT id,code,name,sort_order,enabled FROM category WHERE id=#{id} FOR UPDATE")
  Category lock(long id);

  @Insert("INSERT INTO category(code,name,sort_order) VALUES(#{code},#{name},#{sortOrder})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Category category);

  @Update(
      "UPDATE category SET code=#{request.code},name=#{request.name},sort_order=#{request.sortOrder} WHERE id=#{id}")
  int update(@Param("id") long id, @Param("request") CategoryRequest request);

  @Update("UPDATE category SET enabled=#{enabled} WHERE id=#{id}")
  int status(@Param("id") long id, @Param("enabled") boolean enabled);

  @Delete("DELETE FROM category WHERE id=#{id}")
  int delete(long id);

  @Select("SELECT COUNT(*) FROM product WHERE category_id=#{id}")
  long references(long id);
}
