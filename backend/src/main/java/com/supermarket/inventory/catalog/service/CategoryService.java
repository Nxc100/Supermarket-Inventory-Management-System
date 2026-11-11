package com.supermarket.inventory.catalog.service;

import com.supermarket.inventory.catalog.dto.*;
import com.supermarket.inventory.catalog.entity.Category;
import com.supermarket.inventory.catalog.mapper.CategoryMapper;
import com.supermarket.inventory.catalog.vo.CategoryVO;
import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
  private final CategoryMapper mapper;
  private final AuditService audit;

  public CategoryService(CategoryMapper mapper, AuditService audit) {
    this.mapper = mapper;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public PageResult<CategoryVO> page(PageQuery query) {
    return new PageResult<>(
        mapper.page(query), mapper.count(query), query.getPageNum(), query.getPageSize());
  }

  public CategoryVO get(long id) {
    CategoryVO result = mapper.find(id);
    if (result == null) throw BusinessException.notFound();
    return result;
  }

  @Transactional
  public CategoryVO create(CategoryRequest request, CurrentUser user) {
    Category category = new Category();
    category.setCode(request.code());
    category.setName(request.name());
    category.setSortOrder(request.sortOrder());
    if (mapper.insert(category) != 1) throw new IllegalStateException("Category write failed");
    audit.record(
        user, "CATEGORY_CREATE", "CATEGORY", category.getId().toString(), category.getCode(), null);
    return get(category.getId());
  }

  @Transactional
  public CategoryVO update(long id, CategoryRequest request, CurrentUser user) {
    lock(id);
    if (mapper.update(id, request) != 1) throw BusinessException.notFound();
    audit.record(user, "CATEGORY_EDIT", "CATEGORY", Long.toString(id), request.code(), null);
    return get(id);
  }

  @Transactional
  public CategoryVO status(long id, boolean enabled, CurrentUser user) {
    Category category = lock(id);
    if (mapper.status(id, enabled) != 1) throw BusinessException.notFound();
    audit.record(
        user,
        "CATEGORY_STATUS",
        "CATEGORY",
        Long.toString(id),
        category.getCode(),
        enabled ? "启用" : "停用");
    return get(id);
  }

  @Transactional
  public void delete(long id, CurrentUser user) {
    Category category = lock(id);
    if (mapper.references(id) > 0) throw BusinessException.conflict("分类已被商品引用，请使用停用");
    if (mapper.delete(id) != 1) throw BusinessException.notFound();
    audit.record(user, "CATEGORY_DELETE", "CATEGORY", Long.toString(id), category.getCode(), null);
  }

  private Category lock(long id) {
    Category value = mapper.lock(id);
    if (value == null) throw BusinessException.notFound();
    return value;
  }
}
