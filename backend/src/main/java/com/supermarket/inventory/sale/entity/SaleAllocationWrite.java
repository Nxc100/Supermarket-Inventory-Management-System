package com.supermarket.inventory.sale.entity;

public class SaleAllocationWrite {
  private final long saleItemId, productId, batchItemId;
  private final int quantity;

  public SaleAllocationWrite(long saleItemId, long productId, long batchItemId, int quantity) {
    this.saleItemId = saleItemId;
    this.productId = productId;
    this.batchItemId = batchItemId;
    this.quantity = quantity;
  }

  public long getSaleItemId() {
    return saleItemId;
  }

  public long getProductId() {
    return productId;
  }

  public long getBatchItemId() {
    return batchItemId;
  }

  public int getQuantity() {
    return quantity;
  }
}
