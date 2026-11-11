package com.supermarket.inventory.catalog.service;

import com.supermarket.inventory.catalog.dto.*;
import com.supermarket.inventory.catalog.entity.Supplier;
import com.supermarket.inventory.catalog.mapper.SupplierMapper;
import com.supermarket.inventory.catalog.vo.SupplierVO;
import com.supermarket.inventory.common.*;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.system.service.AuditService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {
  private final SupplierMapper mapper;
  private final AuditService audit;

  public SupplierService(SupplierMapper mapper, AuditService audit) {
    this.mapper = mapper;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public PageResult<SupplierVO> page(SupplierQuery query) {
    return new PageResult<>(
        mapper.page(query), mapper.count(query), query.getPageNum(), query.getPageSize());
  }

  public SupplierVO get(long id) {
    SupplierVO value = mapper.find(id);
    if (value == null) throw BusinessException.notFound();
    return value;
  }

  @Transactional
  public SupplierVO create(SupplierRequest request, CurrentUser user) {
    Supplier value = fields(request);
    value.setCode("SUP" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
    value.setCreatedBy(user.id());
    if (mapper.create(value) != 1) throw new IllegalStateException("Supplier insert failed");
    audit.record(
        user, "SUPPLIER_CREATE", "SUPPLIER", value.getId().toString(), value.getCode(), null);
    return get(value.getId());
  }

  @Transactional
  public SupplierVO update(long id, SupplierRequest request, CurrentUser user) {
    Supplier old = lock(id);
    Supplier value = fields(request);
    value.setId(id);
    if (mapper.update(value) != 1) throw BusinessException.notFound();
    audit.record(
        user, "SUPPLIER_EDIT", "SUPPLIER", Long.toString(id), old.getCode(), "更新当前档案，保留历史单据快照");
    return get(id);
  }

  @Transactional
  public SupplierVO status(long id, boolean enabled, CurrentUser user) {
    Supplier old = lock(id);
    if (mapper.status(id, enabled) != 1) throw BusinessException.notFound();
    audit.record(
        user,
        "SUPPLIER_STATUS_CHANGE",
        "SUPPLIER",
        Long.toString(id),
        old.getCode(),
        enabled ? "启用" : "停用");
    return get(id);
  }

  @Transactional
  public void delete(long id, CurrentUser user) {
    Supplier old = lock(id);
    if (mapper.references(id) > 0) throw BusinessException.conflict("供应商已有采购引用，请使用停用");
    if (mapper.delete(id) != 1) throw BusinessException.notFound();
    audit.record(user, "SUPPLIER_DELETE", "SUPPLIER", Long.toString(id), old.getCode(), null);
  }

  private Supplier lock(long id) {
    Supplier value = mapper.lock(id);
    if (value == null) throw BusinessException.notFound();
    return value;
  }

  private Supplier fields(SupplierRequest r) {
    Supplier s = new Supplier();
    s.setName(r.name());
    s.setContactName(r.contactName());
    s.setContactPhone(r.contactPhone());
    s.setAddress(r.address());
    s.setRemark(r.remark());
    return s;
  }
}
