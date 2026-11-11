package com.supermarket.inventory.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.supermarket.inventory.common.ApiResponse;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  JwtEncoder encoder(@Value("${app.jwt-secret}") String secret) {
    validateSecret(secret);
    return new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
  }

  @Bean
  JwtDecoder decoder(@Value("${app.jwt-secret}") String secret, java.time.Clock clock) {
    validateSecret(secret);
    var decoder =
        NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    var timestamps = new JwtTimestampValidator();
    timestamps.setClock(clock);
    decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(timestamps));
    return decoder;
  }

  private void validateSecret(String secret) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
      throw new IllegalArgumentException("JWT_SECRET requires at least 32 bytes");
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      AccountMapper accounts,
      AccountPrincipalService principals,
      ObjectMapper json)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC)
                    .permitAll()
                    .requestMatchers(
                        "/api/v1/auth/login",
                        "/media/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (req, res, e) -> {
                          res.setStatus(401);
                          res.setContentType("application/json;charset=UTF-8");
                          json.writeValue(
                              res.getOutputStream(),
                              ApiResponse.error("UNAUTHORIZED", "登录已失效，请重新登录"));
                        })
                    .accessDeniedHandler(
                        (req, res, e) -> {
                          res.setStatus(403);
                          res.setContentType("application/json;charset=UTF-8");
                          json.writeValue(
                              res.getOutputStream(), ApiResponse.error("FORBIDDEN", "无权执行此操作"));
                        }))
        .oauth2ResourceServer(
            oauth ->
                oauth
                    .authenticationEntryPoint(
                        (req, res, e) -> {
                          res.setStatus(401);
                          res.setContentType("application/json;charset=UTF-8");
                          json.writeValue(
                              res.getOutputStream(),
                              ApiResponse.error("UNAUTHORIZED", "登录已失效，请重新登录"));
                        })
                    .jwt(
                        jwt ->
                            jwt.jwtAuthenticationConverter(
                                token -> {
                                  SessionAccount account;
                                  try {
                                    account = accounts.findById(Long.parseLong(token.getSubject()));
                                    if (account == null
                                        || !account.enabled()
                                        || account.credentialVersion()
                                            != ((Number) token.getClaim("cv")).longValue()
                                        || account.authVersion()
                                            != ((Number) token.getClaim("rv")).longValue()
                                        || !account
                                            .roleCode()
                                            .equals(token.getClaimAsString("role"))) {
                                      throw new BadCredentialsException("Session revoked");
                                    }
                                  } catch (NumberFormatException
                                      | NullPointerException
                                      | ClassCastException e) {
                                    throw new BadCredentialsException(
                                        "Invalid credential claims", e);
                                  }
                                  CurrentUser principal = principals.principal(account);
                                  return new UsernamePasswordAuthenticationToken(
                                      principal,
                                      null,
                                      principal.permissions().stream()
                                          .map(SimpleGrantedAuthority::new)
                                          .toList());
                                })));
    return http.build();
  }
}
