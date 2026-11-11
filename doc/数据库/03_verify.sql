-- 数据一致性校验。每个检查项 violations 必须为0；只读，可重复执行。
-- 这验证数据库结构与夹具，不代表前后端、文件上传、真实API、并发服务已完成。
SET NAMES utf8mb4;
SET SESSION time_zone = '+08:00';
USE supermarket_inventory;

WITH checks AS (
SELECT 'V01_product_equals_batches' check_name,COUNT(*) violations FROM v_product_inventory WHERE book_qty<>batch_qty
UNION ALL
SELECT 'V02_batch_equals_ledger',COUNT(*) FROM purchase_item i LEFT JOIN (
 SELECT batch_item_id,SUM(delta_qty) qty FROM stock_movement GROUP BY batch_item_id
) m ON m.batch_item_id=i.id WHERE i.batch_no IS NOT NULL AND i.stock_qty<>COALESCE(m.qty,0)
UNION ALL
SELECT 'V03_product_equals_ledger',COUNT(*) FROM product p LEFT JOIN (
 SELECT product_id,SUM(delta_qty) qty FROM stock_movement GROUP BY product_id
) m ON m.product_id=p.id WHERE p.stock_qty<>COALESCE(m.qty,0)
UNION ALL
SELECT 'V04_batch_ledger_chain',COUNT(*) FROM (
 SELECT batch_before,COALESCE(LAG(batch_after) OVER (PARTITION BY batch_item_id ORDER BY occurred_at,id),0) previous_qty FROM stock_movement
) t WHERE batch_before<>previous_qty
UNION ALL
SELECT 'V05_product_ledger_chain',COUNT(*) FROM (
 SELECT product_before,COALESCE(LAG(product_after) OVER (PARTITION BY product_id ORDER BY occurred_at,id),0) previous_qty FROM stock_movement
) t WHERE product_before<>previous_qty
UNION ALL
SELECT 'V06_purchase_header_totals',COUNT(*) FROM purchase_order o LEFT JOIN (
 SELECT purchase_id,SUM(quantity) qty,SUM(COALESCE(received_qty,0)) received,SUM(line_amount) amount FROM purchase_item GROUP BY purchase_id
) i ON i.purchase_id=o.id WHERE i.purchase_id IS NULL OR o.total_qty<>i.qty OR o.total_received_qty<>i.received OR o.total_amount<>i.amount
UNION ALL
SELECT 'V07_posted_batch_integrity',COUNT(*) FROM purchase_item i JOIN purchase_order o ON o.id=i.purchase_id
WHERE (o.status IN ('IN_STOCK','REVERSED') AND (i.batch_no IS NULL OR i.stock_qty IS NULL OR i.received_qty<>i.quantity))
   OR (o.status NOT IN ('IN_STOCK','REVERSED') AND (i.batch_no IS NOT NULL OR i.stock_qty IS NOT NULL))
   OR (o.status='REVERSED' AND i.stock_qty<>0)
UNION ALL
SELECT 'V08_historical_receipt_dates',COUNT(*) FROM purchase_item i JOIN purchase_order o ON o.id=i.purchase_id
WHERE o.status IN ('IN_STOCK','REVERSED') AND i.manages_expiry=1 AND
 (i.production_date IS NULL OR i.expiry_date IS NULL OR i.production_date>DATE(o.stocked_at) OR i.expiry_date<DATE(o.stocked_at))
UNION ALL
SELECT 'V09_purchase_in_events',COUNT(*) FROM purchase_item i JOIN purchase_order o ON o.id=i.purchase_id
LEFT JOIN stock_movement m ON m.batch_item_id=i.id AND m.event_type='PURCHASE_IN'
WHERE o.status IN ('IN_STOCK','REVERSED') AND (m.id IS NULL OR m.delta_qty<>i.received_qty OR m.occurred_at<>o.stocked_at)
UNION ALL
SELECT 'V10_purchase_reversal_events',COUNT(*) FROM purchase_item i JOIN purchase_order o ON o.id=i.purchase_id
LEFT JOIN stock_movement m ON m.batch_item_id=i.id AND m.event_type='PURCHASE_REVERSE'
WHERE (o.status='REVERSED' AND (m.id IS NULL OR m.delta_qty<>-i.received_qty OR m.occurred_at<>o.reversed_at)) OR (o.status<>'REVERSED' AND m.id IS NOT NULL)
UNION ALL
SELECT 'V11_sale_header_totals',COUNT(*) FROM sale_order o LEFT JOIN (
 SELECT sale_id,SUM(quantity) qty,SUM(line_amount) amount FROM sale_item GROUP BY sale_id
) i ON i.sale_id=o.id WHERE i.sale_id IS NULL OR o.total_qty<>i.qty OR o.total_amount<>i.amount
UNION ALL
SELECT 'V12_sale_allocation_totals',COUNT(*) FROM sale_item i LEFT JOIN (
 SELECT sale_item_id,SUM(quantity) qty FROM sale_allocation GROUP BY sale_item_id
) a ON a.sale_item_id=i.id WHERE a.sale_item_id IS NULL OR a.qty<>i.quantity
UNION ALL
SELECT 'V13_sale_out_events',COUNT(*) FROM sale_allocation a JOIN sale_item i ON i.id=a.sale_item_id JOIN sale_order o ON o.id=i.sale_id
LEFT JOIN stock_movement m ON m.sale_allocation_id=a.id AND m.event_type='SALE_OUT'
WHERE m.id IS NULL OR m.delta_qty<>-a.quantity OR m.occurred_at<>o.completed_at
UNION ALL
SELECT 'V14_sale_correction_events',COUNT(*) FROM sale_allocation a JOIN sale_item i ON i.id=a.sale_item_id JOIN sale_order o ON o.id=i.sale_id
LEFT JOIN stock_movement m ON m.sale_allocation_id=a.id AND m.event_type='SALE_CORRECTION'
WHERE (o.status='CORRECTED' AND (m.id IS NULL OR m.delta_qty<>a.quantity OR m.occurred_at<>o.corrected_at)) OR (o.status='COMPLETED' AND m.id IS NOT NULL)
UNION ALL
SELECT 'V15_sale_valid_at_event_time',COUNT(*) FROM sale_allocation a JOIN sale_item s ON s.id=a.sale_item_id JOIN sale_order o ON o.id=s.sale_id
JOIN purchase_item b ON b.id=a.batch_item_id JOIN purchase_order p ON p.id=b.purchase_id
WHERE p.stocked_at>o.completed_at OR (b.manages_expiry=1 AND b.expiry_date<DATE(o.completed_at))
UNION ALL
SELECT 'V16_current_reconciliation',COUNT(*) FROM purchase_order o WHERE o.reconcile_status='MATCHED' AND (
 o.total_amount<>o.external_total_amount OR EXISTS (SELECT 1 FROM purchase_item i WHERE i.purchase_id=o.id AND
 (i.external_qty IS NULL OR i.external_unit_cost IS NULL OR i.external_qty<>i.quantity OR i.external_unit_cost<>i.unit_cost)))
UNION ALL
SELECT 'V17_freeze_points_to_active_task',COUNT(*) FROM product p JOIN stocktake_order s ON s.id=p.frozen_stocktake_id
WHERE s.status NOT IN ('COUNTING','PENDING','REJECTED') OR NOT EXISTS (SELECT 1 FROM stocktake_item i WHERE i.stocktake_id=s.id AND i.product_id=p.id)
UNION ALL
SELECT 'V18_active_task_has_freeze',COUNT(*) FROM stocktake_item i JOIN stocktake_order s ON s.id=i.stocktake_id JOIN product p ON p.id=i.product_id
WHERE s.status IN ('COUNTING','PENDING','REJECTED') AND (p.frozen_stocktake_id IS NULL OR p.frozen_stocktake_id<>s.id)
UNION ALL
SELECT 'V19_active_snapshot_unchanged',COUNT(*) FROM stocktake_item i JOIN stocktake_order s ON s.id=i.stocktake_id JOIN purchase_item b ON b.id=i.batch_item_id
WHERE s.status IN ('COUNTING','PENDING','REJECTED') AND i.book_qty<>b.stock_qty
UNION ALL
SELECT 'V20_submitted_actual_complete',COUNT(*) FROM stocktake_item i JOIN stocktake_order s ON s.id=i.stocktake_id
WHERE s.status IN ('PENDING','REJECTED','COMPLETED') AND i.actual_qty IS NULL
UNION ALL
SELECT 'V21_completed_stocktake_events',COUNT(*) FROM stocktake_item i JOIN stocktake_order s ON s.id=i.stocktake_id
LEFT JOIN stock_movement m ON m.stocktake_item_id=i.id
WHERE (s.status='COMPLETED' AND i.difference_qty<>0 AND (m.id IS NULL OR m.delta_qty<>i.difference_qty OR m.occurred_at<>s.completed_at))
 OR (s.status='COMPLETED' AND i.difference_qty=0 AND m.id IS NOT NULL)
 OR (s.status<>'COMPLETED' AND m.id IS NOT NULL)
UNION ALL
SELECT 'V22_active_scope_all_batches',COUNT(*) FROM purchase_item b JOIN purchase_order o ON o.id=b.purchase_id JOIN product p ON p.id=b.product_id
WHERE p.frozen_stocktake_id IS NOT NULL AND o.status='IN_STOCK' AND NOT EXISTS (
 SELECT 1 FROM stocktake_item i WHERE i.stocktake_id=p.frozen_stocktake_id AND i.batch_item_id=b.id)
UNION ALL
SELECT 'V23_saleable_not_negative',COUNT(*) FROM v_product_inventory WHERE saleable_qty<0 OR saleable_qty>book_qty OR expired_qty>book_qty
UNION ALL
SELECT 'V24_ai_operator_scope',COUNT(*) FROM ai_suggestion a JOIN sys_user u ON u.id=a.operator_id
WHERE u.role_code='CASHIER' OR (a.suggestion_type='SALES' AND u.role_code<>'ADMIN')
UNION ALL
SELECT 'V25_role_permission_subset',COUNT(*) FROM sys_role WHERE JSON_CONTAINS(ceiling_permissions,permissions)<>1 OR JSON_CONTAINS(ceiling_permissions,default_permissions)<>1
UNION ALL
SELECT 'V26_sale_events_equal_line_net',COUNT(*) FROM (
 SELECT (SELECT SUM(signed_qty) FROM v_sale_event) event_qty,
 (SELECT SUM(CASE WHEN o.status='COMPLETED' THEN i.quantity ELSE 0 END) FROM sale_item i JOIN sale_order o ON o.id=i.sale_id) current_net_qty,
 (SELECT SUM(signed_amount) FROM v_sale_event) event_amount,
 (SELECT SUM(CASE WHEN o.status='COMPLETED' THEN i.line_amount ELSE 0 END) FROM sale_item i JOIN sale_order o ON o.id=i.sale_id) current_net_amount
) x WHERE event_qty<>current_net_qty OR event_amount<>current_net_amount
UNION ALL
SELECT 'V27_placeholder_is_relative',COUNT(*) FROM (
 SELECT avatar_path path FROM sys_user UNION ALL SELECT image_path FROM product
) x WHERE path IS NOT NULL AND (path LIKE '/%' OR path LIKE '%:%' OR path LIKE '%..%')
UNION ALL
SELECT 'V28_role_catalog_membership',COUNT(*) FROM sys_role r JOIN JSON_TABLE(r.permissions,'$[*]' COLUMNS (code VARCHAR(100) PATH '$')) p
WHERE NOT EXISTS (
 SELECT 1 FROM sys_config c JOIN JSON_TABLE(c.config_value,'$[*]' COLUMNS (code VARCHAR(100) PATH '$.code',allowed JSON PATH '$.roles')) k
 WHERE c.config_key='security.permission_catalog' AND k.code=p.code AND JSON_CONTAINS(k.allowed,JSON_QUOTE(r.code))=1)
UNION ALL
SELECT 'V29_ai_usage_consistency',COUNT(*) FROM ai_suggestion WHERE NOT (
 (prompt_tokens IS NULL AND completion_tokens IS NULL AND total_tokens IS NULL AND cached_tokens IS NULL)
 OR (prompt_tokens IS NOT NULL AND completion_tokens IS NOT NULL AND total_tokens IS NOT NULL AND prompt_tokens>=0 AND completion_tokens>=0 AND total_tokens=prompt_tokens+completion_tokens AND (cached_tokens IS NULL OR (cached_tokens>=0 AND cached_tokens<=prompt_tokens))))
)
SELECT check_name,violations,IF(violations=0,'PASS','FAIL') result FROM checks ORDER BY check_name;

-- 场景清单：展示全部状态与测试夹具，而非伪造测试执行结果。
SELECT 'purchase' object_type,status,COUNT(*) row_count FROM purchase_order GROUP BY status
UNION ALL SELECT 'reconciliation',reconcile_status,COUNT(*) FROM purchase_order GROUP BY reconcile_status
UNION ALL SELECT 'stocktake',status,COUNT(*) FROM stocktake_order GROUP BY status
UNION ALL SELECT 'sale',status,COUNT(*) FROM sale_order GROUP BY status
UNION ALL SELECT 'ai_fixture',CONCAT(suggestion_type,':',status),COUNT(*) FROM ai_suggestion GROUP BY suggestion_type,status;

SELECT id,code,name,book_qty,batch_qty,saleable_qty,expired_qty,is_frozen,stock_lower_limit FROM v_product_inventory ORDER BY id;
SELECT YEAR(event_at) year_no,MONTH(event_at) month_no,SUM(signed_qty) net_qty,SUM(signed_amount) net_amount
FROM v_sale_event GROUP BY YEAR(event_at),MONTH(event_at) ORDER BY year_no,month_no;
