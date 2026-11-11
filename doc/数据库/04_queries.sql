-- 开发查询示例：仅查询，不模拟应用登录；参数必须由可信后端确定。
SET NAMES utf8mb4;
SET SESSION time_zone = '+08:00';
USE supermarket_inventory;
SET @actor_id=3; -- buyer01，管理员为1，收银员为5/6
SET @period_end=DATE_ADD(CURRENT_DATE(),INTERVAL 1 DAY);
SET @period_start=DATE_SUB(CURRENT_DATE(),INTERVAL 29 DAY);
SET @analysis_year=YEAR(CURRENT_DATE());

-- Q01 当前用户与角色权限：不要把password_hash返回给前端。
SELECT u.id,u.username,u.real_name,u.phone,u.avatar_path,u.role_code,u.credential_version,r.auth_version,r.permissions,r.data_scope
FROM sys_user u JOIN sys_role r ON r.code=u.role_code WHERE u.id=@actor_id AND u.enabled=1;

-- Q02 权限树和默认业务参数。key为只读目录时不能通过参数编辑接口修改。
SELECT config_key,config_value,description,editable FROM sys_config ORDER BY config_key;

-- Q03 采购：管理员全部，采购仅本人，收银无行；还需校验purchase.order权限。
SELECT o.id,o.order_no,o.supplier_name,o.operator_name,o.total_qty,o.total_amount,o.status,o.reconcile_status,o.created_at
FROM purchase_order o JOIN sys_user u ON u.id=@actor_id AND u.enabled=1
WHERE u.role_code='ADMIN' OR (u.role_code='PURCHASER' AND o.operator_id=u.id) ORDER BY o.created_at DESC,o.id DESC LIMIT 20;

-- Q04 收银库存精简投影（只示例合法收银/管理员，无成本和供应商）。
SELECT i.* FROM v_cashier_inventory i JOIN sys_user u ON u.id=@actor_id AND u.enabled=1
WHERE u.role_code IN ('ADMIN','CASHIER') ORDER BY i.id LIMIT 50;

-- Q05 进货/库存人员完整库存查询；收银角色不能用本查询。
SELECT i.* FROM v_product_inventory i JOIN sys_user u ON u.id=@actor_id AND u.enabled=1
WHERE u.role_code IN ('ADMIN','PURCHASER') ORDER BY i.id;

-- Q06 动态低库存、临期和过期：当天有效，日期推进不需要定时更新表字段。
SELECT id,code,name,saleable_qty,stock_lower_limit,is_frozen FROM v_low_stock ORDER BY id;
SELECT b.batch_no,p.name,b.expiry_date,b.remaining_days,b.stock_qty FROM v_batch_inventory b JOIN product p ON p.id=b.product_id WHERE b.is_near_expiry=1 ORDER BY b.expiry_date,b.batch_id;
SELECT b.batch_no,p.name,b.expiry_date,b.stock_qty FROM v_batch_inventory b JOIN product p ON p.id=b.product_id WHERE b.is_expired=1 AND b.stock_qty>0 ORDER BY b.expiry_date,b.batch_id;

-- Q07 FEFO/FIFO候选批次。真正扣库必须先锁product行，再在同事务中锁这些purchase_item行。
SET @product_id=1;
SELECT i.id,i.batch_no,i.stock_qty,i.expiry_date,o.stocked_at
FROM purchase_item i JOIN purchase_order o ON o.id=i.purchase_id JOIN product p ON p.id=i.product_id
WHERE i.product_id=@product_id AND o.status='IN_STOCK' AND i.stock_qty>0
 AND p.enabled=1 AND p.can_sell=1 AND p.frozen_stocktake_id IS NULL
 AND (i.manages_expiry=0 OR i.expiry_date>=CURRENT_DATE())
ORDER BY CASE WHEN i.expiry_date IS NULL THEN '9999-12-31' ELSE i.expiry_date END,o.stocked_at,i.id;

-- Q08 月度销售柱状图：始终12个月；按照业务事件计正负，不以最终状态去掉原事件。
WITH RECURSIVE months AS (SELECT 1 month_no UNION ALL SELECT month_no+1 FROM months WHERE month_no<12), totals AS (
 SELECT MONTH(event_at) month_no,SUM(signed_qty) qty,SUM(signed_amount) amount FROM v_sale_event
 WHERE event_at>=MAKEDATE(@analysis_year,1) AND event_at<MAKEDATE(@analysis_year+1,1) GROUP BY MONTH(event_at)
)
SELECT m.month_no,COALESCE(t.qty,0) net_qty,COALESCE(t.amount,0) net_amount FROM months m LEFT JOIN totals t ON t.month_no=m.month_no ORDER BY m.month_no;

-- Q09 热销前十，统计页入口必须有statistics.sales权限，采购员不得借此打开完整销售分析。
SELECT product_id,MAX(product_name) product_name,SUM(signed_qty) net_qty,SUM(signed_amount) net_amount
FROM v_sale_event WHERE event_at>=@period_start AND event_at<@period_end
GROUP BY product_id ORDER BY net_qty DESC,net_amount DESC,product_id LIMIT 10;

