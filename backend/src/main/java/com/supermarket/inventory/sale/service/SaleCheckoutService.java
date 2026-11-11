package com.supermarket.inventory.sale.service;

import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.sale.dto.SaleCompleteRequest;
import com.supermarket.inventory.sale.mapper.SaleMapper;
import com.supermarket.inventory.sale.vo.SaleDetailVO;
import com.supermarket.inventory.security.CurrentUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Unique-key recovery happens only after the writing transaction has exited and rolled back. */
@Service
public class SaleCheckoutService {
  private final SaleMapper mapper;
  private final SaleAccess access;
  private final SaleCommandService command;
  private final SaleQueryService queries;

  public SaleCheckoutService(
      SaleMapper mapper, SaleAccess access, SaleCommandService command, SaleQueryService queries) {
    this.mapper = mapper;
    this.access = access;
    this.command = command;
    this.queries = queries;
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public SaleDetailVO complete(SaleCompleteRequest request, CurrentUser user) {
    access.require(user, "sale.order.complete");
    String hash = SaleInput.hash(request, user);
    var old = mapper.byKey(request.requestKey());
    if (old != null) {
      access.sameRequest(old, user, hash);
      return queries.detail(old.id(), user);
    }
    try {
      return command.complete(request, hash, user);
    } catch (DuplicateKeyException duplicate) {
      var committed = mapper.byKey(request.requestKey());
      if (committed == null) throw BusinessException.conflict("销售未完成，已回滚，请查询本次结果后重试");
      access.sameRequest(committed, user, hash);
      return queries.detail(committed.id(), user);
    }
  }
}
