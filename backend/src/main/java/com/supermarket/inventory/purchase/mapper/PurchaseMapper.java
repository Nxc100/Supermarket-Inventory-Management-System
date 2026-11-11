package com.supermarket.inventory.purchase.mapper;

import com.supermarket.inventory.purchase.dto.PurchaseQuery;
import com.supermarket.inventory.purchase.entity.*;
import com.supermarket.inventory.purchase.enums.*;
import com.supermarket.inventory.purchase.vo.*;
import com.supermarket.inventory.security.*;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PurchaseMapper {
  PurchaseOrder find(@Param("id") long id, @Param("scope") BusinessScope scope);

  PurchaseOrder lock(@Param("id") long id, @Param("scope") BusinessScope scope);

  List<PurchaseOrder> page(
      @Param("query") PurchaseQuery query, @Param("scope") BusinessScope scope);

  long count(@Param("query") PurchaseQuery query, @Param("scope") BusinessScope scope);

  List<PurchaseItem> items(long purchaseId);

  int create(PurchaseWrite header);

  int update(PurchaseWrite header);

  int insertItems(
      @Param("purchaseId") long purchaseId,
      @Param("items") List<PurchaseItem> items,
      @Param("now") LocalDateTime now);

  @Delete("DELETE FROM purchase_item WHERE purchase_id=#{purchaseId}")
  int deleteItems(long purchaseId);

  @Delete("DELETE FROM purchase_order WHERE id=#{id}")
  int delete(long id);

  @Update(
      "UPDATE purchase_order SET status='PENDING',first_submitted_at=COALESCE(first_submitted_at,#{now}),submitted_at=#{now},reviewed_by=NULL,reviewer_name=NULL,reviewed_at=NULL,rejection_reason=NULL,row_version=row_version+1 WHERE id=#{id}")
  int submit(@Param("id") long id, @Param("now") LocalDateTime now);

  @Update(
      "UPDATE purchase_order SET status='DRAFT',submitted_at=NULL,row_version=row_version+1 WHERE id=#{id}")
  int withdraw(long id);

  @Update(
      "UPDATE purchase_order SET status='REJECTED',reviewed_by=#{actor.id},reviewer_name=#{actor.realName},reviewed_at=#{now},rejection_reason=#{reason},row_version=row_version+1 WHERE id=#{id}")
  int reject(
      @Param("id") long id,
      @Param("actor") CurrentUser actor,
      @Param("now") LocalDateTime now,
      @Param("reason") String reason);

  @Update(
      "UPDATE purchase_order SET status='IN_STOCK',reviewed_by=#{actor.id},reviewer_name=#{actor.realName},reviewed_at=#{now},stocked_at=#{now},rejection_reason=NULL,row_version=row_version+1 WHERE id=#{id}")
  int approve(
      @Param("id") long id, @Param("actor") CurrentUser actor, @Param("now") LocalDateTime now);

  @Update(
      "UPDATE purchase_order SET status='CLOSED',closed_by=#{actor.id},closed_at=#{now},close_reason=#{reason},row_version=row_version+1 WHERE id=#{id}")
  int close(
      @Param("id") long id,
      @Param("actor") CurrentUser actor,
      @Param("now") LocalDateTime now,
      @Param("reason") String reason);

  @Update(
      "UPDATE purchase_order SET status='REVERSED',reversed_by=#{actor.id},reversed_at=#{now},reverse_reason=#{reason},reconcile_status='NA',row_version=row_version+1 WHERE id=#{id}")
  int reverse(
      @Param("id") long id,
      @Param("actor") CurrentUser actor,
      @Param("now") LocalDateTime now,
      @Param("reason") String reason);

  List<ReconcileHistoryRow> reconcileHistory(long id);

  @Select(
      "SELECT id,name,contact_name,contact_phone,address,enabled FROM supplier WHERE id=#{id} FOR UPDATE")
  PurchaseSupplier lockSupplier(long id);

  List<CategoryName> categoryNames(@Param("ids") List<Long> ids);
}
