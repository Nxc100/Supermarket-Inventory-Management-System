package com.supermarket.inventory.system.service;

import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.inventory.dto.WarningQuery;
import com.supermarket.inventory.inventory.mapper.StockWarningMapper;
import com.supermarket.inventory.purchase.dto.PurchaseQuery;
import com.supermarket.inventory.purchase.enums.*;
import com.supermarket.inventory.purchase.mapper.PurchaseMapper;
import com.supermarket.inventory.sale.dto.SaleQuery;
import com.supermarket.inventory.sale.enums.SaleStatus;
import com.supermarket.inventory.sale.mapper.SaleMapper;
import com.supermarket.inventory.security.*;
import com.supermarket.inventory.stocktake.dto.StocktakeQuery;
import com.supermarket.inventory.stocktake.enums.StocktakeStatus;
import com.supermarket.inventory.stocktake.mapper.StocktakeMapper;
import com.supermarket.inventory.system.vo.DashboardVO;
import com.supermarket.inventory.system.vo.DashboardVO.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Reuses the list predicates so each metric links to the same authorized set. */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class DashboardService {
  private final PurchaseMapper purchases;
  private final StocktakeMapper stocktakes;
  private final SaleMapper sales;
  private final StockWarningMapper warnings;
  private final Clock clock;

  public DashboardService(
      PurchaseMapper purchases,
      StocktakeMapper stocktakes,
      SaleMapper sales,
      StockWarningMapper warnings,
      Clock clock) {
    this.purchases = purchases;
    this.stocktakes = stocktakes;
    this.sales = sales;
    this.warnings = warnings;
    this.clock = clock;
  }

  private boolean can(CurrentUser user, String permission) {
    return user.permissions().contains(permission);
  }

  public DashboardVO dashboard(CurrentUser user) {
    if (!can(user, "common.dashboard")
        || !Set.of("ADMIN", "PURCHASER", "CASHIER").contains(user.roleCode()))
      throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "当前岗位无权查看工作台");
    var today = LocalDate.now(clock);
    var scope = BusinessScope.of(user);
    boolean admin = scope.admin(), buyer = "PURCHASER".equals(user.roleCode());
    var metrics = new ArrayList<Metric>();
    var shortcuts = new ArrayList<Shortcut>();
    if ((admin || "CASHIER".equals(user.roleCode())) && can(user, "sale.order")) {
      var query = new SaleQuery();
      query.setStartDate(today);
      query.setEndDate(today);
      query.setStatus(SaleStatus.COMPLETED);
      if (!admin) query.setCashierId(Long.toString(user.id()));
      var totals = sales.totals(query, scope);
      var filters =
          Map.of(
              "fromDashboard",
              admin ? "allToday" : "ownToday",
              "startDate",
              today.toString(),
              "endDate",
              today.toString(),
              "status",
              "COMPLETED");
      metrics.add(
          new Metric(
              "todaySales",
              admin ? "今日有效销售额" : "本人今日有效销售额",
              totals.netAmount().setScale(2).toPlainString(),
              "元",
              "北京时间今天，已完成且未整单纠错",
              "/sales",
              filters));
      if (!admin)
        metrics.add(
            new Metric(
                "todayCompleted",
                "本人今日完成销售单",
                Long.toString(totals.completedCount()),
                "单",
                "已完成且未整单纠错；只含本人",
                "/sales",
                filters));
    }
    if (admin && can(user, "purchase.order") && can(user, "purchase.order.review"))
      purchaseMetric(
          metrics,
          scope,
          PurchaseStatus.PENDING,
          null,
          "pendingPurchases",
          "待审核采购",
          "/purchases/review");
    if (admin && can(user, "inventory.stocktake") && can(user, "inventory.stocktake.review")) {
      var query = new StocktakeQuery();
      query.setStatus(StocktakeStatus.PENDING);
      metrics.add(
          new Metric(
              "pendingStocktakes",
              "待审核盘点",
              Long.toString(stocktakes.count(query, scope)),
              "单",
              "全店待审核任务；不限制创建日期",
              "/stocktakes",
              Map.of("status", "PENDING")));
    }
    if (buyer && can(user, "purchase.order")) {
      purchaseMetric(
          metrics, scope, PurchaseStatus.DRAFT, null, "draftPurchases", "本人采购草稿", "/purchases");
      purchaseMetric(
          metrics,
          scope,
          PurchaseStatus.REJECTED,
          null,
          "rejectedPurchases",
          "本人被驳回采购",
          "/purchases");
      if (can(user, "purchase.order.reconcile")) {
        purchaseMetric(
            metrics,
            scope,
            PurchaseStatus.IN_STOCK,
            ReconcileStatus.UNRECONCILED,
            "unreconciledPurchases",
            "本人待核对采购",
            "/purchases");
        purchaseMetric(
            metrics,
            scope,
            PurchaseStatus.IN_STOCK,
            ReconcileStatus.DIFFERENCE,
            "differentPurchases",
            "本人有差异采购",
            "/purchases");
      }
    }
    if ((admin || buyer) && can(user, "inventory.warning")) {
      for (var type : WarningQuery.Type.values()) {
        if (admin && type == WarningQuery.Type.EXPIRED) continue;
        var query = new WarningQuery();
        query.setType(type);
        var label =
            type == WarningQuery.Type.LOW
                ? "低库存商品"
                : type == WarningQuery.Type.NEAR ? "临期批次" : "过期批次";
        metrics.add(
            new Metric(
                type.name(),
                label,
                Long.toString(warnings.count(query, today)),
                type == WarningQuery.Type.LOW ? "种" : "批",
                "与当前预警列表同口径；冻结不将可售库存归零",
                "/inventory/warnings",
                Map.of("type", type.name())));
      }
    }
    if (admin) {
      shortcut(shortcuts, user, "base.product", "商品维护", "/catalog/products");
      shortcut(shortcuts, user, "purchase.order.review", "采购审核", "/purchases/review");
      if (can(user, "inventory.stocktake.review") && can(user, "inventory.stocktake"))
        shortcuts.add(new Shortcut("盘点审核", "/stocktakes", Map.of("status", "PENDING")));
      shortcut(shortcuts, user, "statistics.sales", "销售统计", "/statistics/sales");
      shortcut(shortcuts, user, "statistics.inventory", "库存统计", "/statistics/inventory");
    } else if (buyer) {
      shortcut(shortcuts, user, "purchase.order.create", "新建采购", "/purchases/new");
      shortcut(shortcuts, user, "base.supplier", "供应商维护", "/catalog/suppliers");
      shortcut(shortcuts, user, "inventory.query", "库存查询", "/inventory/products");
      shortcut(shortcuts, user, "inventory.stocktake", "库存盘点", "/stocktakes");
    } else {
      shortcut(shortcuts, user, "sale.order.complete", "新建销售", "/sales/new");
      shortcut(shortcuts, user, "sale.order", "本人销售记录", "/sales");
      shortcut(shortcuts, user, "inventory.query", "只读商品查询", "/inventory/saleable-products");
      shortcut(shortcuts, user, "statistics.sales", "销售统计", "/statistics/sales");
    }
    if ((admin || buyer) && can(user, "ai.inventory"))
      shortcuts.add(new Shortcut("库存优化建议", "/suggestions", Map.of("type", "INVENTORY")));
    if (admin && can(user, "ai.sales"))
      shortcuts.add(new Shortcut("销售经营建议", "/suggestions", Map.of("type", "SALES")));
    return new DashboardVO(
        today,
        LocalDateTime.now(clock),
        admin ? "全店业务指标" : buyer ? "本人采购任务及全店库存预警" : "仅本人销售业务；统计入口可查看授权的全店聚合",
        List.copyOf(metrics),
        List.copyOf(shortcuts));
  }

  private void shortcut(
      List<Shortcut> target, CurrentUser user, String permission, String label, String path) {
    if (can(user, permission)) target.add(new Shortcut(label, path, Map.of()));
  }

  private void purchaseMetric(
      List<Metric> target,
      BusinessScope scope,
      PurchaseStatus status,
      ReconcileStatus reconcile,
      String code,
      String label,
      String path) {
    var query = new PurchaseQuery();
    query.setStatus(status);
    query.setReconcileStatus(reconcile);
    var filters = new LinkedHashMap<String, String>();
    filters.put("status", status.name());
    if (reconcile != null) filters.put("reconcileStatus", reconcile.name());
    target.add(
        new Metric(
            code,
            label,
            Long.toString(purchases.count(query, scope)),
            "单",
            scope.admin() ? "全店当前状态；不限制创建日期" : "本人当前状态；不限制创建日期",
            path,
            Map.copyOf(filters)));
  }
}
