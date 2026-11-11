package com.supermarket.inventory.ai.service;

import com.supermarket.inventory.ai.entity.SuggestionRecord;
import com.supermarket.inventory.ai.enums.*;
import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.security.CurrentUser;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class SuggestionAccess {
  public List<SuggestionType> allowed(CurrentUser user) {
    var types = new ArrayList<SuggestionType>();
    if (Set.of("ADMIN", "PURCHASER").contains(user.roleCode())
        && user.permissions().contains("ai.inventory")) types.add(SuggestionType.INVENTORY);
    if ("ADMIN".equals(user.roleCode()) && user.permissions().contains("ai.sales"))
      types.add(SuggestionType.SALES);
    if (types.isEmpty()) throw new AccessDeniedException("Suggestions denied");
    return types;
  }

  public void require(CurrentUser user, SuggestionType type) {
    if (!allowed(user).contains(type)) throw new AccessDeniedException("Suggestion type denied");
  }

  public void read(CurrentUser user, SuggestionRecord record) {
    if (record == null) throw BusinessException.notFound();
    require(user, record.suggestionType());
    if (!"ADMIN".equals(user.roleCode())
        && record.status() != SuggestionStatus.SUCCESS
        && record.operatorId() != user.id()) throw BusinessException.notFound();
  }

  public void own(CurrentUser user, SuggestionRecord record) {
    read(user, record);
    if (record.operatorId() != user.id()) throw BusinessException.notFound();
  }
}
