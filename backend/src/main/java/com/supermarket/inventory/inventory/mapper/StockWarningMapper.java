package com.supermarket.inventory.inventory.mapper;

import com.supermarket.inventory.inventory.dto.WarningQuery;
import com.supermarket.inventory.inventory.vo.StockWarningVO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StockWarningMapper {
  List<StockWarningVO> page(@Param("query") WarningQuery query, @Param("today") LocalDate today);

  long count(@Param("query") WarningQuery query, @Param("today") LocalDate today);
}
