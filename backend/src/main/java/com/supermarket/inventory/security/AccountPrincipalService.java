package com.supermarket.inventory.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountPrincipalService {
  private final ObjectMapper json;

  public AccountPrincipalService(ObjectMapper json) {
    this.json = json;
  }

  public CurrentUser principal(SessionAccount account) {
    try {
      List<String> permissions =
          json.readValue(account.permissionsJson(), new TypeReference<>() {});
      return new CurrentUser(
          account.id(),
          account.username(),
          account.realName(),
          account.roleCode(),
          account.credentialVersion(),
          account.authVersion(),
          List.copyOf(permissions));
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Invalid persisted permission set", e);
    }
  }
}
