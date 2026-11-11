package com.supermarket.inventory.common;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentNumbers {
  private final Clock clock;

  public DocumentNumbers(Clock clock) {
    this.clock = clock;
  }

  public String next(String prefix) {
    return prefix
        + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE)
        + "-"
        + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
  }
}
