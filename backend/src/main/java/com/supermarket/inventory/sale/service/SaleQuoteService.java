package com.supermarket.inventory.sale.service;

import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.sale.dto.SaleQuoteRequest;
import com.supermarket.inventory.sale.mapper.SaleMapper;
import com.supermarket.inventory.sale.vo.SaleQuoteVO;
import com.supermarket.inventory.security.CurrentUser;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleQuoteService {
  private final SaleMapper mapper;
  private final SaleAccess access;
  private final Clock clock;

  public SaleQuoteService(SaleMapper mapper, SaleAccess access, Clock clock) {
    this.mapper = mapper;
    this.access = access;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public SaleQuoteVO quote(SaleQuoteRequest request, CurrentUser user) {
    access.require(user, "sale.order.complete");
    var input = SaleInput.lines(request.items());
    var now = LocalDateTime.now(clock);
    var products =
        mapper.quoteProducts(List.copyOf(input.keySet()), now.toLocalDate()).stream()
            .collect(Collectors.toMap(p -> p.id(), Function.identity()));
    if (products.size() != input.size()) throw BusinessException.notFound();
    List<SaleQuoteVO.Line> result = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    long quantity = 0;
    boolean valid = true;
    for (var entry : input.entrySet()) {
      var line = entry.getValue();
      var p = products.get(entry.getKey());
      boolean priceChanged = p.salePrice().compareTo(line.expectedPrice()) != 0;
      String problem = null;
      if (!p.enabled() || !p.canSell()) problem = "商品已停用或禁止销售";
      else if (p.frozen()) problem = "商品盘点冻结，暂不可销售";
      else if (p.saleableQty() < line.quantity())
        problem = "可售库存不足，当前可售" + p.saleableQty() + p.unit();
      else if (priceChanged) problem = "售价已变化，请核对新旧售价后重新确认";
      BigDecimal amount = p.salePrice().multiply(BigDecimal.valueOf(line.quantity()));
      SaleInput.bounded(amount);
      total = total.add(amount);
      quantity += line.quantity();
      valid &= problem == null;
      result.add(
          new SaleQuoteVO.Line(
              p.id().toString(),
              p.code(),
              p.name(),
              p.unit(),
              line.quantity(),
              line.expectedPrice(),
              p.salePrice(),
              amount,
              p.enabled() && p.canSell() ? p.saleableQty() : 0,
              priceChanged,
              problem));
    }
    SaleInput.bounded(total);
    return new SaleQuoteVO(valid, result.size(), quantity, total, now, result);
  }
}
