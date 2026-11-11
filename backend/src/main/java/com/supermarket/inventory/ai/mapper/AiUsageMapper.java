package com.supermarket.inventory.ai.mapper;

import com.supermarket.inventory.ai.dto.AiUsageQuery;
import com.supermarket.inventory.ai.vo.AiUsageVO.UsageRow;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AiUsageMapper {
  String METRICS =
      "COUNT(*) calls,COALESCE(SUM(status='SUCCESS'),0) successes,COALESCE(SUM(status='FAILURE'),0) failures,COALESCE(SUM(status='PENDING'),0) pending,COUNT(total_tokens) reported_calls,COUNT(*)-COUNT(total_tokens) unreported_calls,COALESCE(SUM(prompt_tokens),0) prompt_tokens,COALESCE(SUM(completion_tokens),0) completion_tokens,COALESCE(SUM(total_tokens),0) total_tokens,COALESCE(SUM(cached_tokens),0) cached_tokens,COUNT(cached_tokens) cache_reported_calls";
  String FILTER =
      " FROM ai_suggestion WHERE is_fixture=0 AND requested_at &gt;= #{startDate} AND requested_at &lt; DATE_ADD(#{endDate},INTERVAL 1 DAY)<if test='model!=null'> AND model_name=#{model}</if><if test='type!=null'> AND suggestion_type=#{type}</if> ";

  @Select("<script>SELECT 'TOTAL' label," + METRICS + FILTER + "</script>")
  UsageRow totals(AiUsageQuery query);

  @Select(
      "<script>SELECT model_name label,"
          + METRICS
          + FILTER
          + " GROUP BY model_name ORDER BY calls DESC,model_name</script>")
  List<UsageRow> models(AiUsageQuery query);

  @Select(
      "<script>SELECT CAST(DATE(requested_at) AS CHAR) label,"
          + METRICS
          + FILTER
          + " GROUP BY DATE(requested_at) ORDER BY DATE(requested_at) DESC</script>")
  List<UsageRow> days(AiUsageQuery query);
}
