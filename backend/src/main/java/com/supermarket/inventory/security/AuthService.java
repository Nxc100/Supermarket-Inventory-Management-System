package com.supermarket.inventory.security;

import com.supermarket.inventory.common.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final AccountMapper accounts;
  private final AccountPrincipalService principals;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final Clock clock;
  private final Duration lifetime;
  private final String dummyHash;

  public AuthService(
      AccountMapper accounts,
      AccountPrincipalService principals,
      PasswordEncoder passwords,
      JwtEncoder encoder,
      Clock clock,
      @Value("${app.token-lifetime}") Duration lifetime) {
    this.accounts = accounts;
    this.principals = principals;
    this.passwords = passwords;
    this.encoder = encoder;
    this.clock = clock;
    this.lifetime = lifetime;
    dummyHash = passwords.encode(java.util.UUID.randomUUID().toString());
  }

  public record LoginResult(String token, UserVO user) {}

  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public LoginResult login(LoginRequest request) {
    SessionAccount account = accounts.findByUsername(request.username());
    boolean correct =
        passwords.matches(request.password(), account == null ? dummyHash : account.passwordHash());
    if (account == null || !correct)
      throw new BusinessException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "账号或密码错误");
    if (!account.enabled())
      throw new BusinessException(HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED", "账号已停用，请联系管理员");
    account = accounts.lock(account.id());
    if (!account.enabled() || !passwords.matches(request.password(), account.passwordHash()))
      throw new BusinessException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "账号或密码错误");
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(account.id().toString())
            .issuedAt(now)
            .expiresAt(now.plus(lifetime))
            .claim("cv", account.credentialVersion())
            .claim("rv", account.authVersion())
            .claim("role", account.roleCode())
            .build();
    String token =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    if (accounts.login(account.id(), LocalDateTime.now(clock)) != 1)
      throw BusinessException.notFound();
    return new LoginResult(token, UserVO.from(account, principals.principal(account)));
  }

  public UserVO me(long id) {
    SessionAccount account = accounts.findById(id);
    if (account == null) throw BusinessException.notFound();
    return UserVO.from(account, principals.principal(account));
  }

  @Transactional
  public void logout(long id) {
    if (accounts.invalidate(id) != 1) throw BusinessException.notFound();
  }
}
