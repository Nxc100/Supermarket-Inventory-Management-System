package com.supermarket.inventory.system.mapper;

import com.supermarket.inventory.security.CurrentUser;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AuditMapper {
  @Insert(
      "INSERT INTO operation_log(actor_id,actor_name,action,target_type,target_id,target_no,result,reason,before_data,after_data,request_id,occurred_at) VALUES(#{user.id},#{user.realName},#{action},#{type},#{id},#{number},'SUCCESS',#{reason},#{before},#{after},#{trace},#{now})")
  int snapshot(
      @Param("user") CurrentUser user,
      @Param("action") String action,
      @Param("type") String type,
      @Param("id") String id,
      @Param("number") String number,
      @Param("reason") String reason,
      @Param("before") String before,
      @Param("after") String after,
      @Param("trace") String trace,
      @Param("now") LocalDateTime now);

  @Insert(
      "INSERT INTO operation_log(actor_id,actor_name,action,target_type,target_id,target_no,result,reason,request_id,occurred_at) VALUES(#{user.id},#{user.realName},#{action},#{type},#{id},#{number},#{result},#{reason},#{trace},#{now})")
  int outcome(
      @Param("user") CurrentUser user,
      @Param("action") String action,
      @Param("type") String type,
      @Param("id") String id,
      @Param("number") String number,
      @Param("result") String result,
      @Param("reason") String reason,
      @Param("trace") String trace,
      @Param("now") LocalDateTime now);

  @Insert(
      "INSERT INTO operation_log(actor_id,actor_name,action,target_type,target_id,target_no,result,reason,before_data,after_data,request_id,occurred_at) VALUES(#{user.id},#{user.realName},#{action},'PURCHASE',#{id},#{number},'SUCCESS',#{reason},#{before},#{after},#{trace},#{now})")
  int purchaseSnapshot(
      @Param("user") CurrentUser user,
      @Param("action") String action,
      @Param("id") String id,
      @Param("number") String number,
      @Param("reason") String reason,
      @Param("before") String before,
      @Param("after") String after,
      @Param("trace") String trace,
      @Param("now") LocalDateTime now);

  @Insert(
      "INSERT INTO operation_log(actor_id,actor_name,action,target_type,target_id,target_no,result,reason,request_id,occurred_at) VALUES(#{user.id},#{user.realName},#{action},#{type},#{id},#{number},'SUCCESS',#{reason},#{trace},#{now})")
  int append(
      @Param("user") CurrentUser user,
      @Param("action") String action,
      @Param("type") String type,
      @Param("id") String id,
      @Param("number") String number,
      @Param("reason") String reason,
      @Param("trace") String trace,
      @Param("now") LocalDateTime now);
}
