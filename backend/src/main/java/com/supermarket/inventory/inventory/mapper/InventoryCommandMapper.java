package com.supermarket.inventory.inventory.mapper;

import com.supermarket.inventory.inventory.entity.*;
import com.supermarket.inventory.inventory.enums.MovementType;
import com.supermarket.inventory.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface InventoryCommandMapper {
  List<StockProduct> lockProducts(@Param("ids") List<Long> ids);

  List<BatchInventory> lockPurchaseBatches(long purchaseId);

  List<BatchInventory> lockSaleBatches(@Param("productIds") List<Long> productIds);

  List<BatchInventory> lockStocktakeBatches(@Param("productIds") List<Long> productIds);

  List<BatchInventory> lockBatchIds(@Param("ids") List<Long> ids);

  List<StockReceipt> receipts(@Param("ids") List<Long> ids);

  List<StockCategory> categories(@Param("ids") List<Long> ids);

  @Select("SELECT id FROM product WHERE frozen_stocktake_id=#{id} ORDER BY id")
  List<Long> frozenProductIds(long id);

  @Update(
      "UPDATE purchase_item SET batch_no=#{batchNo},stock_qty=#{qty} WHERE id=#{id} AND batch_no IS NULL AND stock_qty IS NULL")
  int establishBatch(
      @Param("id") long id, @Param("batchNo") String batchNo, @Param("qty") long qty);

  @Update(
      "UPDATE purchase_item SET stock_qty=#{after} WHERE id=#{id} AND stock_qty=#{before} AND batch_no IS NOT NULL")
  int batchBalance(@Param("id") long id, @Param("before") long before, @Param("after") long after);

  @Update(
      "UPDATE product SET stock_qty=#{after},stock_version=stock_version+1 WHERE id=#{product.id} AND stock_qty=#{product.stockQty} AND stock_version=#{product.stockVersion} AND frozen_stocktake_id <=> #{freezeId}")
  int productBalance(
      @Param("product") StockProduct product,
      @Param("after") long after,
      @Param("freezeId") Long freezeId);

  @Update(
      "UPDATE product SET frozen_stocktake_id=#{after},stock_version=stock_version+1 WHERE id=#{id} AND frozen_stocktake_id <=> #{before}")
  int freeze(@Param("id") long id, @Param("before") Long before, @Param("after") Long after);

  long laterMovements(@Param("batchIds") List<Long> batchIds);

  @Insert(
      "INSERT INTO stock_movement(event_type,product_id,batch_item_id,sale_allocation_id,stocktake_item_id,business_no,delta_qty,batch_before,batch_after,product_before,product_after,actor_id,actor_name,reason,occurred_at) VALUES(#{type},#{productId},#{batchId},#{allocationId},#{stocktakeItemId},#{number},#{delta},#{batchBefore},#{batchAfter},#{productBefore},#{productAfter},#{actor.id},#{actor.realName},#{reason},#{now})")
  int movement(
      @Param("type") MovementType type,
      @Param("productId") long productId,
      @Param("batchId") long batchId,
      @Param("number") String number,
      @Param("delta") long delta,
      @Param("batchBefore") long batchBefore,
      @Param("batchAfter") long batchAfter,
      @Param("productBefore") long productBefore,
      @Param("productAfter") long productAfter,
      @Param("actor") CurrentUser actor,
      @Param("reason") String reason,
      @Param("now") LocalDateTime now,
      @Param("allocationId") Long allocationId,
      @Param("stocktakeItemId") Long stocktakeItemId);
}
