package com.supermarket.inventory.system.vo;

import java.time.*;
import java.util.*;

public record DashboardVO(
    LocalDate businessDate,
    LocalDateTime collectedAt,
    String scopeNote,
    List<Metric> metrics,
    List<Shortcut> shortcuts) {
  public record Metric(
      String code,
      String label,
      String value,
      String unit,
      String note,
      String path,
      Map<String, String> query) {}

  public record Shortcut(String label, String path, Map<String, String> query) {}
}
