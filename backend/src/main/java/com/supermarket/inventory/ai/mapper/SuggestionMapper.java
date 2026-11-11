package com.supermarket.inventory.ai.mapper;

import com.supermarket.inventory.ai.client.SuggestionClient;
import com.supermarket.inventory.ai.dto.SuggestionQuery;
import com.supermarket.inventory.ai.entity.*;
import com.supermarket.inventory.ai.enums.SuggestionType;
import com.supermarket.inventory.ai.vo.SuggestionSummaryVO;
import com.supermarket.inventory.security.CurrentUser;
import com.supermarket.inventory.statistics.dto.SalesStatisticsQuery;
import java.time.*;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SuggestionMapper {
  @Select("SELECT CAST(id AS CHAR) id,code,name,enabled FROM category ORDER BY sort_order,id")
  List<com.supermarket.inventory.catalog.vo.CategoryOptionVO> categoryOptions();

  List<com.supermarket.inventory.ai.vo.SuggestionProductOptionVO> productOptions(
      com.supermarket.inventory.ai.dto.SuggestionProductQuery query);

  long productOptionCount(com.supermarket.inventory.ai.dto.SuggestionProductQuery query);

  SuggestionRecord find(long id);

  SuggestionRecord byKey(String key);

  int insertPending(
      @Param("p") PreparedSuggestion p,
      @Param("user") CurrentUser user,
      @Param("identity") SuggestionClient.Identity identity,
      @Param("now") LocalDateTime now);

  int finish(
      @Param("id") long id,
      @Param("content") String content,
      @Param("code") String code,
      @Param("message") String message,
      @Param("identity") SuggestionClient.Identity identity,
      @Param("usage") com.supermarket.inventory.ai.client.TokenUsage usage,
      @Param("duration") int duration,
      @Param("now") LocalDateTime now);

  List<SuggestionSummaryVO> page(
      @Param("q") SuggestionQuery query,
      @Param("user") CurrentUser user,
      @Param("types") List<SuggestionType> types);

  long count(
      @Param("q") SuggestionQuery query,
      @Param("user") CurrentUser user,
      @Param("types") List<SuggestionType> types);

  List<InventorySummaryItem> inventorySummary(
      @Param("q") SalesStatisticsQuery q, @Param("today") LocalDate today);

  boolean hasReceipt(@Param("q") SalesStatisticsQuery q, @Param("today") LocalDate today);
}
