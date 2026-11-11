package com.supermarket.inventory.purchase.mapper;

import com.supermarket.inventory.purchase.dto.ReconcileSaveRequest;
import com.supermarket.inventory.purchase.enums.ReconcileStatus;
import com.supermarket.inventory.security.CurrentUser;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ReconciliationMapper {
  @Update(
      "UPDATE purchase_item SET external_qty=#{line.externalQty},external_unit_cost=#{line.externalUnitCost},reconcile_note=#{line.note} WHERE id=#{id} AND purchase_id=#{purchaseId}")
  int line(
      @Param("purchaseId") long purchaseId,
      @Param("id") long id,
      @Param("line") ReconcileSaveRequest.Line line);

  @Update(
      "UPDATE purchase_order SET reconcile_status=#{status},external_total_amount=#{request.externalTotal},reconcile_reference=#{request.reference},reconcile_difference=#{request.difference},reconcile_resolution=#{request.resolution},reconciled_by=#{actor.id},reconciled_at=#{now},reconcile_version=reconcile_version+1,row_version=row_version+1 WHERE id=#{id}")
  int save(
      @Param("id") long id,
      @Param("status") ReconcileStatus status,
      @Param("request") ReconcileSaveRequest request,
      @Param("actor") CurrentUser actor,
      @Param("now") LocalDateTime now);

  @Update(
      "UPDATE purchase_item SET external_qty=NULL,external_unit_cost=NULL,reconcile_note=NULL WHERE purchase_id=#{purchaseId}")
  int clearLines(long purchaseId);

  @Update(
      "UPDATE purchase_order SET reconcile_status='UNRECONCILED',external_total_amount=NULL,reconcile_reference=NULL,reconcile_difference=NULL,reconcile_resolution=NULL,reconciled_by=NULL,reconciled_at=NULL,reconcile_version=reconcile_version+1,row_version=row_version+1 WHERE id=#{id}")
  int withdraw(long id);
}
