package com.supermarket.inventory.profile.service;

import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.profile.dto.*;
import com.supermarket.inventory.security.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
  private final AccountMapper accounts;
  private final AuthService auth;
  private final PasswordEncoder passwords;

  public ProfileService(AccountMapper accounts, AuthService auth, PasswordEncoder passwords) {
    this.accounts = accounts;
    this.auth = auth;
    this.passwords = passwords;
  }

  @Transactional
  public UserVO update(long id, ProfileUpdateRequest request) {
    if (accounts.updateProfile(id, request.realName(), request.phone()) != 1)
      throw BusinessException.notFound();
    return auth.me(id);
  }

  @Transactional
  public void changePassword(long id, PasswordChangeRequest request) {
    SessionAccount account = accounts.lock(id);
    if (account == null) throw BusinessException.notFound();
    if (!passwords.matches(request.oldPassword(), account.passwordHash()))
      throw new BusinessException(HttpStatus.BAD_REQUEST, "OLD_PASSWORD_INCORRECT", "旧密码错误");
    if (accounts.password(id, passwords.encode(request.newPassword())) != 1)
      throw BusinessException.notFound();
  }
}
