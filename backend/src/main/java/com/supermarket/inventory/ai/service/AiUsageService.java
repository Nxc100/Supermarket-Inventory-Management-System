package com.supermarket.inventory.ai.service;

import com.supermarket.inventory.ai.dto.AiUsageQuery;
import com.supermarket.inventory.ai.mapper.AiUsageMapper;
import com.supermarket.inventory.ai.vo.AiUsageVO;
import com.supermarket.inventory.common.BusinessException;
import com.supermarket.inventory.security.CurrentUser;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AiUsageService {
  private final AiUsageMapper mapper;
  private final AiSettingsService settings;
  private final Clock clock;

  public AiUsageService(AiUsageMapper mapper, AiSettingsService settings, Clock clock) {
    this.mapper = mapper;
    this.settings = settings;
    this.clock = clock;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public AiUsageVO usage(AiUsageQuery query, CurrentUser user) {
    settings.get(user);
    if (query.getEndDate() == null) query.setEndDate(LocalDate.now(clock));
    if (query.getStartDate() == null) query.setStartDate(query.getEndDate().minusDays(29));
    if (query.getStartDate().getYear() < 1
        || query.getEndDate().getYear() > 9998
        || query.getEndDate().isBefore(query.getStartDate())
        || ChronoUnit.DAYS.between(query.getStartDate(), query.getEndDate()) > 365)
      throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "用量查询请选择有效的1～366天区间");
    return new AiUsageVO(
        query.getStartDate(),
        query.getEndDate(),
        LocalDateTime.now(clock),
        mapper.totals(query),
        mapper.models(query),
        mapper.days(query));
  }
}
