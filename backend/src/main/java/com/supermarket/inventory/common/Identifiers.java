package com.supermarket.inventory.common;

import org.springframework.http.HttpStatus;

public final class Identifiers {
  private Identifiers() {}

  public static long parse(String value) {
    try {
      long id = Long.parseLong(value);
      if (id > 0) return id;
    } catch (NumberFormatException ignored) {
    }
    throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "ID必须为正整数的十进制字符串");
  }
}
