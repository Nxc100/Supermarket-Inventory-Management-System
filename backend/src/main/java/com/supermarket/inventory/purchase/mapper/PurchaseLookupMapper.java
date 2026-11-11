package com.supermarket.inventory.purchase.mapper;

import com.supermarket.inventory.common.PageQuery;
import com.supermarket.inventory.purchase.dto.SupplierSummaryQuery;
import com.supermarket.inventory.purchase.vo.*;
import com.supermarket.inventory.security.BusinessScope;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PurchaseLookupMapper {
  List<SupplierOptionVO> suppliers(PageQuery query);

  long supplierCount(PageQuery query);

  List<SupplierPurchaseSummaryVO> summary(
      @Param("query") SupplierSummaryQuery query, @Param("scope") BusinessScope scope);

  long summaryCount(
      @Param("query") SupplierSummaryQuery query, @Param("scope") BusinessScope scope);
}
