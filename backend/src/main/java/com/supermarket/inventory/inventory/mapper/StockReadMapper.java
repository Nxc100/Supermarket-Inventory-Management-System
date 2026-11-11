package com.supermarket.inventory.inventory.mapper;

import com.supermarket.inventory.inventory.dto.*;
import com.supermarket.inventory.inventory.vo.*;
import com.supermarket.inventory.security.BusinessScope;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StockReadMapper {
  List<StockInventoryVO> products(
      @Param("query") StockQuery query, @Param("today") LocalDate today);

  long productCount(@Param("query") StockQuery query, @Param("today") LocalDate today);

  boolean exists(long id);

  List<StockBatchVO> batches(
      @Param("id") long id,
      @Param("query") BatchQuery query,
      @Param("today") LocalDate today,
      @Param("scope") BusinessScope scope,
      @Param("canReadPurchase") boolean canReadPurchase);

  long batchCount(@Param("id") long id, @Param("query") BatchQuery query);

  List<StockMovementVO> movements(
      @Param("query") MovementQuery query,
      @Param("scope") BusinessScope scope,
      @Param("canReadPurchase") boolean canReadPurchase);

  long movementCount(@Param("query") MovementQuery query);
}
