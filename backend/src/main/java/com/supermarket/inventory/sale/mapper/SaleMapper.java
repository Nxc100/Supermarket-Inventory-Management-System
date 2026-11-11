package com.supermarket.inventory.sale.mapper;

import com.supermarket.inventory.sale.dto.SaleQuery;
import com.supermarket.inventory.sale.entity.*;
import com.supermarket.inventory.sale.vo.*;
import com.supermarket.inventory.security.*;
import java.time.*;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SaleMapper {
  SaleOrder byKey(String requestKey);

  SaleOrder find(@Param("id") long id, @Param("scope") BusinessScope scope);

  SaleOrder lock(@Param("id") long id, @Param("scope") BusinessScope scope);

  List<SaleSummaryVO> page(@Param("query") SaleQuery query, @Param("scope") BusinessScope scope);

  long count(@Param("query") SaleQuery query, @Param("scope") BusinessScope scope);

  SaleTotalsVO totals(@Param("query") SaleQuery query, @Param("scope") BusinessScope scope);

  List<SaleItem> items(long saleId);

  List<SaleAllocation> allocations(long saleId);

  @Select(
      "SELECT EXISTS(SELECT 1 FROM sale_allocation a JOIN sale_item s ON s.id=a.sale_item_id JOIN purchase_item b ON b.id=a.batch_item_id WHERE s.sale_id=#{id} AND b.manages_expiry=1 AND b.expiry_date<#{today} AND b.stock_qty>0)")
  boolean expiredSourceStock(@Param("id") long id, @Param("today") LocalDate today);

  List<SaleQuoteProduct> quoteProducts(
      @Param("ids") List<Long> ids, @Param("today") LocalDate today);

  int create(SaleWrite sale);

  int insertItems(@Param("items") List<SaleItemWrite> items);

  int insertAllocations(@Param("items") List<SaleAllocationWrite> items);

  @Update(
      "UPDATE sale_order SET status='CORRECTED',corrected_by=#{actor.id},corrector_name=#{actor.realName},corrected_at=#{now},correction_reason=#{reason},row_version=row_version+1 WHERE id=#{id} AND status='COMPLETED'")
  int correct(
      @Param("id") long id,
      @Param("actor") CurrentUser actor,
      @Param("now") LocalDateTime now,
      @Param("reason") String reason);

  @Select(
      "SELECT CAST(id AS CHAR) id,real_name name FROM sys_user WHERE role_code IN ('ADMIN','CASHIER') OR id IN (SELECT DISTINCT cashier_id FROM sale_order) ORDER BY enabled DESC,id")
  List<CashierOptionVO> cashiers();

  @Select(
      "SELECT JSON_UNQUOTE(config_value) FROM sys_config WHERE config_key='sales.default_payment_method'")
  String defaultPayment();
}
