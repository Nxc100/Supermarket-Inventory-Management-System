package com.supermarket.inventory.catalog.service;

import com.supermarket.inventory.catalog.dto.*;
import com.supermarket.inventory.catalog.entity.*;
import com.supermarket.inventory.catalog.mapper.*;
import com.supermarket.inventory.catalog.vo.*;
import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.service.AuditService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
  private final ProductMapper mapper;
  private final CategoryMapper categories;
  private final AuditService audit;
  private final java.time.Clock clock;

  public ProductService(
      ProductMapper mapper, CategoryMapper categories, AuditService audit, java.time.Clock clock) {
    this.mapper = mapper;
    this.categories = categories;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResult<ProductVO> page(ProductQuery query) {
    validateQuery(query);
    return new PageResult<>(
        mapper.page(query), mapper.count(query), query.getPageNum(), query.getPageSize());
  }

  public ProductVO get(long id) {
    ProductVO value = mapper.find(id);
    if (value == null) throw BusinessException.notFound();
    return value;
  }

  public List<CategoryOptionVO> categoryOptions() {
    return mapper.categoryOptions();
  }

  @Transactional(
      readOnly = true,
      isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
  public ProductDefaultsVO defaults() {
    Long lower = mapper.defaultValue("inventory.default_lower_limit"),
        near = mapper.defaultValue("inventory.default_near_expiry_days");
    if (lower == null || near == null) throw new IllegalStateException("Missing catalog defaults");
    return new ProductDefaultsVO(
        lower, Math.toIntExact(near), List.of("件", "瓶", "袋", "盒", "箱", "包", "罐", "支", "本", "卷"));
  }

  @Transactional(readOnly = true)
  public PageResult<PurchaseProductVO> purchaseLookup(ProductQuery query) {
    validateQuery(query);
    return new PageResult<>(
        mapper.purchaseLookup(query),
        mapper.purchaseLookupCount(query),
        query.getPageNum(),
        query.getPageSize());
  }

  @Transactional(readOnly = true)
  public PageResult<SaleProductVO> saleLookup(ProductQuery query) {
    validateQuery(query);
    return new PageResult<>(
        mapper.saleLookup(query, java.time.LocalDate.now(clock)),
        mapper.saleLookupCount(query),
        query.getPageNum(),
        query.getPageSize());
  }

  @Transactional
  public ProductVO create(ProductRequest request, CurrentUser user) {
    long categoryId = Identifiers.parse(request.categoryId());
    var category = lockCategory(categoryId);
    if (!category.isEnabled()) throw BusinessException.conflict("分类已停用，请选择已启用分类");
    Product product = fields(request, categoryId);
    product.setCreatedBy(user.id());
    if (mapper.create(product) != 1) throw new IllegalStateException("Product insert failed");
    audit.record(
        user, "PRODUCT_CREATE", "PRODUCT", product.getId().toString(), product.getCode(), null);
    return get(product.getId());
  }

  /** Archive edits own no balance, freeze or historical snapshot columns. */
  @Transactional
  public ProductVO update(long id, ProductRequest request, CurrentUser user) {
    long categoryId = Identifiers.parse(request.categoryId());
    var category = lockCategory(categoryId);
    ProductLock old = lock(id);
    boolean referenced = mapper.referenced(id);
    if (referenced
        && (!old.code().equals(request.code())
            || old.categoryId() != categoryId
            || !old.unit().equals(request.unit())))
      throw BusinessException.conflict("商品已有业务引用，编码、分类和基本单位不可修改");
    if (old.categoryId() != categoryId && !category.isEnabled())
      throw BusinessException.conflict("目标分类已停用，请选择已启用分类");
    if (old.managesExpiry() != request.managesExpiry() && (old.stockQty() > 0 || referenced))
      throw BusinessException.conflict("商品已有库存或业务引用，不能切换保质期管理方式");
    Product product = fields(request, categoryId);
    product.setId(id);
    if (mapper.update(product) != 1) throw BusinessException.notFound();
    audit.record(
        user, "PRODUCT_EDIT", "PRODUCT", Long.toString(id), product.getCode(), "档案编辑；历史快照保持原值");
    return get(id);
  }

  @Transactional
  public ProductVO status(long id, boolean enabled, CurrentUser user) {
    ProductLock old = lock(id);
    if (mapper.status(id, enabled) != 1) throw BusinessException.notFound();
    audit.record(
        user,
        "PRODUCT_STATUS_CHANGE",
        "PRODUCT",
        Long.toString(id),
        old.code(),
        enabled ? "启用" : "停用");
    return get(id);
  }

  @Transactional
  public String delete(long id, CurrentUser user) {
    ProductLock old = lock(id);
    if (old.stockQty() != 0 || mapper.referenced(id))
      throw BusinessException.conflict("商品已有库存或业务引用，请使用停用");
    if (mapper.delete(id) != 1) throw BusinessException.notFound();
    audit.record(user, "PRODUCT_DELETE", "PRODUCT", Long.toString(id), old.code(), null);
    return old.imagePath();
  }

  private ProductLock lock(long id) {
    ProductLock old = mapper.lock(id);
    if (old == null) throw BusinessException.notFound();
    return old;
  }

  private Category lockCategory(long id) {
    Category value = categories.lock(id);
    if (value == null) throw BusinessException.notFound();
    return value;
  }

  private void validateQuery(ProductQuery query) {
    if (query.getCategoryId() != null) Identifiers.parse(query.getCategoryId());
  }

  private Product fields(ProductRequest r, long categoryId) {
    Product p = new Product();
    p.setCode(r.code());
    p.setBarcode(r.barcode());
    p.setName(r.name());
    p.setCategoryId(categoryId);
    p.setSpecification(r.specification());
    p.setUnit(r.unit());
    p.setReferenceCost(r.referenceCost());
    p.setSalePrice(r.salePrice());
    p.setManagesExpiry(r.managesExpiry());
    p.setShelfLifeDays(r.shelfLifeDays());
    p.setNearExpiryDays(r.nearExpiryDays());
    p.setStockLowerLimit(r.stockLowerLimit());
    p.setCanPurchase(r.canPurchase());
    p.setCanSell(r.canSell());
    p.setRemark(r.remark());
    return p;
  }
}
