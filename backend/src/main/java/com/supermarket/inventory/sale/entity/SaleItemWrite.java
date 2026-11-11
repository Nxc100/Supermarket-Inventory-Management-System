package com.supermarket.inventory.sale.entity;

import com.supermarket.inventory.inventory.entity.StockProduct;
import java.math.BigDecimal;

public class SaleItemWrite {
  private final long saleId;
  private final int lineNo, quantity;
  private final StockProduct product;
  private final String categoryName;
  private final BigDecimal unitPrice;

  public SaleItemWrite(
      long saleId, int lineNo, int quantity, StockProduct product, String categoryName) {
    this.saleId = saleId;
    this.lineNo = lineNo;
    this.quantity = quantity;
    this.product = product;
    this.categoryName = categoryName;
    this.unitPrice = product.getSalePrice();
  }

  public long getSaleId() {
    return saleId;
  }

  public int getLineNo() {
    return lineNo;
  }

  public int getQuantity() {
    return quantity;
  }

  public StockProduct getProduct() {
    return product;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }
}