-- Q10 滞销：默认含今天的30个自然日；新近入库列为观察不足。
SELECT i.id,i.name,i.saleable_qty,i.first_stocked_at,COALESCE(s.net_qty,0) period_net_qty,
 CASE WHEN DATE(i.first_stocked_at)>DATE(@period_start) THEN 'OBSERVATION_INSUFFICIENT' ELSE 'SLOW_MOVING' END observation
FROM v_product_inventory i LEFT JOIN (
 SELECT product_id,SUM(signed_qty) net_qty FROM v_sale_event WHERE event_at>=@period_start AND event_at<@period_end GROUP BY product_id
) s ON s.product_id=i.id
WHERE i.enabled=1 AND i.can_sell=1 AND i.saleable_qty>0 AND COALESCE(s.net_qty,0)=0 ORDER BY i.id;

-- Q11 分类账面库存占比：不是金额、重量或体积占比；总数0时比例为NULL。
SELECT category_id,category_name,book_qty,ROUND(100*book_qty/NULLIF(SUM(book_qty) OVER (),0),2) percentage FROM v_category_inventory ORDER BY category_id;

-- Q12 销售单据的本人范围不会被全店统计权限扩大。
SELECT s.id,s.order_no,s.cashier_name,s.total_amount,s.status,s.completed_at,s.corrected_at
FROM sale_order s JOIN sys_user u ON u.id=@actor_id AND u.enabled=1
WHERE u.role_code='ADMIN' OR (u.role_code='CASHIER' AND s.cashier_id=u.id) ORDER BY s.completed_at DESC,s.id DESC;

-- Q13 库存优化摘要的商品聚合销量是允许输入，不开放销售单明细。
SELECT i.id,i.code,i.name,i.category_name,i.book_qty,i.saleable_qty,i.stock_lower_limit,i.near_batch_count,i.expired_batch_count,
       COALESCE(s.net_qty,0) recent_net_qty,i.last_stocked_at
FROM v_product_inventory i LEFT JOIN (
 SELECT product_id,SUM(signed_qty) net_qty FROM v_sale_event WHERE event_at>=@period_start AND event_at<@period_end GROUP BY product_id
) s ON s.product_id=i.id ORDER BY i.id;

-- Q14 AI历史授权：采购可读共享库存成功记录以及自己的库存失败记录；收银无记录。
SELECT a.id,a.suggestion_type,a.status,a.is_fixture,a.provider,a.model_name,a.snapshot_at,a.content,a.error_message
FROM ai_suggestion a JOIN sys_user u ON u.id=@actor_id AND u.enabled=1
WHERE u.role_code='ADMIN' OR (u.role_code='PURCHASER' AND a.suggestion_type='INVENTORY' AND (a.status='SUCCESS' OR a.operator_id=u.id))
ORDER BY a.requested_at DESC,a.id DESC;

-- Q15 对账历史：服务端需先校验对象采购的owner，不直接放开整个历史视图。
SELECT h.* FROM v_reconcile_history h JOIN purchase_order o ON o.id=CAST(h.purchase_id AS UNSIGNED)
JOIN sys_user u ON u.id=@actor_id AND u.enabled=1
WHERE u.role_code='ADMIN' OR (u.role_code='PURCHASER' AND o.operator_id=u.id) ORDER BY h.occurred_at,h.id;

-- Q16 当前活动冻结与任务状态。
SELECT p.id,p.name,p.frozen_stocktake_id,s.order_no,s.status,s.operator_name,s.started_at
FROM product p JOIN stocktake_order s ON s.id=p.frozen_stocktake_id ORDER BY s.started_at,p.id;

-- Q17 销售幂等结果查询；仅返回实际收银员本人的提交结果，管理员按功能权限可查询。
SET @request_key='seed-sale-request-4';
SELECT id,order_no,total_amount,status,completed_at FROM sale_order WHERE request_key=@request_key AND cashier_id=@actor_id;

-- 事务协议（以下是说明，不会被本文件自动执行）：
-- 1 BEGIN；验证用户和权限；单据用SELECT ... FOR UPDATE锁定并校验合法状态。
-- 2 按product.id升序SELECT ... FOR UPDATE，验证frozen_stocktake_id、enabled与业务开关。
-- 3 再按统一顺序锁采购批次；重查日期、单价、剩余数，服务器计算批次分配及金额。
-- 4 更新purchase_item.stock_qty与product.stock_qty、stock_version，写stock_movement及单据。
-- 5 所有步骤成功COMMIT，任何异常ROLLBACK。UPDATE余额必须附stock_qty>=扣减量条件并检查行数。
-- 6 盘点开始也先锁相同商品行，设置frozen_stocktake_id并保存book_qty；完成/关闭同事务释放。
-- 7 销售幂等key冲突后查询原单，验证cashier和request_hash相同才能返回原结果。
-- 8 修改管理员账号/授权先锁sys_role的ADMIN行，再检查至少一个启用管理员及关键权限。
-- 9 成功/撤回对账时，锁采购单，将当前单头及逐行核验快照写operation_log再修改当前字段。
-- 10 主档编辑不允许更新库存余额；已生效快照、批次来源与流水均不允许通用保存接口任意改写。
