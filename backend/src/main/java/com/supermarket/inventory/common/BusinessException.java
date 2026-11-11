package com.supermarket.inventory.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  public BusinessException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }

  public static BusinessException conflict(String message) {
    return new BusinessException(HttpStatus.CONFLICT, "STATE_CONFLICT", message);
  }

  public static BusinessException notFound() {
    return new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "记录不存在或无权访问");
  }
}
