package com.supermarket.inventory.sale.service;

import com.supermarket.inventory.common.*;
import com.supermarket.inventory.sale.dto.*;
import com.supermarket.inventory.security.CurrentUser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.http.HttpStatus;

public final class SaleInput {
  public static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999999999.99");

  private SaleInput() {}

  public static SortedMap<Long, SaleLineRequest> lines(List<SaleLineRequest> items) {
    if (items == null || items.isEmpty() || items.size() > 100) throw bad("销售必须有1～100种商品");
    SortedMap<Long, SaleLineRequest> result = new TreeMap<>();
    for (var item : items) {
      if (result.put(Identifiers.parse(item.productId()), item) != null)
        throw bad("同商品请合并数量，不得重复传入明细");
    }
    return result;
  }

  public static String hash(SaleCompleteRequest request, CurrentUser user) {
    StringBuilder s =
        new StringBuilder("sale-v1|")
            .append(user.id())
            .append('|')
            .append(request.paymentMethod())
            .append('|')
            .append(request.receivedAmount().setScale(2).toPlainString());
    lines(request.items())
        .forEach(
            (id, line) ->
                s.append('|')
                    .append(id)
                    .append(':')
                    .append(line.quantity())
                    .append(':')
                    .append(line.expectedPrice().setScale(2).toPlainString()));
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(s.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void bounded(BigDecimal value) {
    if (value.compareTo(MAX_AMOUNT) > 0) throw bad("销售金额超出允许范围");
  }

  public static BusinessException bad(String message) {
    return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
  }
}
