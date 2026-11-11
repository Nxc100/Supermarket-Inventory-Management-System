package com.supermarket.inventory.inventory.mapper;

import com.supermarket.inventory.inventory.dto.InventoryQuery;
import com.supermarket.inventory.inventory.vo.CashierInventoryVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SaleableInventoryMapper {
  List<CashierInventoryVO> page(
      @org.apache.ibatis.annotations.Param("query") InventoryQuery query,
      @org.apache.ibatis.annotations.Param("today") java.time.LocalDate today);

  long count(@org.apache.ibatis.annotations.Param("query") InventoryQuery query);
}
