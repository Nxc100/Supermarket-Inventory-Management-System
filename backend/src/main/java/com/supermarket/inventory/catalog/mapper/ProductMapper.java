package com.supermarket.inventory.catalog.mapper;

import com.supermarket.inventory.catalog.dto.ProductQuery;
import com.supermarket.inventory.catalog.entity.*;
import com.supermarket.inventory.catalog.vo.*;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProductMapper {
  List<ProductVO> page(ProductQuery query);

  long count(ProductQuery query);

  ProductVO find(long id);

  List<PurchaseProductVO> purchaseLookup(ProductQuery query);

  long purchaseLookupCount(ProductQuery query);

  List<SaleProductVO> saleLookup(
      @Param("query") ProductQuery query, @Param("today") java.time.LocalDate today);

  long saleLookupCount(ProductQuery query);

  @Select(
      "SELECT id,code,category_id,unit,manages_expiry,stock_qty,image_path FROM product WHERE id=#{id} FOR UPDATE")
  ProductLock lock(long id);

  boolean referenced(long id);

  int update(Product product);

  @Options(useGeneratedKeys = true, keyProperty = "id")
  @Insert(
      "INSERT INTO product(code,barcode,name,category_id,specification,unit,reference_cost,sale_price,manages_expiry,shelf_life_days,near_expiry_days,stock_lower_limit,can_purchase,can_sell,remark,created_by,image_path) VALUES(#{code},#{barcode},#{name},#{categoryId},#{specification},#{unit},#{referenceCost},#{salePrice},#{managesExpiry},#{shelfLifeDays},#{nearExpiryDays},#{stockLowerLimit},#{canPurchase},#{canSell},#{remark},#{createdBy},NULL)")
  int create(Product product);

  @Update("UPDATE product SET enabled=#{enabled} WHERE id=#{id}")
  int status(@Param("id") long id, @Param("enabled") boolean enabled);

  @Delete("DELETE FROM product WHERE id=#{id}")
  int delete(long id);

  @Select("SELECT CAST(id AS CHAR) id,code,name,enabled FROM category ORDER BY sort_order,id")
  List<CategoryOptionVO> categoryOptions();

  @Select("SELECT CAST(config_value AS UNSIGNED) FROM sys_config WHERE config_key=#{key}")
  Long defaultValue(String key);

  @Update("UPDATE product SET image_path=#{path},image_meta=#{meta} WHERE id=#{id}")
  int image(@Param("id") long id, @Param("path") String path, @Param("meta") String meta);
}
