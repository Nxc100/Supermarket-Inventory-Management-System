/*
 Navicat Premium Data Transfer

 Source Server         : root
 Source Server Type    : MySQL
 Source Server Version : 80034
 Source Host           : localhost:3306
 Source Schema         : supermarket_inventory

 Target Server Type    : MySQL
 Target Server Version : 80034
 File Encoding         : 65001

 Date: 10/11/2026 16:30:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_suggestion
-- ----------------------------
DROP TABLE IF EXISTS `ai_suggestion`;
CREATE TABLE `ai_suggestion`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_key` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `suggestion_type` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'INVENTORY/SALES',
  `operator_id` bigint NOT NULL,
  `operator_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `filter_data` json NOT NULL COMMENT '分类/商品ID筛选，不含联系人、账号密码等',
  `input_summary` json NOT NULL COMMENT '实际发送的最小聚合业务快照',
  `snapshot_at` datetime(6) NOT NULL,
  `provider` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `is_fixture` tinyint NOT NULL DEFAULT 0 COMMENT '1仅初始测试夹具，界面必须标注，不能当真实API成功证据',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'PENDING/SUCCESS/FAILURE',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `error_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `error_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `duration_ms` int NULL DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `finished_at` datetime(6) NULL DEFAULT NULL,
  `prompt_tokens` bigint NULL DEFAULT NULL COMMENT '服务商输入Token，NULL为未报告',
  `completion_tokens` bigint NULL DEFAULT NULL COMMENT '服务商输出Token，NULL为未报告',
  `total_tokens` bigint NULL DEFAULT NULL,
  `cached_tokens` bigint NULL DEFAULT NULL,
  `provider_request_id` varchar(128) NULL DEFAULT NULL,
  INDEX `ix_ai_usage` (`is_fixture`,`requested_at`,`model_name`),
  CONSTRAINT `ck_ai_usage` CHECK ((prompt_tokens IS NULL AND completion_tokens IS NULL AND total_tokens IS NULL AND cached_tokens IS NULL) OR (prompt_tokens IS NOT NULL AND completion_tokens IS NOT NULL AND total_tokens IS NOT NULL AND prompt_tokens>=0 AND completion_tokens>=0 AND total_tokens=prompt_tokens+completion_tokens AND (cached_tokens IS NULL OR (cached_tokens>=0 AND cached_tokens<=prompt_tokens)))),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_request`(`request_key` ASC) USING BTREE,
  INDEX `ix_ai_type_status`(`suggestion_type` ASC, `status` ASC, `requested_at` ASC) USING BTREE,
  INDEX `ix_ai_owner`(`operator_id` ASC, `requested_at` ASC) USING BTREE,
  CONSTRAINT `fk_ai_user` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_ai_fixture` CHECK ((`is_fixture` in (0,1)) and ((`is_fixture` = 0) or (`provider` = _utf8mb4'FIXTURE'))),
  CONSTRAINT `ck_ai_result` CHECK (((`status` = _utf8mb4'PENDING') and (`content` is null) and (`finished_at` is null)) or ((`status` = _utf8mb4'SUCCESS') and (`content` is not null) and (char_length(trim(`content`)) > 0) and (`finished_at` is not null) and (`error_code` is null)) or ((`status` = _utf8mb4'FAILURE') and (`content` is null) and (`error_code` is not null) and (`error_message` is not null) and (`finished_at` is not null))),
  CONSTRAINT `ck_ai_status` CHECK (`status` in (_utf8mb4'PENDING',_utf8mb4'SUCCESS',_utf8mb4'FAILURE')),
  CONSTRAINT `ck_ai_time` CHECK ((`period_end` >= `period_start`) and ((`duration_ms` is null) or (`duration_ms` >= 0))),
  CONSTRAINT `ck_ai_type` CHECK (`suggestion_type` in (_utf8mb4'INVENTORY',_utf8mb4'SALES'))
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模型输出与失败记录；不含密钥，测试数据不伪装真实模型调用' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_suggestion
-- ----------------------------
INSERT INTO `ai_suggestion` (id,request_key,suggestion_type,operator_id,operator_name,period_start,period_end,filter_data,input_summary,snapshot_at,provider,model_name,is_fixture,status,content,error_code,error_message,duration_ms,requested_at,finished_at) VALUES (1, 'seed-ai-1', 'INVENTORY', 3, '采购张三', '2026-10-12', '2026-11-10', '{\"productIds\": [1]}', '{\"bookQty\": 5, \"fixture\": true, \"productId\": 1, \"capturedAt\": \"2026-11-10 16:13:25.806904\", \"lowerLimit\": 5, \"saleableQty\": 5, \"periodNetQty\": 14, \"periodNetAmount\": 70.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'SUCCESS', '【初始测试数据，非真实模型调用】库存5瓶、下限5瓶，近期净销量14瓶；请人工核实补货数量。', NULL, NULL, 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `ai_suggestion` (id,request_key,suggestion_type,operator_id,operator_name,period_start,period_end,filter_data,input_summary,snapshot_at,provider,model_name,is_fixture,status,content,error_code,error_message,duration_ms,requested_at,finished_at) VALUES (2, 'seed-ai-2', 'SALES', 1, '系统管理员', '2026-10-12', '2026-11-10', '{}', '{\"netQty\": 18, \"fixture\": true, \"netAmount\": 97.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'SUCCESS', '【初始测试数据，非真实模型调用】请结合输入摘要查看销售与商品排行，并人工核查滞销原因。', NULL, NULL, 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `ai_suggestion` (id,request_key,suggestion_type,operator_id,operator_name,period_start,period_end,filter_data,input_summary,snapshot_at,provider,model_name,is_fixture,status,content,error_code,error_message,duration_ms,requested_at,finished_at) VALUES (3, 'seed-ai-3', 'INVENTORY', 3, '采购张三', '2026-10-12', '2026-11-10', '{\"productIds\": [1]}', '{\"bookQty\": 5, \"fixture\": true, \"productId\": 1, \"capturedAt\": \"2026-11-10 16:13:25.806904\", \"lowerLimit\": 5, \"saleableQty\": 5, \"periodNetQty\": 14, \"periodNetAmount\": 70.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'FAILURE', NULL, 'TIMEOUT', '测试降级记录：TIMEOUT', 15000, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `ai_suggestion` (id,request_key,suggestion_type,operator_id,operator_name,period_start,period_end,filter_data,input_summary,snapshot_at,provider,model_name,is_fixture,status,content,error_code,error_message,duration_ms,requested_at,finished_at) VALUES (4, 'seed-ai-4', 'SALES', 1, '系统管理员', '2026-10-12', '2026-11-10', '{}', '{\"netQty\": 18, \"fixture\": true, \"netAmount\": 97.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'FAILURE', NULL, 'NOT_CONFIGURED', '测试降级记录：NOT_CONFIGURED', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `ai_suggestion` (id,request_key,suggestion_type,operator_id,operator_name,period_start,period_end,filter_data,input_summary,snapshot_at,provider,model_name,is_fixture,status,content,error_code,error_message,duration_ms,requested_at,finished_at) VALUES (5, 'seed-ai-5', 'INVENTORY', 4, '采购李四', '2026-10-12', '2026-11-10', '{\"productIds\": [1]}', '{\"bookQty\": 5, \"fixture\": true, \"productId\": 1, \"capturedAt\": \"2026-11-10 16:13:25.806904\", \"lowerLimit\": 5, \"saleableQty\": 5, \"periodNetQty\": 14, \"periodNetAmount\": 70.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'FAILURE', NULL, 'RATE_LIMITED', '测试降级记录：RATE_LIMITED', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `ai_suggestion` (id,request_key,suggestion_type,operator_id,operator_name,period_start,period_end,filter_data,input_summary,snapshot_at,provider,model_name,is_fixture,status,content,error_code,error_message,duration_ms,requested_at,finished_at) VALUES (6, 'seed-ai-6', 'INVENTORY', 3, '采购张三', '2026-10-12', '2026-11-10', '{\"productIds\": [1]}', '{\"bookQty\": 5, \"fixture\": true, \"productId\": 1, \"capturedAt\": \"2026-11-10 16:13:25.806904\", \"lowerLimit\": 5, \"saleableQty\": 5, \"periodNetQty\": 14, \"periodNetAmount\": 70.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'FAILURE', NULL, 'EMPTY_RESULT', '测试降级记录：EMPTY_RESULT', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(20) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_category_code`(`code` ASC) USING BTREE,
  UNIQUE INDEX `uk_category_name`(`name` ASC) USING BTREE,
  CONSTRAINT `ck_category_values` CHECK ((`sort_order` >= 0) and (`enabled` in (0,1)) and (char_length(`name`) between 2 and 30))
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '一级商品分类，引用后限制删除' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of category
-- ----------------------------
INSERT INTO `category` VALUES (1, 'C001', '饮料乳品', 1, 1, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `category` VALUES (2, 'C002', '粮油食品', 2, 1, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `category` VALUES (3, 'C003', '日用清洁', 3, 1, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `category` VALUES (4, 'C004', '文具用品', 4, 1, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `category` VALUES (5, 'C005', '糕点零食', 5, 1, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `category` VALUES (6, 'C006', '未用分类', 6, 1, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NULL DEFAULT NULL,
  `actor_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作者姓名快照',
  `action` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `target_type` varchar(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'USER/ROLE/PRODUCT/SUPPLIER/PURCHASE/SALE/STOCKTAKE/AI/CONFIG',
  `target_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '对象ID或角色/参数键；多态历史不作为业务主数据',
  `target_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单号或编码快照',
  `result` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUCCESS',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `before_data` json NULL COMMENT '对账撤回时必须保存逐行原始核对信息',
  `after_data` json NULL,
  `request_id` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL,
  `occurred_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `ix_log_object`(`target_type` ASC, `target_id` ASC, `occurred_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_log_actor`(`actor_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `ix_log_action_time`(`action` ASC, `occurred_at` ASC) USING BTREE,
  CONSTRAINT `fk_log_user` FOREIGN KEY (`actor_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `ck_log_result` CHECK (`result` in (_utf8mb4'SUCCESS',_utf8mb4'FAILURE'))
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '只追加操作历史；JSON 保存不参与库存计算的审计快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of operation_log
-- ----------------------------
INSERT INTO `operation_log` VALUES (1, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '1', 'CG-DEMO-0001', 'SUCCESS', NULL, NULL, '{\"status\": \"IN_STOCK\", \"batchIds\": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18]}', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `operation_log` VALUES (2, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '2', 'CG-DEMO-0002', 'SUCCESS', NULL, NULL, '{\"status\": \"IN_STOCK\", \"batchIds\": [19, 20]}', NULL, '2026-10-26 16:13:25.806904');
INSERT INTO `operation_log` VALUES (3, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '3', 'CG-DEMO-0003', 'SUCCESS', NULL, NULL, '{\"status\": \"IN_STOCK\", \"batchIds\": [21]}', NULL, '2026-11-09 16:13:25.806904');
INSERT INTO `operation_log` VALUES (4, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '4', 'CG-DEMO-0004', 'SUCCESS', NULL, NULL, '{\"status\": \"IN_STOCK\", \"batchIds\": [22]}', NULL, '2026-11-07 16:13:25.806904');
INSERT INTO `operation_log` VALUES (5, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '5', 'CG-DEMO-0005', 'SUCCESS', NULL, NULL, '{\"status\": \"IN_STOCK\", \"batchIds\": [23]}', NULL, '2026-11-08 16:13:25.806904');
INSERT INTO `operation_log` VALUES (6, 1, '系统管理员', 'PURCHASE_REJECT', 'PURCHASE', '7', 'CG-DEMO-0007', 'SUCCESS', '缺少实物生产日期和到期日期，请补充', NULL, '{\"status\": \"REJECTED\"}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (7, 3, '采购张三', 'PURCHASE_WITHDRAW', 'PURCHASE', '10', 'CG-DEMO-0010', 'SUCCESS', NULL, '{\"status\": \"PENDING\"}', '{\"status\": \"DRAFT\"}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (8, 1, '系统管理员', 'PURCHASE_REVERSE', 'PURCHASE', '4', 'CG-DEMO-0004', 'SUCCESS', '录入重复采购，尚未使用', '{\"status\": \"IN_STOCK\"}', '{\"status\": \"REVERSED\"}', NULL, '2026-11-08 16:13:25.806904');
INSERT INTO `operation_log` VALUES (9, 1, '系统管理员', 'SALE_CORRECT', 'SALE', '6', 'XS-DEMO-0006', 'SUCCESS', '当天整单误录，货品完整在店', '{\"status\": \"COMPLETED\"}', '{\"status\": \"CORRECTED\"}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (10, 1, '系统管理员', 'STOCKTAKE_APPROVE', 'STOCKTAKE', '1', 'PD-DEMO-0001', 'SUCCESS', NULL, NULL, '{\"status\": \"COMPLETED\", \"productIds\": [1, 5, 7, 20]}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (11, 3, '采购张三', 'STOCKTAKE_COUNTING', 'STOCKTAKE', '2', 'PD-DEMO-0002', 'SUCCESS', NULL, NULL, '{\"status\": \"COUNTING\", \"productIds\": [10]}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (12, 4, '采购李四', 'STOCKTAKE_PENDING', 'STOCKTAKE', '3', 'PD-DEMO-0003', 'SUCCESS', NULL, NULL, '{\"status\": \"PENDING\", \"productIds\": [11]}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (13, 1, '系统管理员', 'STOCKTAKE_REJECTED', 'STOCKTAKE', '4', 'PD-DEMO-0004', 'SUCCESS', NULL, NULL, '{\"status\": \"REJECTED\", \"productIds\": [12]}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (14, 1, '系统管理员', 'STOCKTAKE_CLOSED', 'STOCKTAKE', '5', 'PD-DEMO-0005', 'SUCCESS', NULL, NULL, '{\"status\": \"CLOSED\", \"productIds\": [8]}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (15, 3, '采购张三', 'RECONCILE_MATCH', 'PURCHASE', '1', 'CG-DEMO-0001', 'SUCCESS', NULL, '{\"status\": \"UNRECONCILED\"}', '{\"lines\": [{\"itemId\": 1, \"externalQty\": 10, \"externalUnitCost\": \"3\"}, {\"itemId\": 2, \"externalQty\": 10, \"externalUnitCost\": \"3\"}, {\"itemId\": 3, \"externalQty\": 40, \"externalUnitCost\": \"4\"}, {\"itemId\": 4, \"externalQty\": 12, \"externalUnitCost\": \"5\"}, {\"itemId\": 5, \"externalQty\": 50, \"externalUnitCost\": \"48\"}, {\"itemId\": 6, \"externalQty\": 30, \"externalUnitCost\": \"40\"}, {\"itemId\": 7, \"externalQty\": 40, \"externalUnitCost\": \"8\"}, {\"itemId\": 8, \"externalQty\": 30, \"externalUnitCost\": \"3.5\"}, {\"itemId\": 9, \"externalQty\": 5, \"externalUnitCost\": \"1\"}, {\"itemId\": 10, \"externalQty\": 20, \"externalUnitCost\": \"1\"}, {\"itemId\": 11, \"externalQty\": 10, \"externalUnitCost\": \"2\"}, {\"itemId\": 12, \"externalQty\": 12, \"externalUnitCost\": \"5\"}, {\"itemId\": 13, \"externalQty\": 8, \"externalUnitCost\": \"6\"}, {\"itemId\": 14, \"externalQty\": 7, \"externalUnitCost\": \"6\"}, {\"itemId\": 15, \"externalQty\": 8, \"externalUnitCost\": \"6\"}, {\"itemId\": 16, \"externalQty\": 4, \"externalUnitCost\": \"5\"}, {\"itemId\": 17, \"externalQty\": 6, \"externalUnitCost\": \"5\"}, {\"itemId\": 18, \"externalQty\": 30, \"externalUnitCost\": \"4\"}], \"status\": \"MATCHED\", \"externalTotal\": \"4718.0\"}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (16, 4, '采购李四', 'RECONCILE_DIFFERENCE', 'PURCHASE', '3', 'CG-DEMO-0003', 'SUCCESS', NULL, '{\"status\": \"UNRECONCILED\"}', '{\"lines\": [{\"itemId\": 21, \"externalQty\": 21, \"externalUnitCost\": \"4\"}], \"status\": \"DIFFERENCE\", \"externalTotal\": \"84\"}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (17, 3, '采购张三', 'RECONCILE_MATCH', 'PURCHASE', '2', 'CG-DEMO-0002', 'SUCCESS', NULL, '{\"status\": \"UNRECONCILED\"}', '{\"lines\": [{\"itemId\": 19, \"externalQty\": 6, \"externalUnitCost\": \"2\"}, {\"itemId\": 20, \"externalQty\": 5, \"externalUnitCost\": \"5\"}], \"status\": \"MATCHED\", \"externalTotal\": \"37\"}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (18, 3, '采购张三', 'RECONCILE_WITHDRAW', 'PURCHASE', '2', 'CG-DEMO-0002', 'SUCCESS', '重新检查供应商纸质凭据', '{\"lines\": [{\"itemId\": 19, \"externalQty\": 6, \"externalUnitCost\": \"2\"}, {\"itemId\": 20, \"externalQty\": 5, \"externalUnitCost\": \"5\"}], \"status\": \"MATCHED\", \"externalTotal\": \"37\"}', '{\"status\": \"UNRECONCILED\"}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (19, 1, '系统管理员', 'PRODUCT_PRICE_CHANGE', 'PRODUCT', '23', 'P0023', 'SUCCESS', '验证历史售价不随新售价变化', '{\"salePrice\": \"7.50\"}', '{\"salePrice\": \"9.00\"}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (20, 1, '系统管理员', 'PRODUCT_STATUS_CHANGE', 'PRODUCT', '13', 'P0013', 'SUCCESS', NULL, '{\"canSell\": 1, \"enabled\": 1, \"canPurchase\": 1}', '{\"canSell\": 1, \"enabled\": 0, \"canPurchase\": 1}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (21, 1, '系统管理员', 'PRODUCT_STATUS_CHANGE', 'PRODUCT', '14', 'P0014', 'SUCCESS', NULL, '{\"canSell\": 1, \"enabled\": 1, \"canPurchase\": 1}', '{\"canSell\": 1, \"enabled\": 1, \"canPurchase\": 0}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (22, 1, '系统管理员', 'PRODUCT_STATUS_CHANGE', 'PRODUCT', '15', 'P0015', 'SUCCESS', NULL, '{\"canSell\": 1, \"enabled\": 1, \"canPurchase\": 1}', '{\"canSell\": 0, \"enabled\": 1, \"canPurchase\": 1}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (23, 3, '采购张三', 'AI_SUCCESS', 'AI', '1', 'seed-ai-1', 'SUCCESS', NULL, NULL, '{\"type\": \"INVENTORY\", \"status\": \"SUCCESS\", \"isFixture\": true}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (24, 1, '系统管理员', 'AI_SUCCESS', 'AI', '2', 'seed-ai-2', 'SUCCESS', NULL, NULL, '{\"type\": \"SALES\", \"status\": \"SUCCESS\", \"isFixture\": true}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (25, 3, '采购张三', 'AI_FAILURE', 'AI', '3', 'seed-ai-3', 'SUCCESS', NULL, NULL, '{\"type\": \"INVENTORY\", \"status\": \"FAILURE\", \"isFixture\": true}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (26, 1, '系统管理员', 'AI_FAILURE', 'AI', '4', 'seed-ai-4', 'SUCCESS', NULL, NULL, '{\"type\": \"SALES\", \"status\": \"FAILURE\", \"isFixture\": true}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (27, 4, '采购李四', 'AI_FAILURE', 'AI', '5', 'seed-ai-5', 'SUCCESS', NULL, NULL, '{\"type\": \"INVENTORY\", \"status\": \"FAILURE\", \"isFixture\": true}', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `operation_log` VALUES (28, 3, '采购张三', 'AI_FAILURE', 'AI', '6', 'seed-ai-6', 'SUCCESS', NULL, NULL, '{\"type\": \"INVENTORY\", \"status\": \"FAILURE\", \"isFixture\": true}', NULL, '2026-11-10 16:13:25.806904');

-- ----------------------------
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `barcode` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL COMMENT '无条码用NULL，不用空串',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category_id` bigint NOT NULL,
  `specification` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `unit` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `reference_cost` decimal(16, 2) NULL DEFAULT NULL,
  `sale_price` decimal(16, 2) NOT NULL,
  `image_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '测试图片.jpg',
  `image_meta` json NULL,
  `manages_expiry` tinyint NOT NULL DEFAULT 0,
  `shelf_life_days` int NULL DEFAULT NULL,
  `near_expiry_days` int NOT NULL DEFAULT 7,
  `stock_lower_limit` bigint NOT NULL DEFAULT 0,
  `can_purchase` tinyint NOT NULL DEFAULT 1,
  `can_sell` tinyint NOT NULL DEFAULT 1,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `stock_qty` bigint NOT NULL DEFAULT 0 COMMENT '冗余账面余额，必须等于有效批次余额之和，业务表单禁止直接修改',
  `frozen_stocktake_id` bigint NULL DEFAULT NULL COMMENT '活动盘点指针，一个商品最多被一个盘点冻结；后置FK',
  `stock_version` bigint NOT NULL DEFAULT 0 COMMENT '每次库存变化或冻结变化递增',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_product_code`(`code` ASC) USING BTREE,
  UNIQUE INDEX `uk_product_barcode`(`barcode` ASC) USING BTREE,
  INDEX `ix_product_category`(`category_id` ASC, `enabled` ASC, `id` ASC) USING BTREE,
  INDEX `ix_product_status`(`enabled` ASC, `can_sell` ASC, `can_purchase` ASC, `id` ASC) USING BTREE,
  INDEX `ix_product_freeze`(`frozen_stocktake_id` ASC) USING BTREE,
  INDEX `fk_product_user`(`created_by` ASC) USING BTREE,
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_product_freeze` FOREIGN KEY (`frozen_stocktake_id`) REFERENCES `stocktake_order` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_product_user` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_product_barcode` CHECK ((`barcode` is null) or (char_length(`barcode`) between 4 and 32)),
  CONSTRAINT `ck_product_expiry` CHECK ((`near_expiry_days` >= 0) and (((`manages_expiry` = 1) and (`shelf_life_days` is not null) and (`shelf_life_days` > 0)) or ((`manages_expiry` = 0) and (`shelf_life_days` is null)))),
  CONSTRAINT `ck_product_flags` CHECK ((`manages_expiry` in (0,1)) and (`can_purchase` in (0,1)) and (`can_sell` in (0,1)) and (`enabled` in (0,1))),
  CONSTRAINT `ck_product_name` CHECK (char_length(`name`) between 2 and 100),
  CONSTRAINT `ck_product_price` CHECK ((`sale_price` > 0) and ((`reference_cost` is null) or (`reference_cost` >= 0))),
  CONSTRAINT `ck_product_qty` CHECK ((`stock_qty` between 0 and 999999999999) and (`stock_lower_limit` between 0 and 999999999999) and (`stock_version` >= 0))
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品档案、库存余额及唯一活动冻结；不单建库存余额和冻结表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (1, 'P0001', '6900000000001', '演示瓶装饮料', 1, '基本单位包装', '瓶', 3.00, 5.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 5, 1, 1, 1, 5, NULL, 6, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (2, 'P0002', '6900000000002', '纯牛奶', 1, '基本单位包装', '件', 4.00, 6.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 10, 1, 1, 1, 38, NULL, 2, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (3, 'P0003', '6900000000003', '临期酸奶', 1, '基本单位包装', '件', 5.00, 8.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 2, 1, 1, 1, 12, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (4, 'P0004', '6900000000004', '过期面包', 5, '基本单位包装', '件', 2.00, 5.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 5, 1, 1, 1, 6, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (5, 'P0005', '6900000000005', '大米五公斤', 2, '基本单位包装', '件', 48.00, 68.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 5, 1, 1, 1, 45, NULL, 3, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (6, 'P0006', '6900000000006', '滞销食用油', 2, '基本单位包装', '件', 40.00, 58.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 3, 1, 1, 1, 30, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (7, 'P0007', '6900000000007', '抽纸三包装', 3, '基本单位包装', '件', 8.00, 12.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 5, 1, 1, 1, 40, NULL, 7, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (8, 'P0008', '6900000000008', '香皂', 3, '基本单位包装', '件', 3.50, 6.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 5, 1, 1, 1, 30, NULL, 3, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (9, 'P0009', '6900000000009', '并发测试矿泉水', 1, '基本单位包装', '瓶', 1.00, 2.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 5, 1, 1, 1, 5, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (10, 'P0010', '6900000000010', '盘点中文具笔', 4, '基本单位包装', '件', 1.00, 3.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 3, 1, 1, 1, 20, 2, 2, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:26.002946');
INSERT INTO `product` VALUES (11, 'P0011', '6900000000011', '待审核盘点方便面', 2, '基本单位包装', '件', 2.00, 4.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 3, 1, 1, 1, 10, 3, 2, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:26.003347');
INSERT INTO `product` VALUES (12, 'P0012', '6900000000012', '驳回盘点饼干', 5, '基本单位包装', '件', 5.00, 9.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 3, 1, 1, 1, 12, 4, 2, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:26.003570');
INSERT INTO `product` VALUES (13, 'P0013', '6900000000013', '已停用历史商品', 3, '基本单位包装', '件', 6.00, 10.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 2, 1, 1, 0, 8, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (14, 'P0014', '6900000000014', '禁止采购商品', 3, '基本单位包装', '件', 6.00, 10.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 2, 0, 1, 1, 7, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (15, 'P0015', '6900000000015', '禁止销售商品', 3, '基本单位包装', '件', 6.00, 10.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 2, 1, 0, 1, 8, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (16, 'P0016', '6900000000016', '零库存商品', 4, '基本单位包装', '件', 2.00, 5.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 0, 1, 1, 1, 0, NULL, 0, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (17, 'P0017', '6900000000017', '新近入库商品', 4, '基本单位包装', '件', 4.00, 7.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 3, 1, 1, 1, 20, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (18, 'P0018', '6900000000018', '到期当天食品', 5, '基本单位包装', '件', 5.00, 8.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 0, 2, 1, 1, 1, 4, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (19, 'P0019', '6900000000019', '明天到期食品', 5, '基本单位包装', '件', 5.00, 8.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 2, 1, 1, 1, 6, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (20, 'P0020', '6900000000020', '过期已清零食品', 5, '基本单位包装', '件', 5.00, 8.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 2, 1, 1, 1, 0, NULL, 2, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (21, 'P0021', '6900000000021', '待审核采购商品', 4, '基本单位包装', '件', 2.00, 5.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 2, 1, 1, 1, 0, NULL, 0, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (22, 'P0022', NULL, '可删除未使用商品', 4, '基本单位包装', '件', 3.00, 6.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 0, 1, 1, 1, 0, NULL, 0, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (23, 'P0023', '6900000000023', '调价快照测试商品', 3, '基本单位包装', '件', 4.00, 9.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 2, 1, 1, 1, 28, NULL, 2, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (24, 'P0024', '6900000000024', '缺日期驳回食品', 5, '基本单位包装', '件', 5.00, 9.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 2, 1, 1, 1, 0, NULL, 0, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');

-- ----------------------------
-- Table structure for purchase_item
-- ----------------------------
DROP TABLE IF EXISTS `purchase_item`;
CREATE TABLE `purchase_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `purchase_id` bigint NOT NULL,
  `line_no` int NOT NULL,
  `product_id` bigint NOT NULL,
  `product_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `product_barcode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category_id` bigint NOT NULL,
  `category_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `specification` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `unit` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `manages_expiry` tinyint NOT NULL,
  `quantity` int NOT NULL,
  `received_qty` int NULL DEFAULT NULL COMMENT '未登记到货为NULL，入库前必须等于quantity',
  `unit_cost` decimal(16, 2) NOT NULL,
  `line_amount` decimal(16, 2) GENERATED ALWAYS AS ((`quantity` * `unit_cost`)) STORED NULL,
  `supplier_batch_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `production_date` date NULL DEFAULT NULL,
  `expiry_date` date NULL DEFAULT NULL,
  `batch_no` varchar(48) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL COMMENT '系统审核入库时生成，唯一',
  `stock_qty` bigint NULL DEFAULT NULL COMMENT '批次当前在库量，未入库NULL，已入库及撤销批次为非负数',
  `external_qty` int NULL DEFAULT NULL COMMENT '最近一次外部核对数量，历史保存在对账日志',
  `external_unit_cost` decimal(16, 2) NULL DEFAULT NULL,
  `external_line_amount` decimal(16, 2) GENERATED ALWAYS AS ((`external_qty` * `external_unit_cost`)) STORED NULL,
  `reconcile_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_purchase_line`(`purchase_id` ASC, `line_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_batch_product`(`id` ASC, `product_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_batch_no`(`batch_no` ASC) USING BTREE,
  INDEX `ix_batch_fefo`(`product_id` ASC, `expiry_date` ASC, `id` ASC) USING BTREE,
  INDEX `ix_batch_expiry`(`expiry_date` ASC, `product_id` ASC) USING BTREE,
  INDEX `fk_pitem_category`(`category_id` ASC) USING BTREE,
  CONSTRAINT `fk_pitem_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_pitem_order` FOREIGN KEY (`purchase_id`) REFERENCES `purchase_order` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_pitem_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_pitem_batch` CHECK (((`batch_no` is null) and (`stock_qty` is null)) or ((`batch_no` is not null) and (`stock_qty` is not null) and (`stock_qty` between 0 and 999999999999) and (`received_qty` is not null) and (`received_qty` = `quantity`))),
  CONSTRAINT `ck_pitem_expiry` CHECK ((`manages_expiry` in (0,1)) and (((`manages_expiry` = 0) and (`production_date` is null) and (`expiry_date` is null)) or ((`manages_expiry` = 1) and ((`production_date` is null) or (`expiry_date` is null) or (`expiry_date` >= `production_date`))))),
  CONSTRAINT `ck_pitem_external` CHECK (((`external_qty` is null) and (`external_unit_cost` is null)) or ((`external_qty` is not null) and (`external_unit_cost` is not null) and (`external_qty` between 0 and 999999) and (`external_unit_cost` >= 0))),
  CONSTRAINT `ck_pitem_price` CHECK ((`unit_cost` > 0) and (`line_no` > 0)),
  CONSTRAINT `ck_pitem_qty` CHECK ((`quantity` between 1 and 999999) and ((`received_qty` is null) or (`received_qty` between 0 and 999999))),
  CONSTRAINT `ck_pitem_stock_dates` CHECK ((`batch_no` is null) or (`manages_expiry` = 0) or ((`production_date` is not null) and (`expiry_date` is not null)))
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购明细与其唯一库存批次合并，业务追溯仍使用真实外键' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of purchase_item
-- ----------------------------
INSERT INTO `purchase_item` VALUES (1, 1, 1, 1, 'P0001', '6900000000001', '演示瓶装饮料', 1, '饮料乳品', '基本单位包装', '瓶', 1, 10, 10, 3.00, DEFAULT, 'SUPLOT-0001-1', '2026-08-22', '2026-11-13', 'BATCH-00001', 0, 10, 3.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (2, 1, 2, 1, 'P0001', '6900000000001', '演示瓶装饮料', 1, '饮料乳品', '基本单位包装', '瓶', 1, 10, 10, 3.00, DEFAULT, 'SUPLOT-0001-2', '2026-08-22', '2027-02-08', 'BATCH-00002', 5, 10, 3.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (3, 1, 3, 2, 'P0002', '6900000000002', '纯牛奶', 1, '饮料乳品', '基本单位包装', '件', 1, 40, 40, 4.00, DEFAULT, 'SUPLOT-0001-3', '2026-08-22', '2027-01-09', 'BATCH-00003', 38, 40, 4.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (4, 1, 4, 3, 'P0003', '6900000000003', '临期酸奶', 1, '饮料乳品', '基本单位包装', '件', 1, 12, 12, 5.00, DEFAULT, 'SUPLOT-0001-4', '2026-08-22', '2026-11-13', 'BATCH-00004', 12, 12, 5.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (5, 1, 5, 5, 'P0005', '6900000000005', '大米五公斤', 2, '粮油食品', '基本单位包装', '件', 0, 50, 50, 48.00, DEFAULT, 'SUPLOT-0001-5', NULL, NULL, 'BATCH-00005', 45, 50, 48.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (6, 1, 6, 6, 'P0006', '6900000000006', '滞销食用油', 2, '粮油食品', '基本单位包装', '件', 0, 30, 30, 40.00, DEFAULT, 'SUPLOT-0001-6', NULL, NULL, 'BATCH-00006', 30, 30, 40.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (7, 1, 7, 7, 'P0007', '6900000000007', '抽纸三包装', 3, '日用清洁', '基本单位包装', '件', 0, 40, 40, 8.00, DEFAULT, 'SUPLOT-0001-7', NULL, NULL, 'BATCH-00007', 40, 40, 8.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (8, 1, 8, 8, 'P0008', '6900000000008', '香皂', 3, '日用清洁', '基本单位包装', '件', 0, 30, 30, 3.50, DEFAULT, 'SUPLOT-0001-8', NULL, NULL, 'BATCH-00008', 27, 30, 3.50, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (9, 1, 9, 9, 'P0009', '6900000000009', '并发测试矿泉水', 1, '饮料乳品', '基本单位包装', '瓶', 0, 5, 5, 1.00, DEFAULT, 'SUPLOT-0001-9', NULL, NULL, 'BATCH-00009', 5, 5, 1.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (10, 1, 10, 10, 'P0010', '6900000000010', '盘点中文具笔', 4, '文具用品', '基本单位包装', '件', 0, 20, 20, 1.00, DEFAULT, 'SUPLOT-0001-10', NULL, NULL, 'BATCH-00010', 20, 20, 1.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (11, 1, 11, 11, 'P0011', '6900000000011', '待审核盘点方便面', 2, '粮油食品', '基本单位包装', '件', 0, 10, 10, 2.00, DEFAULT, 'SUPLOT-0001-11', NULL, NULL, 'BATCH-00011', 10, 10, 2.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (12, 1, 12, 12, 'P0012', '6900000000012', '驳回盘点饼干', 5, '糕点零食', '基本单位包装', '件', 1, 12, 12, 5.00, DEFAULT, 'SUPLOT-0001-12', '2026-08-22', '2026-12-30', 'BATCH-00012', 12, 12, 5.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (13, 1, 13, 13, 'P0013', '6900000000013', '已停用历史商品', 3, '日用清洁', '基本单位包装', '件', 0, 8, 8, 6.00, DEFAULT, 'SUPLOT-0001-13', NULL, NULL, 'BATCH-00013', 8, 8, 6.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (14, 1, 14, 14, 'P0014', '6900000000014', '禁止采购商品', 3, '日用清洁', '基本单位包装', '件', 0, 7, 7, 6.00, DEFAULT, 'SUPLOT-0001-14', NULL, NULL, 'BATCH-00014', 7, 7, 6.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (15, 1, 15, 15, 'P0015', '6900000000015', '禁止销售商品', 3, '日用清洁', '基本单位包装', '件', 0, 8, 8, 6.00, DEFAULT, 'SUPLOT-0001-15', NULL, NULL, 'BATCH-00015', 8, 8, 6.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (16, 1, 16, 18, 'P0018', '6900000000018', '到期当天食品', 5, '糕点零食', '基本单位包装', '件', 1, 4, 4, 5.00, DEFAULT, 'SUPLOT-0001-16', '2026-08-22', '2026-11-10', 'BATCH-00016', 4, 4, 5.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (17, 1, 17, 19, 'P0019', '6900000000019', '明天到期食品', 5, '糕点零食', '基本单位包装', '件', 1, 6, 6, 5.00, DEFAULT, 'SUPLOT-0001-17', '2026-08-22', '2026-11-11', 'BATCH-00017', 6, 6, 5.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (18, 1, 18, 23, 'P0023', '6900000000023', '调价快照测试商品', 3, '日用清洁', '基本单位包装', '件', 0, 30, 30, 4.00, DEFAULT, 'SUPLOT-0001-18', NULL, NULL, 'BATCH-00018', 28, 30, 4.00, DEFAULT, '一致', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_item` VALUES (19, 2, 1, 4, 'P0004', '6900000000004', '过期面包', 5, '糕点零食', '基本单位包装', '件', 1, 6, 6, 2.00, DEFAULT, 'SUPLOT-0002-1', '2026-08-22', '2026-11-09', 'BATCH-00019', 6, NULL, NULL, DEFAULT, NULL, '2026-10-26 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (20, 2, 2, 20, 'P0020', '6900000000020', '过期已清零食品', 5, '糕点零食', '基本单位包装', '件', 1, 5, 5, 5.00, DEFAULT, 'SUPLOT-0002-2', '2026-08-22', '2026-11-08', 'BATCH-00020', 0, NULL, NULL, DEFAULT, NULL, '2026-10-26 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (21, 3, 1, 17, 'P0017', '6900000000017', '新近入库商品', 4, '文具用品', '基本单位包装', '件', 0, 20, 20, 4.00, DEFAULT, 'SUPLOT-0003-1', NULL, NULL, 'BATCH-00021', 20, 21, 4.00, DEFAULT, '凭据数量多一件，待核查', '2026-11-09 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (22, 4, 1, 7, 'P0007', '6900000000007', '抽纸三包装', 3, '日用清洁', '基本单位包装', '件', 0, 4, 4, 8.00, DEFAULT, 'SUPLOT-0004-1', NULL, NULL, 'BATCH-00022', 0, NULL, NULL, DEFAULT, NULL, '2026-11-07 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (23, 5, 1, 8, 'P0008', '6900000000008', '香皂', 3, '日用清洁', '基本单位包装', '件', 0, 3, 3, 3.50, DEFAULT, 'SUPLOT-0005-1', NULL, NULL, 'BATCH-00023', 3, NULL, NULL, DEFAULT, NULL, '2026-11-08 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (24, 6, 1, 21, 'P0021', '6900000000021', '待审核采购商品', 4, '文具用品', '基本单位包装', '件', 0, 10, 10, 2.00, DEFAULT, 'SUPLOT-0006-1', NULL, NULL, NULL, NULL, NULL, NULL, DEFAULT, NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (25, 7, 1, 24, 'P0024', '6900000000024', '缺日期驳回食品', 5, '糕点零食', '基本单位包装', '件', 1, 8, 8, 5.00, DEFAULT, 'SUPLOT-0007-1', NULL, NULL, NULL, NULL, NULL, NULL, DEFAULT, NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (26, 8, 1, 21, 'P0021', '6900000000021', '待审核采购商品', 4, '文具用品', '基本单位包装', '件', 0, 2, NULL, 2.00, DEFAULT, 'SUPLOT-0008-1', NULL, NULL, NULL, NULL, NULL, NULL, DEFAULT, NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (27, 9, 1, 21, 'P0021', '6900000000021', '待审核采购商品', 4, '文具用品', '基本单位包装', '件', 0, 3, 3, 2.00, DEFAULT, 'SUPLOT-0009-1', NULL, NULL, NULL, NULL, NULL, NULL, DEFAULT, NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (28, 10, 1, 5, 'P0005', '6900000000005', '大米五公斤', 2, '粮油食品', '基本单位包装', '件', 0, 6, NULL, 48.00, DEFAULT, 'SUPLOT-0010-1', NULL, NULL, NULL, NULL, NULL, NULL, DEFAULT, NULL, '2026-11-10 16:13:25.806904');

-- ----------------------------
-- Table structure for purchase_order
-- ----------------------------
DROP TABLE IF EXISTS `purchase_order`;
CREATE TABLE `purchase_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `supplier_id` bigint NOT NULL,
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `supplier_contact` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `supplier_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `supplier_address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operator_id` bigint NOT NULL,
  `operator_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/REJECTED/IN_STOCK/CLOSED/REVERSED',
  `total_qty` bigint NOT NULL DEFAULT 0,
  `total_received_qty` bigint NOT NULL DEFAULT 0,
  `total_amount` decimal(16, 2) NOT NULL DEFAULT 0.00,
  `expected_arrival_date` date NULL DEFAULT NULL,
  `arrived_at` datetime(6) NULL DEFAULT NULL,
  `first_submitted_at` datetime(6) NULL DEFAULT NULL COMMENT '一旦非空即禁止硬删，撤回不清空',
  `submitted_at` datetime(6) NULL DEFAULT NULL,
  `reviewed_by` bigint NULL DEFAULT NULL,
  `reviewer_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reviewed_at` datetime(6) NULL DEFAULT NULL,
  `rejection_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `stocked_at` datetime(6) NULL DEFAULT NULL,
  `closed_by` bigint NULL DEFAULT NULL,
  `closed_at` datetime(6) NULL DEFAULT NULL,
  `close_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reversed_by` bigint NULL DEFAULT NULL,
  `reversed_at` datetime(6) NULL DEFAULT NULL,
  `reverse_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reconcile_status` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'UNRECONCILED' COMMENT 'UNRECONCILED/DIFFERENCE/MATCHED/NA',
  `external_total_amount` decimal(16, 2) NULL DEFAULT NULL,
  `reconcile_reference` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reconcile_difference` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reconcile_resolution` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reconciled_by` bigint NULL DEFAULT NULL,
  `reconciled_at` datetime(6) NULL DEFAULT NULL,
  `reconcile_version` int NOT NULL DEFAULT 0 COMMENT '每次核对/撤回递增，历史落operation_log完整JSON',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `row_version` bigint NOT NULL DEFAULT 0,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_purchase_no`(`order_no` ASC) USING BTREE,
  INDEX `ix_purchase_owner`(`operator_id` ASC, `status` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_purchase_status`(`status` ASC, `submitted_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_purchase_supplier`(`supplier_id` ASC, `stocked_at` ASC) USING BTREE,
  INDEX `ix_purchase_reconcile`(`reconcile_status` ASC, `stocked_at` ASC) USING BTREE,
  INDEX `fk_purchase_reviewer`(`reviewed_by` ASC) USING BTREE,
  INDEX `fk_purchase_closer`(`closed_by` ASC) USING BTREE,
  INDEX `fk_purchase_reverser`(`reversed_by` ASC) USING BTREE,
  INDEX `fk_purchase_reconciler`(`reconciled_by` ASC) USING BTREE,
  CONSTRAINT `fk_purchase_closer` FOREIGN KEY (`closed_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_purchase_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_purchase_reconciler` FOREIGN KEY (`reconciled_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_purchase_reverser` FOREIGN KEY (`reversed_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_purchase_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_purchase_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_purchase_closed` CHECK ((`status` <> _utf8mb4'CLOSED') or ((`closed_at` is not null) and (`closed_by` is not null) and (`close_reason` is not null))),
  CONSTRAINT `ck_purchase_difference` CHECK ((`reconcile_status` <> _utf8mb4'DIFFERENCE') or ((`status` = _utf8mb4'IN_STOCK') and (`external_total_amount` is not null) and (`reconcile_difference` is not null) and (char_length(`reconcile_difference`) >= 2))),
  CONSTRAINT `ck_purchase_instock` CHECK ((`status` not in (_utf8mb4'IN_STOCK',_utf8mb4'REVERSED')) or ((`stocked_at` is not null) and (`reviewed_at` is not null) and (`reviewed_by` is not null) and (`arrived_at` is not null) and (`total_qty` = `total_received_qty`))),
  CONSTRAINT `ck_purchase_matched` CHECK ((`reconcile_status` <> _utf8mb4'MATCHED') or ((`status` = _utf8mb4'IN_STOCK') and (`external_total_amount` is not null) and (`external_total_amount` = `total_amount`) and (`reconciled_by` is not null) and (`reconciled_at` is not null))),
  CONSTRAINT `ck_purchase_pending` CHECK ((`status` <> _utf8mb4'PENDING') or ((`submitted_at` is not null) and (`first_submitted_at` is not null) and (`arrived_at` is not null) and (`total_qty` = `total_received_qty`))),
  CONSTRAINT `ck_purchase_reconcile` CHECK ((`reconcile_status` in (_utf8mb4'UNRECONCILED',_utf8mb4'DIFFERENCE',_utf8mb4'MATCHED',_utf8mb4'NA')) and ((`external_total_amount` is null) or (`external_total_amount` >= 0))),
  CONSTRAINT `ck_purchase_rejected` CHECK ((`status` <> _utf8mb4'REJECTED') or ((`reviewed_at` is not null) and (`rejection_reason` is not null) and (char_length(`rejection_reason`) >= 2))),
  CONSTRAINT `ck_purchase_reversed` CHECK (((`status` = _utf8mb4'REVERSED') and (`reversed_at` is not null) and (`reversed_by` is not null) and (`reverse_reason` is not null) and (char_length(`reverse_reason`) >= 2) and (`reconcile_status` = _utf8mb4'NA')) or ((`status` <> _utf8mb4'REVERSED') and (`reversed_at` is null) and (`reconcile_status` <> _utf8mb4'NA'))),
  CONSTRAINT `ck_purchase_status` CHECK (`status` in (_utf8mb4'DRAFT',_utf8mb4'PENDING',_utf8mb4'REJECTED',_utf8mb4'IN_STOCK',_utf8mb4'CLOSED',_utf8mb4'REVERSED')),
  CONSTRAINT `ck_purchase_totals` CHECK ((`total_qty` > 0) and (`total_received_qty` >= 0) and (`total_amount` > 0) and (`row_version` >= 0) and (`reconcile_version` >= 0))
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购单与当前对账摘要；核对撤回历史不覆盖，通过operation_log保留' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of purchase_order
-- ----------------------------
INSERT INTO `purchase_order` VALUES (1, 'CG-DEMO-0001', 1, '本地综合供应商', '联系人1', '13900000001', '演示地址1号', 3, '采购张三', 'IN_STOCK', 332, 332, 4718.00, NULL, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000', 1, '系统管理员', '2026-09-01 00:00:00.000000', NULL, '2026-09-01 00:00:00.000000', NULL, NULL, NULL, NULL, NULL, NULL, 'MATCHED', 4718.00, NULL, NULL, '逐行核验一致', 3, '2026-11-10 16:13:25.806904', 1, '多批次、滞销、冻结、调价、边界预警的来源', 0, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `purchase_order` VALUES (2, 'CG-DEMO-0002', 6, '零食供应商', '联系人6', '13900000006', '演示地址6号', 3, '采购张三', 'IN_STOCK', 11, 11, 37.00, NULL, '2026-10-26 16:13:25.806904', '2026-10-26 16:13:25.806904', '2026-10-26 16:13:25.806904', 1, '系统管理员', '2026-10-26 16:13:25.806904', NULL, '2026-10-26 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 2, NULL, 0, '2026-10-26 16:13:25.806904', '2026-10-26 16:13:25.806904');
INSERT INTO `purchase_order` VALUES (3, 'CG-DEMO-0003', 5, '文具供应商', '联系人5', '13900000005', '演示地址5号', 4, '采购李四', 'IN_STOCK', 20, 20, 80.00, NULL, '2026-11-09 16:13:25.806904', '2026-11-09 16:13:25.806904', '2026-11-09 16:13:25.806904', 1, '系统管理员', '2026-11-09 16:13:25.806904', NULL, '2026-11-09 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, NULL, 'DIFFERENCE', 84.00, NULL, '供应商凭据数量多一件，等待核实', NULL, 4, '2026-11-10 16:13:25.806904', 1, NULL, 0, '2026-11-09 16:13:25.806904', '2026-11-09 16:13:25.806904');
INSERT INTO `purchase_order` VALUES (4, 'CG-DEMO-0004', 1, '本地综合供应商', '联系人1', '13900000001', '演示地址1号', 3, '采购张三', 'REVERSED', 4, 4, 32.00, NULL, '2026-11-07 16:13:25.806904', '2026-11-07 16:13:25.806904', '2026-11-07 16:13:25.806904', 1, '系统管理员', '2026-11-07 16:13:25.806904', NULL, '2026-11-07 16:13:25.806904', NULL, NULL, NULL, 1, '2026-11-08 16:13:25.806904', '录入重复采购，尚未使用', 'NA', NULL, NULL, NULL, NULL, NULL, NULL, 0, '已撤销入库的历史样例', 0, '2026-11-07 16:13:25.806904', '2026-11-07 16:13:25.806904');
INSERT INTO `purchase_order` VALUES (5, 'CG-DEMO-0005', 4, '日用品供应商', '联系人4', '13900000004', '演示地址4号', 4, '采购李四', 'IN_STOCK', 3, 3, 10.50, NULL, '2026-11-08 16:13:25.806904', '2026-11-08 16:13:25.806904', '2026-11-08 16:13:25.806904', 1, '系统管理员', '2026-11-08 16:13:25.806904', NULL, '2026-11-08 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 0, '无后续变动且未核对，可测试管理员撤销', 0, '2026-11-08 16:13:25.806904', '2026-11-08 16:13:25.806904');
INSERT INTO `purchase_order` VALUES (6, 'CG-DEMO-0006', 1, '本地综合供应商', '联系人1', '13900000001', '演示地址1号', 3, '采购张三', 'PENDING', 10, 10, 20.00, NULL, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 0, '待审核，可验证审核与撤回竞争', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_order` VALUES (7, 'CG-DEMO-0007', 1, '本地综合供应商', '联系人1', '13900000001', '演示地址1号', 3, '采购张三', 'REJECTED', 8, 8, 40.00, NULL, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', 1, '系统管理员', '2026-11-10 16:13:25.806904', '缺少实物生产日期和到期日期，请补充', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_order` VALUES (8, 'CG-DEMO-0008', 1, '本地综合供应商', '联系人1', '13900000001', '演示地址1号', 4, '采购李四', 'DRAFT', 2, 0, 4.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 0, '未提交草稿，可删除', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_order` VALUES (9, 'CG-DEMO-0009', 1, '本地综合供应商', '联系人1', '13900000001', '演示地址1号', 3, '采购张三', 'CLOSED', 3, 3, 6.00, NULL, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, 1, '2026-11-10 16:13:25.806904', '取消本次未生效采购', NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_order` VALUES (10, 'CG-DEMO-0010', 1, '本地综合供应商', '联系人1', '13900000001', '演示地址1号', 3, '采购张三', 'DRAFT', 6, 0, 288.00, NULL, NULL, '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 0, '已撤回草稿，不允许硬删', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');

-- ----------------------------
-- Table structure for sale_allocation
-- ----------------------------
DROP TABLE IF EXISTS `sale_allocation`;
CREATE TABLE `sale_allocation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sale_item_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `batch_item_id` bigint NOT NULL COMMENT 'purchase_item.id，即系统库存批次ID',
  `quantity` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_allocation_line_batch`(`sale_item_id` ASC, `batch_item_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_allocation_source`(`id` ASC, `batch_item_id` ASC, `product_id` ASC) USING BTREE,
  INDEX `fk_alloc_sitem`(`sale_item_id` ASC, `product_id` ASC) USING BTREE,
  INDEX `fk_alloc_batch`(`batch_item_id` ASC, `product_id` ASC) USING BTREE,
  CONSTRAINT `fk_alloc_batch` FOREIGN KEY (`batch_item_id`, `product_id`) REFERENCES `purchase_item` (`id`, `product_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alloc_sitem` FOREIGN KEY (`sale_item_id`, `product_id`) REFERENCES `sale_item` (`id`, `product_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_allocation_qty` CHECK (`quantity` between 1 and 999999)
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '按FEFO/FIFO扣库明细，也是销售纠错恢复批次的依据' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sale_allocation
-- ----------------------------
INSERT INTO `sale_allocation` VALUES (1, 1, 5, 5, 3);
INSERT INTO `sale_allocation` VALUES (2, 2, 7, 7, 2);
INSERT INTO `sale_allocation` VALUES (3, 3, 5, 5, 2);
INSERT INTO `sale_allocation` VALUES (4, 4, 8, 8, 3);
INSERT INTO `sale_allocation` VALUES (5, 5, 23, 18, 2);
INSERT INTO `sale_allocation` VALUES (6, 6, 1, 1, 10);
INSERT INTO `sale_allocation` VALUES (7, 6, 1, 2, 2);
INSERT INTO `sale_allocation` VALUES (8, 7, 2, 3, 2);
INSERT INTO `sale_allocation` VALUES (9, 8, 7, 7, 1);
INSERT INTO `sale_allocation` VALUES (10, 9, 1, 2, 2);

-- ----------------------------
-- Table structure for sale_item
-- ----------------------------
DROP TABLE IF EXISTS `sale_item`;
CREATE TABLE `sale_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sale_id` bigint NOT NULL,
  `line_no` int NOT NULL,
  `product_id` bigint NOT NULL,
  `product_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `product_barcode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category_id` bigint NOT NULL,
  `category_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `specification` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `unit` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `unit_price` decimal(16, 2) NOT NULL,
  `quantity` int NOT NULL,
  `line_amount` decimal(16, 2) GENERATED ALWAYS AS ((`unit_price` * `quantity`)) STORED NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sale_line`(`sale_id` ASC, `line_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_sale_product`(`sale_id` ASC, `product_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_sitem_product`(`id` ASC, `product_id` ASC) USING BTREE,
  INDEX `ix_sitem_product`(`product_id` ASC, `sale_id` ASC) USING BTREE,
  INDEX `fk_sitem_category`(`category_id` ASC) USING BTREE,
  CONSTRAINT `fk_sitem_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sitem_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sitem_sale` FOREIGN KEY (`sale_id`) REFERENCES `sale_order` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_sitem_values` CHECK ((`line_no` > 0) and (`unit_price` > 0) and (`quantity` between 1 and 999999))
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '销售商品明细，不包含收银角色无权查看的采购成本' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sale_item
-- ----------------------------
INSERT INTO `sale_item` VALUES (1, 1, 1, 5, 'P0005', '6900000000005', '大米五公斤', 2, '粮油食品', '基本单位包装', '件', 68.00, 3, DEFAULT);
INSERT INTO `sale_item` VALUES (2, 1, 2, 7, 'P0007', '6900000000007', '抽纸三包装', 3, '日用清洁', '基本单位包装', '件', 12.00, 2, DEFAULT);
INSERT INTO `sale_item` VALUES (3, 2, 1, 5, 'P0005', '6900000000005', '大米五公斤', 2, '粮油食品', '基本单位包装', '件', 68.00, 2, DEFAULT);
INSERT INTO `sale_item` VALUES (4, 2, 2, 8, 'P0008', '6900000000008', '香皂', 3, '日用清洁', '基本单位包装', '件', 6.00, 3, DEFAULT);
INSERT INTO `sale_item` VALUES (5, 3, 1, 23, 'P0023', '6900000000023', '调价快照测试商品', 3, '日用清洁', '基本单位包装', '件', 7.50, 2, DEFAULT);
INSERT INTO `sale_item` VALUES (6, 4, 1, 1, 'P0001', '6900000000001', '演示瓶装饮料', 1, '饮料乳品', '基本单位包装', '瓶', 5.00, 12, DEFAULT);
INSERT INTO `sale_item` VALUES (7, 5, 1, 2, 'P0002', '6900000000002', '纯牛奶', 1, '饮料乳品', '基本单位包装', '件', 6.00, 2, DEFAULT);
INSERT INTO `sale_item` VALUES (8, 6, 1, 7, 'P0007', '6900000000007', '抽纸三包装', 3, '日用清洁', '基本单位包装', '件', 12.00, 1, DEFAULT);
INSERT INTO `sale_item` VALUES (9, 7, 1, 1, 'P0001', '6900000000001', '演示瓶装饮料', 1, '饮料乳品', '基本单位包装', '瓶', 5.00, 2, DEFAULT);

-- ----------------------------
-- Table structure for sale_order
-- ----------------------------
DROP TABLE IF EXISTS `sale_order`;
CREATE TABLE `sale_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `request_key` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '确认销售唯一幂等标识，重试返回原单',
  `request_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '请求规范化摘要，同key不同内容必须拒绝',
  `cashier_id` bigint NOT NULL,
  `cashier_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'COMPLETED',
  `total_qty` bigint NOT NULL,
  `total_amount` decimal(16, 2) NOT NULL,
  `payment_method` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'CASH/MOCK_EPAY',
  `received_amount` decimal(16, 2) NOT NULL,
  `change_amount` decimal(16, 2) GENERATED ALWAYS AS ((`received_amount` - `total_amount`)) STORED NULL,
  `completed_at` datetime(6) NOT NULL,
  `corrected_by` bigint NULL DEFAULT NULL,
  `corrector_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `corrected_at` datetime(6) NULL DEFAULT NULL,
  `correction_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `row_version` bigint NOT NULL DEFAULT 0,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sale_no`(`order_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_sale_request`(`request_key` ASC) USING BTREE,
  INDEX `ix_sale_cashier`(`cashier_id` ASC, `completed_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_sale_time`(`completed_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_sale_correction`(`corrected_at` ASC, `id` ASC) USING BTREE,
  INDEX `fk_sale_corrector`(`corrected_by` ASC) USING BTREE,
  CONSTRAINT `fk_sale_cashier` FOREIGN KEY (`cashier_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sale_corrector` FOREIGN KEY (`corrected_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_sale_amount` CHECK ((`total_qty` > 0) and (`total_amount` > 0) and (`received_amount` >= `total_amount`) and (`row_version` >= 0)),
  CONSTRAINT `ck_sale_corrected` CHECK (((`status` = _utf8mb4'COMPLETED') and (`corrected_at` is null) and (`corrected_by` is null)) or ((`status` = _utf8mb4'CORRECTED') and (`corrected_at` is not null) and (`corrected_by` is not null) and (`correction_reason` is not null) and (char_length(`correction_reason`) >= 2) and (cast(`corrected_at` as date) = cast(`completed_at` as date)) and (`corrected_at` >= `completed_at`))),
  CONSTRAINT `ck_sale_payment` CHECK ((`payment_method` in (_utf8mb4'CASH',_utf8mb4'MOCK_EPAY')) and ((`payment_method` <> _utf8mb4'MOCK_EPAY') or (`received_amount` = `total_amount`))),
  CONSTRAINT `ck_sale_status` CHECK (`status` in (_utf8mb4'COMPLETED',_utf8mb4'CORRECTED'))
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '已完成销售及最多一次当天整单纠错，保留原金额和时间' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sale_order
-- ----------------------------
INSERT INTO `sale_order` VALUES (1, 'XS-DEMO-0001', 'seed-sale-request-1', '23f928fb4160417181a3270e267935fa7f8c010ad64cf1fbe6380bda15ac1e16', 5, '收银王五', 'COMPLETED', 5, 228.00, 'CASH', 228.00, DEFAULT, '2026-09-11 00:00:00.000000', NULL, NULL, NULL, NULL, 0, '2026-09-11 00:00:00.000000');
INSERT INTO `sale_order` VALUES (2, 'XS-DEMO-0002', 'seed-sale-request-2', '4c8fb025429c03b83ee1c3d4d7372441e5ba5b5b565654c4cf03096db3b3a580', 6, '收银赵六', 'COMPLETED', 5, 154.00, 'MOCK_EPAY', 154.00, DEFAULT, '2026-10-06 00:00:00.000000', NULL, NULL, NULL, NULL, 0, '2026-10-06 00:00:00.000000');
INSERT INTO `sale_order` VALUES (3, 'XS-DEMO-0003', 'seed-sale-request-3', '4e44ea03c2a63a7dce9da411a9854da2b11e857c57ecd4b1434145a525214395', 6, '收银赵六', 'COMPLETED', 2, 15.00, 'CASH', 15.00, DEFAULT, '2026-11-05 16:13:25.806904', NULL, NULL, NULL, NULL, 0, '2026-11-05 16:13:25.806904');
INSERT INTO `sale_order` VALUES (4, 'XS-DEMO-0004', 'seed-sale-request-4', '26b463ce1813d543d4a1eb9067871972acc5413d08f85391aaa96cea3fb477fc', 5, '收银王五', 'COMPLETED', 12, 60.00, 'CASH', 100.00, DEFAULT, '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, 0, '2026-11-10 16:13:25.806904');
INSERT INTO `sale_order` VALUES (5, 'XS-DEMO-0005', 'seed-sale-request-5', '9c1705ad7806a05ce9ffa7282d2bc571c41ab5f6388a9f03cd37bd21728e0f85', 6, '收银赵六', 'COMPLETED', 2, 12.00, 'MOCK_EPAY', 12.00, DEFAULT, '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, 0, '2026-11-10 16:13:25.806904');
INSERT INTO `sale_order` VALUES (6, 'XS-DEMO-0006', 'seed-sale-request-6', 'b2a16ff79e865a6d3329c50c5468a9a426668810f842c6490a5b4ee4ac364d61', 6, '收银赵六', 'CORRECTED', 1, 12.00, 'CASH', 12.00, DEFAULT, '2026-11-10 16:13:25.806904', 1, '系统管理员', '2026-11-10 16:13:25.806904', '当天整单误录，货品完整在店', 0, '2026-11-10 16:13:25.806904');
INSERT INTO `sale_order` VALUES (7, 'XS-DEMO-0007', 'seed-sale-request-7', '608c0e16ec2fb03c93985b29b93d630d0df47580a8cc7709b5c7b6b82e6caf9d', 5, '收银王五', 'COMPLETED', 2, 10.00, 'CASH', 10.00, DEFAULT, '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, 0, '2026-11-10 16:13:25.806904');

-- ----------------------------
-- Table structure for stock_movement
-- ----------------------------
DROP TABLE IF EXISTS `stock_movement`;
CREATE TABLE `stock_movement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'PURCHASE_IN/PURCHASE_REVERSE/SALE_OUT/SALE_CORRECTION/STOCKTAKE_GAIN/STOCKTAKE_LOSS',
  `product_id` bigint NOT NULL,
  `batch_item_id` bigint NOT NULL,
  `sale_allocation_id` bigint NULL DEFAULT NULL,
  `stocktake_item_id` bigint NULL DEFAULT NULL,
  `source_key` bigint GENERATED ALWAYS AS (coalesce(`sale_allocation_id`,`stocktake_item_id`,`batch_item_id`)) STORED NULL,
  `business_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `delta_qty` bigint NOT NULL,
  `batch_before` bigint NOT NULL,
  `batch_after` bigint NOT NULL,
  `product_before` bigint NOT NULL,
  `product_after` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `occurred_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_movement_once`(`event_type` ASC, `source_key` ASC) USING BTREE COMMENT '同类来源只能入账一次，正反向类型不同',
  INDEX `ix_movement_product`(`product_id` ASC, `occurred_at` ASC, `id` ASC) USING BTREE,
  INDEX `ix_movement_batch`(`batch_item_id` ASC, `id` ASC) USING BTREE,
  INDEX `ix_movement_business`(`business_no` ASC, `event_type` ASC) USING BTREE,
  INDEX `fk_movement_batch`(`batch_item_id` ASC, `product_id` ASC) USING BTREE,
  INDEX `fk_movement_allocation`(`sale_allocation_id` ASC, `batch_item_id` ASC, `product_id` ASC) USING BTREE,
  INDEX `fk_movement_stocktake`(`stocktake_item_id` ASC, `batch_item_id` ASC, `product_id` ASC) USING BTREE,
  INDEX `fk_movement_actor`(`actor_id` ASC) USING BTREE,
  CONSTRAINT `fk_movement_actor` FOREIGN KEY (`actor_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_movement_allocation` FOREIGN KEY (`sale_allocation_id`, `batch_item_id`, `product_id`) REFERENCES `sale_allocation` (`id`, `batch_item_id`, `product_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_movement_batch` FOREIGN KEY (`batch_item_id`, `product_id`) REFERENCES `purchase_item` (`id`, `product_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_movement_stocktake` FOREIGN KEY (`stocktake_item_id`, `batch_item_id`, `product_id`) REFERENCES `stocktake_item` (`id`, `batch_item_id`, `product_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_movement_math` CHECK ((`delta_qty` <> 0) and (`batch_before` >= 0) and (`batch_after` >= 0) and (`product_before` >= 0) and (`product_after` >= 0) and ((`batch_before` + `delta_qty`) = `batch_after`) and ((`product_before` + `delta_qty`) = `product_after`)),
  CONSTRAINT `ck_movement_sign` CHECK (((`event_type` in (_utf8mb4'PURCHASE_IN',_utf8mb4'SALE_CORRECTION',_utf8mb4'STOCKTAKE_GAIN')) and (`delta_qty` > 0)) or ((`event_type` in (_utf8mb4'PURCHASE_REVERSE',_utf8mb4'SALE_OUT',_utf8mb4'STOCKTAKE_LOSS')) and (`delta_qty` < 0))),
  CONSTRAINT `ck_movement_source` CHECK (((`event_type` in (_utf8mb4'PURCHASE_IN',_utf8mb4'PURCHASE_REVERSE')) and (`sale_allocation_id` is null) and (`stocktake_item_id` is null)) or ((`event_type` in (_utf8mb4'SALE_OUT',_utf8mb4'SALE_CORRECTION')) and (`sale_allocation_id` is not null) and (`stocktake_item_id` is null)) or ((`event_type` in (_utf8mb4'STOCKTAKE_GAIN',_utf8mb4'STOCKTAKE_LOSS')) and (`stocktake_item_id` is not null) and (`sale_allocation_id` is null))),
  CONSTRAINT `ck_movement_type` CHECK (`event_type` in (_utf8mb4'PURCHASE_IN',_utf8mb4'PURCHASE_REVERSE',_utf8mb4'SALE_OUT',_utf8mb4'SALE_CORRECTION',_utf8mb4'STOCKTAKE_GAIN',_utf8mb4'STOCKTAKE_LOSS'))
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '不可任意编辑删除的库存流水，正反事件构建统计视图，防止冲销覆盖历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock_movement
-- ----------------------------
INSERT INTO `stock_movement` VALUES (1, 'PURCHASE_IN', 1, 1, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 10, 0, 10, 0, 10, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (2, 'PURCHASE_IN', 1, 2, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 10, 0, 10, 10, 20, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (3, 'PURCHASE_IN', 2, 3, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 40, 0, 40, 0, 40, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (4, 'PURCHASE_IN', 3, 4, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 12, 0, 12, 0, 12, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (5, 'PURCHASE_IN', 5, 5, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 50, 0, 50, 0, 50, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (6, 'PURCHASE_IN', 6, 6, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 30, 0, 30, 0, 30, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (7, 'PURCHASE_IN', 7, 7, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 40, 0, 40, 0, 40, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (8, 'PURCHASE_IN', 8, 8, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 30, 0, 30, 0, 30, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (9, 'PURCHASE_IN', 9, 9, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 5, 0, 5, 0, 5, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (10, 'PURCHASE_IN', 10, 10, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 20, 0, 20, 0, 20, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (11, 'PURCHASE_IN', 11, 11, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 10, 0, 10, 0, 10, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (12, 'PURCHASE_IN', 12, 12, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 12, 0, 12, 0, 12, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (13, 'PURCHASE_IN', 13, 13, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 8, 0, 8, 0, 8, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (14, 'PURCHASE_IN', 14, 14, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 7, 0, 7, 0, 7, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (15, 'PURCHASE_IN', 15, 15, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 8, 0, 8, 0, 8, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (16, 'PURCHASE_IN', 18, 16, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 4, 0, 4, 0, 4, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (17, 'PURCHASE_IN', 19, 17, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 6, 0, 6, 0, 6, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (18, 'PURCHASE_IN', 23, 18, NULL, NULL, DEFAULT, 'CG-DEMO-0001', 30, 0, 30, 0, 30, 1, '系统管理员', NULL, '2026-09-01 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (19, 'SALE_OUT', 5, 5, 1, NULL, DEFAULT, 'XS-DEMO-0001', -3, 50, 47, 50, 47, 5, '收银王五', NULL, '2026-09-11 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (20, 'SALE_OUT', 7, 7, 2, NULL, DEFAULT, 'XS-DEMO-0001', -2, 40, 38, 40, 38, 5, '收银王五', NULL, '2026-09-11 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (21, 'SALE_OUT', 5, 5, 3, NULL, DEFAULT, 'XS-DEMO-0002', -2, 47, 45, 47, 45, 6, '收银赵六', NULL, '2026-10-06 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (22, 'SALE_OUT', 8, 8, 4, NULL, DEFAULT, 'XS-DEMO-0002', -3, 30, 27, 30, 27, 6, '收银赵六', NULL, '2026-10-06 00:00:00.000000');
INSERT INTO `stock_movement` VALUES (23, 'PURCHASE_IN', 4, 19, NULL, NULL, DEFAULT, 'CG-DEMO-0002', 6, 0, 6, 0, 6, 1, '系统管理员', NULL, '2026-10-26 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (24, 'PURCHASE_IN', 20, 20, NULL, NULL, DEFAULT, 'CG-DEMO-0002', 5, 0, 5, 0, 5, 1, '系统管理员', NULL, '2026-10-26 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (25, 'SALE_OUT', 23, 18, 5, NULL, DEFAULT, 'XS-DEMO-0003', -2, 30, 28, 30, 28, 6, '收银赵六', NULL, '2026-11-05 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (26, 'PURCHASE_IN', 7, 22, NULL, NULL, DEFAULT, 'CG-DEMO-0004', 4, 0, 4, 38, 42, 1, '系统管理员', NULL, '2026-11-07 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (27, 'PURCHASE_IN', 8, 23, NULL, NULL, DEFAULT, 'CG-DEMO-0005', 3, 0, 3, 27, 30, 1, '系统管理员', NULL, '2026-11-08 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (28, 'PURCHASE_REVERSE', 7, 22, NULL, NULL, DEFAULT, 'CG-DEMO-0004', -4, 4, 0, 42, 38, 1, '系统管理员', '录入重复采购，尚未使用', '2026-11-08 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (29, 'PURCHASE_IN', 17, 21, NULL, NULL, DEFAULT, 'CG-DEMO-0003', 20, 0, 20, 0, 20, 1, '系统管理员', NULL, '2026-11-09 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (30, 'SALE_OUT', 1, 1, 6, NULL, DEFAULT, 'XS-DEMO-0004', -10, 10, 0, 20, 10, 5, '收银王五', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (31, 'SALE_OUT', 1, 2, 7, NULL, DEFAULT, 'XS-DEMO-0004', -2, 10, 8, 10, 8, 5, '收银王五', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (32, 'SALE_OUT', 2, 3, 8, NULL, DEFAULT, 'XS-DEMO-0005', -2, 40, 38, 40, 38, 6, '收银赵六', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (33, 'SALE_OUT', 7, 7, 9, NULL, DEFAULT, 'XS-DEMO-0006', -1, 38, 37, 38, 37, 6, '收银赵六', NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (34, 'SALE_CORRECTION', 7, 7, 9, NULL, DEFAULT, 'XS-DEMO-0006', 1, 37, 38, 37, 38, 1, '系统管理员', '当天整单误录，货品完整在店', '2026-11-10 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (35, 'STOCKTAKE_LOSS', 1, 2, NULL, 1, DEFAULT, 'PD-DEMO-0001', -1, 8, 7, 8, 7, 1, '系统管理员', '破损损耗一瓶', '2026-11-10 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (36, 'STOCKTAKE_GAIN', 7, 7, NULL, 2, DEFAULT, 'PD-DEMO-0001', 2, 38, 40, 38, 40, 1, '系统管理员', '复核原先漏记两件', '2026-11-10 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (37, 'STOCKTAKE_LOSS', 20, 20, NULL, 4, DEFAULT, 'PD-DEMO-0001', -5, 5, 0, 5, 0, 1, '系统管理员', '过期实物已完整清理', '2026-11-10 16:13:25.806904');
INSERT INTO `stock_movement` VALUES (38, 'SALE_OUT', 1, 2, 10, NULL, DEFAULT, 'XS-DEMO-0007', -2, 7, 5, 7, 5, 5, '收银王五', NULL, '2026-11-10 16:13:25.806904');

-- ----------------------------
-- Table structure for stocktake_item
-- ----------------------------
DROP TABLE IF EXISTS `stocktake_item`;
CREATE TABLE `stocktake_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stocktake_id` bigint NOT NULL,
  `batch_item_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `book_qty` bigint NOT NULL,
  `actual_qty` bigint NULL DEFAULT NULL,
  `difference_qty` bigint GENERATED ALWAYS AS ((`actual_qty` - `book_qty`)) STORED NULL,
  `reason_type` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'COUNT_ERROR/LOSS/EXPIRY/OTHER',
  `reason_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stocktake_batch`(`stocktake_id` ASC, `batch_item_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_stitem_source`(`id` ASC, `batch_item_id` ASC, `product_id` ASC) USING BTREE,
  INDEX `ix_stocktake_product`(`product_id` ASC, `stocktake_id` ASC) USING BTREE,
  INDEX `fk_stitem_batch`(`batch_item_id` ASC, `product_id` ASC) USING BTREE,
  CONSTRAINT `fk_stitem_batch` FOREIGN KEY (`batch_item_id`, `product_id`) REFERENCES `purchase_item` (`id`, `product_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_stitem_order` FOREIGN KEY (`stocktake_id`) REFERENCES `stocktake_order` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_stitem_qty` CHECK ((`book_qty` between 0 and 999999999999) and ((`actual_qty` is null) or (`actual_qty` between 0 and 999999999999))),
  CONSTRAINT `ck_stitem_reason` CHECK ((`actual_qty` is null) or (`actual_qty` = `book_qty`) or ((`reason_type` is not null) and (`reason_note` is not null) and (char_length(`reason_note`) >= 2))),
  CONSTRAINT `ck_stitem_reason_type` CHECK ((`reason_type` is null) or (`reason_type` in (_utf8mb4'COUNT_ERROR',_utf8mb4'LOSS',_utf8mb4'EXPIRY',_utf8mb4'OTHER')))
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '批次账面快照与实盘差异；不允许未知来源盘盈' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stocktake_item
-- ----------------------------
INSERT INTO `stocktake_item` VALUES (1, 1, 2, 1, 8, 7, DEFAULT, 'LOSS', '破损损耗一瓶');
INSERT INTO `stocktake_item` VALUES (2, 1, 7, 7, 38, 40, DEFAULT, 'COUNT_ERROR', '复核原先漏记两件');
INSERT INTO `stocktake_item` VALUES (3, 1, 5, 5, 45, 45, DEFAULT, NULL, NULL);
INSERT INTO `stocktake_item` VALUES (4, 1, 20, 20, 5, 0, DEFAULT, 'EXPIRY', '过期实物已完整清理');
INSERT INTO `stocktake_item` VALUES (5, 2, 10, 10, 20, NULL, DEFAULT, NULL, NULL);
INSERT INTO `stocktake_item` VALUES (6, 3, 11, 11, 10, 10, DEFAULT, NULL, NULL);
INSERT INTO `stocktake_item` VALUES (7, 4, 12, 12, 12, 11, DEFAULT, 'LOSS', '盘亏待复核');
INSERT INTO `stocktake_item` VALUES (8, 5, 8, 8, 27, 27, DEFAULT, NULL, NULL);

-- ----------------------------
-- Table structure for stocktake_order
-- ----------------------------
DROP TABLE IF EXISTS `stocktake_order`;
CREATE TABLE `stocktake_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `operator_id` bigint NOT NULL,
  `operator_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'COUNTING' COMMENT 'COUNTING/PENDING/REJECTED/COMPLETED/CLOSED',
  `started_at` datetime(6) NOT NULL,
  `submitted_at` datetime(6) NULL DEFAULT NULL,
  `reviewed_by` bigint NULL DEFAULT NULL,
  `reviewer_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reviewed_at` datetime(6) NULL DEFAULT NULL,
  `rejection_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `completed_at` datetime(6) NULL DEFAULT NULL,
  `closed_by` bigint NULL DEFAULT NULL,
  `closed_at` datetime(6) NULL DEFAULT NULL,
  `close_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `row_version` bigint NOT NULL DEFAULT 0,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stocktake_no`(`order_no` ASC) USING BTREE,
  INDEX `ix_stocktake_owner`(`operator_id` ASC, `status` ASC, `started_at` ASC) USING BTREE,
  INDEX `ix_stocktake_status`(`status` ASC, `submitted_at` ASC) USING BTREE,
  INDEX `fk_stocktake_reviewer`(`reviewed_by` ASC) USING BTREE,
  INDEX `fk_stocktake_closer`(`closed_by` ASC) USING BTREE,
  CONSTRAINT `fk_stocktake_closer` FOREIGN KEY (`closed_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_stocktake_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_stocktake_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_stocktake_closed` CHECK ((`status` <> _utf8mb4'CLOSED') or ((`closed_at` is not null) and (`closed_by` is not null) and (`close_reason` is not null))),
  CONSTRAINT `ck_stocktake_done` CHECK ((`status` <> _utf8mb4'COMPLETED') or ((`completed_at` is not null) and (`reviewed_at` is not null) and (`reviewed_by` is not null))),
  CONSTRAINT `ck_stocktake_pending` CHECK ((`status` not in (_utf8mb4'PENDING',_utf8mb4'REJECTED',_utf8mb4'COMPLETED')) or (`submitted_at` is not null)),
  CONSTRAINT `ck_stocktake_reject` CHECK ((`status` <> _utf8mb4'REJECTED') or ((`rejection_reason` is not null) and (char_length(`rejection_reason`) >= 2) and (`reviewed_at` is not null))),
  CONSTRAINT `ck_stocktake_status` CHECK (`status` in (_utf8mb4'COUNTING',_utf8mb4'PENDING',_utf8mb4'REJECTED',_utf8mb4'COMPLETED',_utf8mb4'CLOSED')),
  CONSTRAINT `ck_stocktake_version` CHECK (`row_version` >= 0)
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '盘点任务、审核及冻结生命周期' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stocktake_order
-- ----------------------------
INSERT INTO `stocktake_order` VALUES (1, 'PD-DEMO-0001', 3, '采购张三', 'COMPLETED', '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', 1, '系统管理员', '2026-11-10 16:13:25.806904', NULL, '2026-11-10 16:13:25.806904', NULL, NULL, NULL, '测试冻结、空值、差异、审核和关闭', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `stocktake_order` VALUES (2, 'PD-DEMO-0002', 3, '采购张三', 'COUNTING', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '测试冻结、空值、差异、审核和关闭', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `stocktake_order` VALUES (3, 'PD-DEMO-0003', 4, '采购李四', 'PENDING', '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '测试冻结、空值、差异、审核和关闭', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `stocktake_order` VALUES (4, 'PD-DEMO-0004', 3, '采购张三', 'REJECTED', '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', 1, '系统管理员', '2026-11-10 16:13:25.806904', '实盘说明待补充核实', NULL, NULL, NULL, NULL, '测试冻结、空值、差异、审核和关闭', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `stocktake_order` VALUES (5, 'PD-DEMO-0005', 4, '采购李四', 'CLOSED', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-11-10 16:13:25.806904', '取消本次盘点并释放冻结', '测试冻结、空值、差异、审核和关闭', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');

-- ----------------------------
-- Table structure for supplier
-- ----------------------------
DROP TABLE IF EXISTS `supplier`;
CREATE TABLE `supplier`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `contact_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_supplier_code`(`code` ASC) USING BTREE,
  UNIQUE INDEX `uk_supplier_name`(`name` ASC) USING BTREE,
  INDEX `ix_supplier_enabled`(`enabled` ASC, `id` ASC) USING BTREE,
  INDEX `fk_supplier_user`(`created_by` ASC) USING BTREE,
  CONSTRAINT `fk_supplier_user` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_supplier_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `ck_supplier_names` CHECK ((char_length(`name`) between 2 and 100) and (char_length(`contact_name`) between 2 and 30)),
  CONSTRAINT `ck_supplier_phone` CHECK (char_length(`contact_phone`) between 6 and 20)
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '供应商；历史采购使用单头快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of supplier
-- ----------------------------
INSERT INTO `supplier` VALUES (1, 'SUP001', '本地综合供应商', '联系人1', '13900000001', '演示地址1号', 1, '毕业设计测试档案', 3, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `supplier` VALUES (2, 'SUP002', '乳品供应商', '联系人2', '13900000002', '演示地址2号', 1, '毕业设计测试档案', 3, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `supplier` VALUES (3, 'SUP003', '粮油供应商', '联系人3', '13900000003', '演示地址3号', 1, '毕业设计测试档案', 3, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `supplier` VALUES (4, 'SUP004', '日用品供应商', '联系人4', '13900000004', '演示地址4号', 1, '毕业设计测试档案', 3, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `supplier` VALUES (5, 'SUP005', '文具供应商', '联系人5', '13900000005', '演示地址5号', 1, '毕业设计测试档案', 3, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `supplier` VALUES (6, 'SUP006', '零食供应商', '联系人6', '13900000006', '演示地址6号', 1, '毕业设计测试档案', 3, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `supplier` VALUES (7, 'SUP007', '停用供应商', '联系人7', '13900000007', '演示地址7号', 0, '毕业设计测试档案', 3, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `supplier` VALUES (8, 'SUP008', '未引用供应商', '联系人8', '13900000008', '演示地址8号', 1, '毕业设计测试档案', 3, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `config_key` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `config_value` json NOT NULL COMMENT 'JSON 数值/数组/对象，具体类型由配置键限定',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `editable` tinyint NOT NULL DEFAULT 1 COMMENT '权限目录为只读0，普通业务参数为1',
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`config_key`) USING BTREE,
  INDEX `fk_config_user`(`updated_by` ASC) USING BTREE,
  CONSTRAINT `fk_config_user` FOREIGN KEY (`updated_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `ck_config_editable` CHECK (`editable` in (0,1))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '默认库存下限、默认临期天数和菜单按钮权限目录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES ('demo.seed_info', '{\"image\": \"测试图片.jpg\", \"fixture\": true, \"password\": \"见README，不在配置中存口令\"}', '测试数据说明', 0, 1, '2026-11-10 16:13:25.806904');
INSERT INTO `sys_config` VALUES ('inventory.default_lower_limit', '5', '新建商品默认库存下限，不覆盖已有商品', 1, 1, '2026-11-10 16:13:25.806904');
INSERT INTO `sys_config` VALUES ('inventory.default_near_expiry_days', '7', '新建商品默认临期天数', 1, 1, '2026-11-10 16:13:25.806904');
INSERT INTO `sys_config` VALUES ('security.permission_catalog', '[{\"code\": \"common\", \"kind\": \"MODULE\", \"label\": \"公共功能\", \"roles\": [\"ADMIN\", \"PURCHASER\", \"CASHIER\"], \"parent\": null}, {\"code\": \"common.dashboard\", \"kind\": \"ACTION\", \"label\": \"工作台\", \"roles\": [\"ADMIN\", \"PURCHASER\", \"CASHIER\"], \"parent\": \"common\"}, {\"code\": \"common.profile\", \"kind\": \"ACTION\", \"label\": \"个人中心\", \"roles\": [\"ADMIN\", \"PURCHASER\", \"CASHIER\"], \"parent\": \"common\"}, {\"code\": \"common.avatar\", \"kind\": \"ACTION\", \"label\": \"更换或移除本人头像\", \"roles\": [\"ADMIN\", \"PURCHASER\", \"CASHIER\"], \"parent\": \"common\"}, {\"code\": \"common.password\", \"kind\": \"ACTION\", \"label\": \"修改本人密码\", \"roles\": [\"ADMIN\", \"PURCHASER\", \"CASHIER\"], \"parent\": \"common\"}, {\"code\": \"common.logout\", \"kind\": \"ACTION\", \"label\": \"退出\", \"roles\": [\"ADMIN\", \"PURCHASER\", \"CASHIER\"], \"parent\": \"common\"}, {\"code\": \"system\", \"kind\": \"MODULE\", \"label\": \"系统管理\", \"roles\": [\"ADMIN\"], \"parent\": null}, {\"code\": \"system.user\", \"kind\": \"PAGE\", \"label\": \"用户管理\", \"roles\": [\"ADMIN\"], \"parent\": \"system\"}, {\"code\": \"system.role\", \"kind\": \"PAGE\", \"label\": \"角色权限\", \"roles\": [\"ADMIN\"], \"parent\": \"system\"}, {\"code\": \"system.config\", \"kind\": \"PAGE\", \"label\": \"业务参数\", \"roles\": [\"ADMIN\"], \"parent\": \"system\"}, {\"code\": \"system.log\", \"kind\": \"PAGE\", \"label\": \"操作记录\", \"roles\": [\"ADMIN\"], \"parent\": \"system\"}, {\"code\": \"system.user.create\", \"kind\": \"ACTION\", \"label\": \"create\", \"roles\": [\"ADMIN\"], \"parent\": \"system.user\"}, {\"code\": \"system.user.edit\", \"kind\": \"ACTION\", \"label\": \"edit\", \"roles\": [\"ADMIN\"], \"parent\": \"system.user\"}, {\"code\": \"system.user.enable\", \"kind\": \"ACTION\", \"label\": \"enable\", \"roles\": [\"ADMIN\"], \"parent\": \"system.user\"}, {\"code\": \"system.user.reset_password\", \"kind\": \"ACTION\", \"label\": \"reset_password\", \"roles\": [\"ADMIN\"], \"parent\": \"system.user\"}, {\"code\": \"system.user.delete\", \"kind\": \"ACTION\", \"label\": \"delete\", \"roles\": [\"ADMIN\"], \"parent\": \"system.user\"}, {\"code\": \"system.role.save\", \"kind\": \"ACTION\", \"label\": \"保存权限\", \"roles\": [\"ADMIN\"], \"parent\": \"system.role\"}, {\"code\": \"system.role.restore\", \"kind\": \"ACTION\", \"label\": \"恢复默认\", \"roles\": [\"ADMIN\"], \"parent\": \"system.role\"}, {\"code\": \"system.config.save\", \"kind\": \"ACTION\", \"label\": \"保存参数\", \"roles\": [\"ADMIN\"], \"parent\": \"system.config\"}, {\"code\": \"base\", \"kind\": \"MODULE\", \"label\": \"基础信息\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": null}, {\"code\": \"base.category\", \"kind\": \"PAGE\", \"label\": \"商品分类\", \"roles\": [\"ADMIN\"], \"parent\": \"base\"}, {\"code\": \"base.category.create\", \"kind\": \"ACTION\", \"label\": \"create\", \"roles\": [\"ADMIN\"], \"parent\": \"base.category\"}, {\"code\": \"base.category.edit\", \"kind\": \"ACTION\", \"label\": \"edit\", \"roles\": [\"ADMIN\"], \"parent\": \"base.category\"}, {\"code\": \"base.category.enable\", \"kind\": \"ACTION\", \"label\": \"enable\", \"roles\": [\"ADMIN\"], \"parent\": \"base.category\"}, {\"code\": \"base.category.delete\", \"kind\": \"ACTION\", \"label\": \"delete\", \"roles\": [\"ADMIN\"], \"parent\": \"base.category\"}, {\"code\": \"base.product\", \"kind\": \"PAGE\", \"label\": \"商品档案\", \"roles\": [\"ADMIN\"], \"parent\": \"base\"}, {\"code\": \"base.product.create\", \"kind\": \"ACTION\", \"label\": \"create\", \"roles\": [\"ADMIN\"], \"parent\": \"base.product\"}, {\"code\": \"base.product.edit\", \"kind\": \"ACTION\", \"label\": \"edit\", \"roles\": [\"ADMIN\"], \"parent\": \"base.product\"}, {\"code\": \"base.product.enable\", \"kind\": \"ACTION\", \"label\": \"enable\", \"roles\": [\"ADMIN\"], \"parent\": \"base.product\"}, {\"code\": \"base.product.delete\", \"kind\": \"ACTION\", \"label\": \"delete\", \"roles\": [\"ADMIN\"], \"parent\": \"base.product\"}, {\"code\": \"base.supplier\", \"kind\": \"PAGE\", \"label\": \"供应商\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"base\"}, {\"code\": \"base.supplier.create\", \"kind\": \"ACTION\", \"label\": \"create\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"base.supplier\"}, {\"code\": \"base.supplier.edit\", \"kind\": \"ACTION\", \"label\": \"edit\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"base.supplier\"}, {\"code\": \"base.supplier.enable\", \"kind\": \"ACTION\", \"label\": \"enable\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"base.supplier\"}, {\"code\": \"base.supplier.delete\", \"kind\": \"ACTION\", \"label\": \"delete\", \"roles\": [\"ADMIN\"], \"parent\": \"base.supplier\"}, {\"code\": \"base.product.image\", \"kind\": \"ACTION\", \"label\": \"商品图片上传移除\", \"roles\": [\"ADMIN\"], \"parent\": \"base.product\"}, {\"code\": \"product.purchase_lookup\", \"kind\": \"ACTION\", \"label\": \"采购选品及参考进价\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": null}, {\"code\": \"product.sale_lookup\", \"kind\": \"ACTION\", \"label\": \"销售选品\", \"roles\": [\"ADMIN\", \"CASHIER\"], \"parent\": null}, {\"code\": \"purchase\", \"kind\": \"MODULE\", \"label\": \"进货管理\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": null}, {\"code\": \"purchase.order\", \"kind\": \"PAGE\", \"label\": \"采购单\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"purchase\"}, {\"code\": \"purchase.order.create\", \"kind\": \"ACTION\", \"label\": \"create\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"purchase.order\"}, {\"code\": \"purchase.order.edit\", \"kind\": \"ACTION\", \"label\": \"edit\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"purchase.order\"}, {\"code\": \"purchase.order.delete_draft\", \"kind\": \"ACTION\", \"label\": \"delete_draft\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"purchase.order\"}, {\"code\": \"purchase.order.submit\", \"kind\": \"ACTION\", \"label\": \"submit\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"purchase.order\"}, {\"code\": \"purchase.order.withdraw\", \"kind\": \"ACTION\", \"label\": \"withdraw\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"purchase.order\"}, {\"code\": \"purchase.order.close\", \"kind\": \"ACTION\", \"label\": \"close\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"purchase.order\"}, {\"code\": \"purchase.order.reconcile\", \"kind\": \"ACTION\", \"label\": \"reconcile\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"purchase.order\"}, {\"code\": \"purchase.order.review\", \"kind\": \"ACTION\", \"label\": \"review\", \"roles\": [\"ADMIN\"], \"parent\": \"purchase.order\"}, {\"code\": \"purchase.order.reject\", \"kind\": \"ACTION\", \"label\": \"reject\", \"roles\": [\"ADMIN\"], \"parent\": \"purchase.order\"}, {\"code\": \"purchase.order.reverse\", \"kind\": \"ACTION\", \"label\": \"reverse\", \"roles\": [\"ADMIN\"], \"parent\": \"purchase.order\"}, {\"code\": \"sale\", \"kind\": \"MODULE\", \"label\": \"销售管理\", \"roles\": [\"ADMIN\", \"CASHIER\"], \"parent\": null}, {\"code\": \"sale.order\", \"kind\": \"PAGE\", \"label\": \"销售开单和记录\", \"roles\": [\"ADMIN\", \"CASHIER\"], \"parent\": \"sale\"}, {\"code\": \"sale.order.complete\", \"kind\": \"ACTION\", \"label\": \"确认收款销售\", \"roles\": [\"ADMIN\", \"CASHIER\"], \"parent\": \"sale.order\"}, {\"code\": \"sale.order.correct\", \"kind\": \"ACTION\", \"label\": \"整单纠错\", \"roles\": [\"ADMIN\"], \"parent\": \"sale.order\"}, {\"code\": \"inventory\", \"kind\": \"MODULE\", \"label\": \"库存管理\", \"roles\": [\"ADMIN\", \"PURCHASER\", \"CASHIER\"], \"parent\": null}, {\"code\": \"inventory.query\", \"kind\": \"PAGE\", \"label\": \"库存只读查询\", \"roles\": [\"ADMIN\", \"PURCHASER\", \"CASHIER\"], \"parent\": \"inventory\"}, {\"code\": \"inventory.batch\", \"kind\": \"PAGE\", \"label\": \"批次库存\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"inventory\"}, {\"code\": \"inventory.movement\", \"kind\": \"PAGE\", \"label\": \"库存流水\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"inventory\"}, {\"code\": \"inventory.warning\", \"kind\": \"PAGE\", \"label\": \"库存预警\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"inventory\"}, {\"code\": \"inventory.stocktake\", \"kind\": \"PAGE\", \"label\": \"库存盘点\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"inventory\"}, {\"code\": \"inventory.stocktake.start\", \"kind\": \"ACTION\", \"label\": \"start\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"inventory.stocktake\"}, {\"code\": \"inventory.stocktake.save\", \"kind\": \"ACTION\", \"label\": \"save\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"inventory.stocktake\"}, {\"code\": \"inventory.stocktake.submit\", \"kind\": \"ACTION\", \"label\": \"submit\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"inventory.stocktake\"}, {\"code\": \"inventory.stocktake.withdraw\", \"kind\": \"ACTION\", \"label\": \"withdraw\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"inventory.stocktake\"}, {\"code\": \"inventory.stocktake.close\", \"kind\": \"ACTION\", \"label\": \"close\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"inventory.stocktake\"}, {\"code\": \"inventory.stocktake.review\", \"kind\": \"ACTION\", \"label\": \"review\", \"roles\": [\"ADMIN\"], \"parent\": \"inventory.stocktake\"}, {\"code\": \"inventory.stocktake.reject\", \"kind\": \"ACTION\", \"label\": \"reject\", \"roles\": [\"ADMIN\"], \"parent\": \"inventory.stocktake\"}, {\"code\": \"statistics\", \"kind\": \"MODULE\", \"label\": \"统计分析\", \"roles\": [\"ADMIN\", \"PURCHASER\", \"CASHIER\"], \"parent\": null}, {\"code\": \"statistics.sales\", \"kind\": \"PAGE\", \"label\": \"销售图表与热销滞销\", \"roles\": [\"ADMIN\", \"CASHIER\"], \"parent\": \"statistics\"}, {\"code\": \"statistics.inventory\", \"kind\": \"PAGE\", \"label\": \"库存统计\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"statistics\"}, {\"code\": \"ai\", \"kind\": \"MODULE\", \"label\": \"智能经营建议\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": null}, {\"code\": \"ai.inventory\", \"kind\": \"PAGE\", \"label\": \"库存优化建议与历史\", \"roles\": [\"ADMIN\", \"PURCHASER\"], \"parent\": \"ai\"}, {\"code\": \"ai.sales\", \"kind\": \"PAGE\", \"label\": \"销售经营建议与历史\", \"roles\": [\"ADMIN\"], \"parent\": \"ai\"}]', '菜单按钮权限目录，代码修改时同步维护', 0, 1, '2026-11-10 16:13:25.806904');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `code` varchar(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'ADMIN/PURCHASER/CASHIER，固定三角色',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `permissions` json NOT NULL COMMENT '当前权限代码数组，保存时必须是 ceiling_permissions 子集',
  `default_permissions` json NOT NULL COMMENT '恢复默认使用的权限数组',
  `ceiling_permissions` json NOT NULL COMMENT '岗位允许的最大权限集合，应用不可通过普通授权接口修改',
  `data_scope` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ALL 全部业务 / OWN 本人业务；共享库存及聚合统计另按权限控制',
  `auth_version` bigint NOT NULL DEFAULT 0 COMMENT '权限变化递增，登录凭证携带并校验角色版本',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`code`) USING BTREE,
  CONSTRAINT `ck_role_code` CHECK (`code` in (_utf8mb4'ADMIN',_utf8mb4'PURCHASER',_utf8mb4'CASHIER')),
  CONSTRAINT `ck_role_json` CHECK ((json_type(`permissions`) = _utf8mb4'ARRAY') and (json_type(`default_permissions`) = _utf8mb4'ARRAY') and (json_type(`ceiling_permissions`) = _utf8mb4'ARRAY')),
  CONSTRAINT `ck_role_scope` CHECK (((`code` = _utf8mb4'ADMIN') and (`data_scope` = _utf8mb4'ALL')) or ((`code` <> _utf8mb4'ADMIN') and (`data_scope` = _utf8mb4'OWN'))),
  CONSTRAINT `ck_role_subset` CHECK ((json_contains(`ceiling_permissions`,`permissions`) = 1) and (json_contains(`ceiling_permissions`,`default_permissions`) = 1)),
  CONSTRAINT `ck_role_version` CHECK (`auth_version` >= 0)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '固定角色及菜单按钮授权，权限目录见 sys_config' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES ('ADMIN', '超市管理员', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"system\", \"system.user\", \"system.role\", \"system.config\", \"system.log\", \"system.user.create\", \"system.user.edit\", \"system.user.enable\", \"system.user.reset_password\", \"system.user.delete\", \"system.role.save\", \"system.role.restore\", \"system.config.save\", \"base\", \"base.category\", \"base.category.create\", \"base.category.edit\", \"base.category.enable\", \"base.category.delete\", \"base.product\", \"base.product.create\", \"base.product.edit\", \"base.product.enable\", \"base.product.delete\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"base.supplier.delete\", \"base.product.image\", \"product.purchase_lookup\", \"product.sale_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"purchase.order.review\", \"purchase.order.reject\", \"purchase.order.reverse\", \"sale\", \"sale.order\", \"sale.order.complete\", \"sale.order.correct\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"inventory.stocktake.review\", \"inventory.stocktake.reject\", \"statistics\", \"statistics.sales\", \"statistics.inventory\", \"ai\", \"ai.inventory\", \"ai.sales\"]', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"system\", \"system.user\", \"system.role\", \"system.config\", \"system.log\", \"system.user.create\", \"system.user.edit\", \"system.user.enable\", \"system.user.reset_password\", \"system.user.delete\", \"system.role.save\", \"system.role.restore\", \"system.config.save\", \"base\", \"base.category\", \"base.category.create\", \"base.category.edit\", \"base.category.enable\", \"base.category.delete\", \"base.product\", \"base.product.create\", \"base.product.edit\", \"base.product.enable\", \"base.product.delete\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"base.supplier.delete\", \"base.product.image\", \"product.purchase_lookup\", \"product.sale_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"purchase.order.review\", \"purchase.order.reject\", \"purchase.order.reverse\", \"sale\", \"sale.order\", \"sale.order.complete\", \"sale.order.correct\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"inventory.stocktake.review\", \"inventory.stocktake.reject\", \"statistics\", \"statistics.sales\", \"statistics.inventory\", \"ai\", \"ai.inventory\", \"ai.sales\"]', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"system\", \"system.user\", \"system.role\", \"system.config\", \"system.log\", \"system.user.create\", \"system.user.edit\", \"system.user.enable\", \"system.user.reset_password\", \"system.user.delete\", \"system.role.save\", \"system.role.restore\", \"system.config.save\", \"base\", \"base.category\", \"base.category.create\", \"base.category.edit\", \"base.category.enable\", \"base.category.delete\", \"base.product\", \"base.product.create\", \"base.product.edit\", \"base.product.enable\", \"base.product.delete\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"base.supplier.delete\", \"base.product.image\", \"product.purchase_lookup\", \"product.sale_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"purchase.order.review\", \"purchase.order.reject\", \"purchase.order.reverse\", \"sale\", \"sale.order\", \"sale.order.complete\", \"sale.order.correct\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"inventory.stocktake.review\", \"inventory.stocktake.reject\", \"statistics\", \"statistics.sales\", \"statistics.inventory\", \"ai\", \"ai.inventory\", \"ai.sales\"]', 'ALL', 0, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `sys_role` VALUES ('CASHIER', '前台收银操作员', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"product.sale_lookup\", \"sale\", \"sale.order\", \"sale.order.complete\", \"inventory\", \"inventory.query\", \"statistics\", \"statistics.sales\"]', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"product.sale_lookup\", \"sale\", \"sale.order\", \"sale.order.complete\", \"inventory\", \"inventory.query\", \"statistics\", \"statistics.sales\"]', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"product.sale_lookup\", \"sale\", \"sale.order\", \"sale.order.complete\", \"inventory\", \"inventory.query\", \"statistics\", \"statistics.sales\"]', 'OWN', 0, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `sys_role` VALUES ('PURCHASER', '采购入库操作员', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"base\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"product.purchase_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"statistics\", \"statistics.inventory\", \"ai\", \"ai.inventory\"]', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"base\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"product.purchase_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"statistics\", \"statistics.inventory\", \"ai\", \"ai.inventory\"]', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"base\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"product.purchase_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"statistics\", \"statistics.inventory\", \"ai\", \"ai.inventory\"]', 'OWN', 0, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(20) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `password_hash` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'BCrypt $2b$ 或 $2a$ 格式，禁止明文/MD5',
  `real_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `phone` varchar(11) CHARACTER SET ascii COLLATE ascii_general_ci NULL DEFAULT NULL,
  `avatar_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '测试图片.jpg' COMMENT '相对路径，NULL 表示使用默认头像',
  `avatar_meta` json NULL COMMENT '上传大小、MIME、宽高、上传时间等；初始样例仅标记 fixture',
  `role_code` varchar(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `credential_version` bigint NOT NULL DEFAULT 0 COMMENT '密码变更、停用、角色变更、退出递增；无需会话表即可使旧凭证失效',
  `last_login_at` datetime(6) NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_username`(`username` ASC) USING BTREE,
  INDEX `ix_user_role_status`(`role_code` ASC, `enabled` ASC) USING BTREE,
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_code`) REFERENCES `sys_role` (`code`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_user_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `ck_user_name` CHECK (char_length(`real_name`) between 2 and 20),
  CONSTRAINT `ck_user_password` CHECK ((char_length(`password_hash`) = 60) and (left(`password_hash`,4) in (_utf8mb4'$2b$',_utf8mb4'$2a$',_utf8mb4'$2y$'))),
  CONSTRAINT `ck_user_phone` CHECK ((`phone` is null) or ((char_length(`phone`) = 11) and (left(`phone`,1) = _utf8mb4'1'))),
  CONSTRAINT `ck_user_username` CHECK (char_length(`username`) between 4 and 20),
  CONSTRAINT `ck_user_version` CHECK (`credential_version` >= 0)
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与本人资料；最后管理员保护及账号状态变更在服务事务内实现' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '系统管理员', '13800000001', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'ADMIN', 1, 0, NULL, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `sys_user` VALUES (2, 'admin2', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '备用管理员', '13800000002', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'ADMIN', 1, 0, NULL, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `sys_user` VALUES (3, 'buyer01', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '采购张三', '13800000003', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'PURCHASER', 1, 0, NULL, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `sys_user` VALUES (4, 'buyer02', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '采购李四', '13800000004', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'PURCHASER', 1, 0, NULL, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `sys_user` VALUES (5, 'cashier01', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '收银王五', '13800000005', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'CASHIER', 1, 0, NULL, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `sys_user` VALUES (6, 'cashier02', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '收银赵六', '13800000006', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'CASHIER', 1, 0, NULL, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `sys_user` VALUES (7, 'disabled01', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '停用收银', '13800000007', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'CASHIER', 0, 0, NULL, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `sys_user` VALUES (8, 'unused01', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '待删除用户', '13800000008', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'CASHIER', 1, 0, NULL, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');

-- Views are ordered by dependency for first-time imports.
-- ----------------------------
-- View structure for v_batch_inventory
-- ----------------------------
DROP VIEW IF EXISTS `v_batch_inventory`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY INVOKER VIEW `v_batch_inventory` AS select `i`.`id` AS `batch_id`,`i`.`batch_no` AS `batch_no`,`i`.`product_id` AS `product_id`,`i`.`purchase_id` AS `purchase_id`,`o`.`order_no` AS `purchase_no`,`o`.`supplier_id` AS `supplier_id`,`o`.`operator_id` AS `purchase_operator_id`,`o`.`stocked_at` AS `stocked_at`,`i`.`supplier_batch_no` AS `supplier_batch_no`,`i`.`production_date` AS `production_date`,`i`.`expiry_date` AS `expiry_date`,`i`.`unit_cost` AS `unit_cost`,`i`.`received_qty` AS `received_qty`,`i`.`stock_qty` AS `stock_qty`,(case when (`i`.`manages_expiry` = 1) then (to_days(`i`.`expiry_date`) - to_days(curdate())) else NULL end) AS `remaining_days`,(case when ((`i`.`manages_expiry` = 1) and (`i`.`expiry_date` < curdate())) then 1 else 0 end) AS `is_expired`,(case when ((`i`.`manages_expiry` = 1) and (`i`.`stock_qty` > 0) and (`i`.`expiry_date` between curdate() and (curdate() + interval `p`.`near_expiry_days` day))) then 1 else 0 end) AS `is_near_expiry` from ((`purchase_item` `i` join `purchase_order` `o` on((`o`.`id` = `i`.`purchase_id`))) join `product` `p` on((`p`.`id` = `i`.`product_id`))) where ((`o`.`status` = 'IN_STOCK') and (`i`.`batch_no` is not null));

-- ----------------------------
-- View structure for v_product_inventory
-- ----------------------------
DROP VIEW IF EXISTS `v_product_inventory`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY INVOKER VIEW `v_product_inventory` AS select `p`.`id` AS `id`,`p`.`code` AS `code`,`p`.`barcode` AS `barcode`,`p`.`name` AS `name`,`p`.`category_id` AS `category_id`,`c`.`name` AS `category_name`,`p`.`specification` AS `specification`,`p`.`unit` AS `unit`,`p`.`sale_price` AS `sale_price`,`p`.`image_path` AS `image_path`,`p`.`enabled` AS `enabled`,`p`.`can_sell` AS `can_sell`,`p`.`can_purchase` AS `can_purchase`,`p`.`stock_qty` AS `book_qty`,coalesce(`b`.`batch_qty`,0) AS `batch_qty`,(case when ((`p`.`enabled` = 1) and (`p`.`can_sell` = 1)) then coalesce(`b`.`unexpired_qty`,0) else 0 end) AS `saleable_qty`,coalesce(`b`.`expired_qty`,0) AS `expired_qty`,coalesce(`b`.`near_batch_count`,0) AS `near_batch_count`,coalesce(`b`.`expired_batch_count`,0) AS `expired_batch_count`,`p`.`stock_lower_limit` AS `stock_lower_limit`,`p`.`frozen_stocktake_id` AS `frozen_stocktake_id`,(case when (`p`.`frozen_stocktake_id` is null) then 0 else 1 end) AS `is_frozen`,`b`.`last_stocked_at` AS `last_stocked_at`,`b`.`first_stocked_at` AS `first_stocked_at` from ((`product` `p` join `category` `c` on((`c`.`id` = `p`.`category_id`))) left join (select `v_batch_inventory`.`product_id` AS `product_id`,sum(`v_batch_inventory`.`stock_qty`) AS `batch_qty`,sum((case when (`v_batch_inventory`.`is_expired` = 0) then `v_batch_inventory`.`stock_qty` else 0 end)) AS `unexpired_qty`,sum((case when (`v_batch_inventory`.`is_expired` = 1) then `v_batch_inventory`.`stock_qty` else 0 end)) AS `expired_qty`,sum(`v_batch_inventory`.`is_near_expiry`) AS `near_batch_count`,sum((case when ((`v_batch_inventory`.`is_expired` = 1) and (`v_batch_inventory`.`stock_qty` > 0)) then 1 else 0 end)) AS `expired_batch_count`,max(`v_batch_inventory`.`stocked_at`) AS `last_stocked_at`,min(`v_batch_inventory`.`stocked_at`) AS `first_stocked_at` from `v_batch_inventory` group by `v_batch_inventory`.`product_id`) `b` on((`b`.`product_id` = `p`.`id`)));

-- ----------------------------
-- View structure for v_cashier_inventory
-- ----------------------------
DROP VIEW IF EXISTS `v_cashier_inventory`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY INVOKER VIEW `v_cashier_inventory` AS select `v_product_inventory`.`id` AS `id`,`v_product_inventory`.`code` AS `code`,`v_product_inventory`.`barcode` AS `barcode`,`v_product_inventory`.`name` AS `name`,`v_product_inventory`.`category_id` AS `category_id`,`v_product_inventory`.`category_name` AS `category_name`,`v_product_inventory`.`specification` AS `specification`,`v_product_inventory`.`unit` AS `unit`,`v_product_inventory`.`sale_price` AS `sale_price`,`v_product_inventory`.`image_path` AS `image_path`,`v_product_inventory`.`saleable_qty` AS `saleable_qty`,`v_product_inventory`.`is_frozen` AS `is_frozen` from `v_product_inventory`;

-- ----------------------------
-- View structure for v_category_inventory
-- ----------------------------
DROP VIEW IF EXISTS `v_category_inventory`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY INVOKER VIEW `v_category_inventory` AS select `c`.`id` AS `category_id`,`c`.`name` AS `category_name`,coalesce(sum(`p`.`stock_qty`),0) AS `book_qty` from (`category` `c` left join `product` `p` on((`p`.`category_id` = `c`.`id`))) group by `c`.`id`,`c`.`name`;

-- ----------------------------
-- View structure for v_low_stock
-- ----------------------------
DROP VIEW IF EXISTS `v_low_stock`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY INVOKER VIEW `v_low_stock` AS select `v_product_inventory`.`id` AS `id`,`v_product_inventory`.`code` AS `code`,`v_product_inventory`.`barcode` AS `barcode`,`v_product_inventory`.`name` AS `name`,`v_product_inventory`.`category_id` AS `category_id`,`v_product_inventory`.`category_name` AS `category_name`,`v_product_inventory`.`specification` AS `specification`,`v_product_inventory`.`unit` AS `unit`,`v_product_inventory`.`sale_price` AS `sale_price`,`v_product_inventory`.`image_path` AS `image_path`,`v_product_inventory`.`enabled` AS `enabled`,`v_product_inventory`.`can_sell` AS `can_sell`,`v_product_inventory`.`can_purchase` AS `can_purchase`,`v_product_inventory`.`book_qty` AS `book_qty`,`v_product_inventory`.`batch_qty` AS `batch_qty`,`v_product_inventory`.`saleable_qty` AS `saleable_qty`,`v_product_inventory`.`expired_qty` AS `expired_qty`,`v_product_inventory`.`near_batch_count` AS `near_batch_count`,`v_product_inventory`.`expired_batch_count` AS `expired_batch_count`,`v_product_inventory`.`stock_lower_limit` AS `stock_lower_limit`,`v_product_inventory`.`frozen_stocktake_id` AS `frozen_stocktake_id`,`v_product_inventory`.`is_frozen` AS `is_frozen`,`v_product_inventory`.`last_stocked_at` AS `last_stocked_at`,`v_product_inventory`.`first_stocked_at` AS `first_stocked_at` from `v_product_inventory` where ((`v_product_inventory`.`enabled` = 1) and (`v_product_inventory`.`can_sell` = 1) and (`v_product_inventory`.`saleable_qty` <= `v_product_inventory`.`stock_lower_limit`));

-- ----------------------------
-- View structure for v_purchase_event
-- ----------------------------
DROP VIEW IF EXISTS `v_purchase_event`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY INVOKER VIEW `v_purchase_event` AS select `o`.`id` AS `purchase_id`,`o`.`order_no` AS `order_no`,`o`.`supplier_id` AS `supplier_id`,`o`.`supplier_name` AS `supplier_name`,`o`.`operator_id` AS `operator_id`,`i`.`id` AS `purchase_item_id`,`i`.`product_id` AS `product_id`,'PURCHASE' AS `event_type`,`o`.`stocked_at` AS `event_at`,`i`.`received_qty` AS `signed_qty`,`i`.`line_amount` AS `signed_amount` from (`purchase_order` `o` join `purchase_item` `i` on((`i`.`purchase_id` = `o`.`id`))) where (`o`.`status` in ('IN_STOCK','REVERSED')) union all select `o`.`id` AS `id`,`o`.`order_no` AS `order_no`,`o`.`supplier_id` AS `supplier_id`,`o`.`supplier_name` AS `supplier_name`,`o`.`operator_id` AS `operator_id`,`i`.`id` AS `id`,`i`.`product_id` AS `product_id`,'REVERSAL' AS `REVERSAL`,`o`.`reversed_at` AS `reversed_at`,-(`i`.`received_qty`) AS `-i.received_qty`,-(`i`.`line_amount`) AS `-i.line_amount` from (`purchase_order` `o` join `purchase_item` `i` on((`i`.`purchase_id` = `o`.`id`))) where (`o`.`status` = 'REVERSED');

-- ----------------------------
-- View structure for v_reconcile_history
-- ----------------------------
DROP VIEW IF EXISTS `v_reconcile_history`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY INVOKER VIEW `v_reconcile_history` AS select `operation_log`.`id` AS `id`,`operation_log`.`actor_id` AS `actor_id`,`operation_log`.`actor_name` AS `actor_name`,`operation_log`.`target_id` AS `purchase_id`,`operation_log`.`target_no` AS `order_no`,`operation_log`.`action` AS `action`,`operation_log`.`reason` AS `reason`,`operation_log`.`before_data` AS `before_data`,`operation_log`.`after_data` AS `after_data`,`operation_log`.`occurred_at` AS `occurred_at` from `operation_log` where ((`operation_log`.`target_type` = 'PURCHASE') and (`operation_log`.`action` in ('RECONCILE_DIFFERENCE','RECONCILE_MATCH','RECONCILE_WITHDRAW')));

-- ----------------------------
-- View structure for v_sale_event
-- ----------------------------
DROP VIEW IF EXISTS `v_sale_event`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY INVOKER VIEW `v_sale_event` AS select `s`.`id` AS `sale_id`,`s`.`order_no` AS `order_no`,`s`.`cashier_id` AS `cashier_id`,`s`.`cashier_name` AS `cashier_name`,`i`.`id` AS `sale_item_id`,`i`.`product_id` AS `product_id`,`i`.`category_id` AS `category_id`,`i`.`category_name` AS `category_name`,`i`.`product_code` AS `product_code`,`i`.`product_name` AS `product_name`,'SALE' AS `event_type`,`s`.`completed_at` AS `event_at`,`i`.`quantity` AS `signed_qty`,`i`.`line_amount` AS `signed_amount` from (`sale_order` `s` join `sale_item` `i` on((`i`.`sale_id` = `s`.`id`))) union all select `s`.`id` AS `id`,`s`.`order_no` AS `order_no`,`s`.`cashier_id` AS `cashier_id`,`s`.`cashier_name` AS `cashier_name`,`i`.`id` AS `id`,`i`.`product_id` AS `product_id`,`i`.`category_id` AS `category_id`,`i`.`category_name` AS `category_name`,`i`.`product_code` AS `product_code`,`i`.`product_name` AS `product_name`,'CORRECTION' AS `CORRECTION`,`s`.`corrected_at` AS `corrected_at`,-(`i`.`quantity`) AS `-i.quantity`,-(`i`.`line_amount`) AS `-i.line_amount` from (`sale_order` `s` join `sale_item` `i` on((`i`.`sale_id` = `s`.`id`))) where (`s`.`status` = 'CORRECTED');

SET FOREIGN_KEY_CHECKS = 1;
