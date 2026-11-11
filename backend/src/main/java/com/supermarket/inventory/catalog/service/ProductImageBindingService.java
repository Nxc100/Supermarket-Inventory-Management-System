package com.supermarket.inventory.catalog.service;

import com.supermarket.inventory.catalog.mapper.ProductMapper;
import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductImageBindingService {
  private final ProductMapper products;
  private final AuditService audit;

  public ProductImageBindingService(ProductMapper products, AuditService audit) {
    this.products = products;
    this.audit = audit;
  }

  @Transactional
  public String bind(long id, String path, String metadata, CurrentUser user) {
    var old = products.lock(id);
    if (old == null) throw BusinessException.notFound();
    if (products.image(id, path, metadata) != 1) throw BusinessException.notFound();
    audit.record(
        user,
        "PRODUCT_IMAGE_CHANGE",
        "PRODUCT",
        Long.toString(id),
        old.code(),
        path == null ? "移除商品图片" : "更新商品图片");
    return old.imagePath();
  }
}
