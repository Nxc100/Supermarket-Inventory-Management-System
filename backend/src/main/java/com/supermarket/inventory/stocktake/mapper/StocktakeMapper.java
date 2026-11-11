package com.supermarket.inventory.stocktake.mapper;

import com.supermarket.inventory.inventory.entity.BatchInventory;
import com.supermarket.inventory.security.*;
import com.supermarket.inventory.stocktake.dto.*;
import com.supermarket.inventory.stocktake.entity.*;
import com.supermarket.inventory.stocktake.enums.StocktakeStatus;
import com.supermarket.inventory.stocktake.vo.*;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StocktakeMapper {
  StocktakeOrder find(@Param("id") long id, @Param("scope") BusinessScope scope);

  StocktakeOrder lock(@Param("id") long id, @Param("scope") BusinessScope scope);

  List<StocktakeItem> items(long id);

  List<StocktakeItemVO> itemViews(long id);

  StocktakeSummaryVO summary(long id);

  List<StocktakeSummaryVO> page(@Param("q") StocktakeQuery q, @Param("scope") BusinessScope scope);

  long count(@Param("q") StocktakeQuery q, @Param("scope") BusinessScope scope);

  int create(StocktakeWrite header);

  int insertItems(@Param("id") long id, @Param("batches") List<BatchInventory> batches);

  @Update(
      "UPDATE stocktake_item SET actual_qty=#{line.actualQty},reason_type=#{line.reasonType},reason_note=#{line.reasonNote} WHERE id=#{line.id} AND stocktake_id=#{id}")
  int saveLine(@Param("id") long id, @Param("line") StocktakeSaveRequest.Line line);

  @Update("UPDATE stocktake_order SET row_version=row_version+1 WHERE id=#{id}")
  int touch(long id);

  int transition(
      @Param("id") long id,
      @Param("status") StocktakeStatus status,
      @Param("user") CurrentUser user,
      @Param("reason") String reason,
      @Param("now") LocalDateTime now);
}
