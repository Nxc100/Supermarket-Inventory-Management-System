package com.supermarket.inventory.catalog.mapper;

import com.supermarket.inventory.catalog.dto.SupplierQuery;
import com.supermarket.inventory.catalog.entity.Supplier;
import com.supermarket.inventory.catalog.vo.SupplierVO;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SupplierMapper {
  List<SupplierVO> page(SupplierQuery query);

  long count(SupplierQuery query);

  SupplierVO find(long id);

  @Select("SELECT id,code FROM supplier WHERE id=#{id} FOR UPDATE")
  Supplier lock(long id);

  @Insert(
      "INSERT INTO supplier(code,name,contact_name,contact_phone,address,remark,created_by) VALUES(#{code},#{name},#{contactName},#{contactPhone},#{address},#{remark},#{createdBy})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int create(Supplier supplier);

  @Update(
      "UPDATE supplier SET name=#{name},contact_name=#{contactName},contact_phone=#{contactPhone},address=#{address},remark=#{remark} WHERE id=#{id}")
  int update(Supplier supplier);

  @Update("UPDATE supplier SET enabled=#{enabled} WHERE id=#{id}")
  int status(@Param("id") long id, @Param("enabled") boolean enabled);

  @Select("SELECT COUNT(*) FROM purchase_order WHERE supplier_id=#{id}")
  long references(long id);

  @Delete("DELETE FROM supplier WHERE id=#{id}")
  int delete(long id);
}
