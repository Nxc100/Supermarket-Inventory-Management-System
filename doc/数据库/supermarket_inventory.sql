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

 Date: 11/11/2026 17:04:31
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
  `prompt_tokens` bigint NULL DEFAULT NULL,
  `completion_tokens` bigint NULL DEFAULT NULL,
  `total_tokens` bigint NULL DEFAULT NULL,
  `cached_tokens` bigint NULL DEFAULT NULL,
  `provider_request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_request`(`request_key` ASC) USING BTREE,
  INDEX `ix_ai_type_status`(`suggestion_type` ASC, `status` ASC, `requested_at` ASC) USING BTREE,
  INDEX `ix_ai_owner`(`operator_id` ASC, `requested_at` ASC) USING BTREE,
  INDEX `ix_ai_usage`(`is_fixture` ASC, `requested_at` ASC, `model_name` ASC) USING BTREE,
  CONSTRAINT `fk_ai_user` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_ai_fixture` CHECK ((`is_fixture` in (0,1)) and ((`is_fixture` = 0) or (`provider` = _utf8mb4'FIXTURE'))),
  CONSTRAINT `ck_ai_result` CHECK (((`status` = _utf8mb4'PENDING') and (`content` is null) and (`finished_at` is null)) or ((`status` = _utf8mb4'SUCCESS') and (`content` is not null) and (char_length(trim(`content`)) > 0) and (`finished_at` is not null) and (`error_code` is null)) or ((`status` = _utf8mb4'FAILURE') and (`content` is null) and (`error_code` is not null) and (`error_message` is not null) and (`finished_at` is not null))),
  CONSTRAINT `ck_ai_status` CHECK (`status` in (_utf8mb4'PENDING',_utf8mb4'SUCCESS',_utf8mb4'FAILURE')),
  CONSTRAINT `ck_ai_time` CHECK ((`period_end` >= `period_start`) and ((`duration_ms` is null) or (`duration_ms` >= 0))),
  CONSTRAINT `ck_ai_type` CHECK (`suggestion_type` in (_utf8mb4'INVENTORY',_utf8mb4'SALES')),
  CONSTRAINT `ck_ai_usage` CHECK (((`prompt_tokens` is null) and (`completion_tokens` is null) and (`total_tokens` is null) and (`cached_tokens` is null)) or ((`prompt_tokens` is not null) and (`completion_tokens` is not null) and (`total_tokens` is not null) and (`prompt_tokens` >= 0) and (`completion_tokens` >= 0) and (`total_tokens` = (`prompt_tokens` + `completion_tokens`)) and ((`cached_tokens` is null) or ((`cached_tokens` >= 0) and (`cached_tokens` <= `prompt_tokens`)))))
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模型输出与失败记录；不含密钥，测试数据不伪装真实模型调用' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_suggestion
-- ----------------------------
INSERT INTO `ai_suggestion` VALUES (1, 'seed-ai-1', 'INVENTORY', 3, '采购张三', '2026-10-12', '2026-11-10', '{\"productIds\": [1]}', '{\"bookQty\": 5, \"fixture\": true, \"productId\": 1, \"capturedAt\": \"2026-11-10 16:13:25.806904\", \"lowerLimit\": 5, \"saleableQty\": 5, \"periodNetQty\": 14, \"periodNetAmount\": 70.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'SUCCESS', '【初始测试数据，非真实模型调用】库存5瓶、下限5瓶，近期净销量14瓶；请人工核实补货数量。', NULL, NULL, 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `ai_suggestion` VALUES (2, 'seed-ai-2', 'SALES', 1, '系统管理员', '2026-10-12', '2026-11-10', '{}', '{\"netQty\": 18, \"fixture\": true, \"netAmount\": 97.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'SUCCESS', '【初始测试数据，非真实模型调用】请结合输入摘要查看销售与商品排行，并人工核查滞销原因。', NULL, NULL, 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `ai_suggestion` VALUES (3, 'seed-ai-3', 'INVENTORY', 3, '采购张三', '2026-10-12', '2026-11-10', '{\"productIds\": [1]}', '{\"bookQty\": 5, \"fixture\": true, \"productId\": 1, \"capturedAt\": \"2026-11-10 16:13:25.806904\", \"lowerLimit\": 5, \"saleableQty\": 5, \"periodNetQty\": 14, \"periodNetAmount\": 70.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'FAILURE', NULL, 'TIMEOUT', '测试降级记录：TIMEOUT', 15000, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `ai_suggestion` VALUES (4, 'seed-ai-4', 'SALES', 1, '系统管理员', '2026-10-12', '2026-11-10', '{}', '{\"netQty\": 18, \"fixture\": true, \"netAmount\": 97.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'FAILURE', NULL, 'NOT_CONFIGURED', '测试降级记录：NOT_CONFIGURED', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `ai_suggestion` VALUES (5, 'seed-ai-5', 'INVENTORY', 4, '采购李四', '2026-10-12', '2026-11-10', '{\"productIds\": [1]}', '{\"bookQty\": 5, \"fixture\": true, \"productId\": 1, \"capturedAt\": \"2026-11-10 16:13:25.806904\", \"lowerLimit\": 5, \"saleableQty\": 5, \"periodNetQty\": 14, \"periodNetAmount\": 70.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'FAILURE', NULL, 'RATE_LIMITED', '测试降级记录：RATE_LIMITED', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `ai_suggestion` VALUES (6, 'seed-ai-6', 'INVENTORY', 3, '采购张三', '2026-10-12', '2026-11-10', '{\"productIds\": [1]}', '{\"bookQty\": 5, \"fixture\": true, \"productId\": 1, \"capturedAt\": \"2026-11-10 16:13:25.806904\", \"lowerLimit\": 5, \"saleableQty\": 5, \"periodNetQty\": 14, \"periodNetAmount\": 70.00}', '2026-11-10 16:13:25.806904', 'FIXTURE', 'fixture-not-api', 1, 'FAILURE', NULL, 'EMPTY_RESULT', '测试降级记录：EMPTY_RESULT', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `ai_suggestion` VALUES (7, '9018d931-dc63-43fb-89c9-70bbec35cefa', 'INVENTORY', 3, '采购张三', '2026-10-13', '2026-11-11', '{\"productId\": \"25\"}', '{\"type\": \"INVENTORY\", \"coverage\": {\"selection\": \"最多50个商品，优先过期量、临期量，再按账面库存及商品ID；总计覆盖全部筛选范围。\", \"truncated\": false, \"includedProducts\": 1, \"matchingProducts\": 1}, \"products\": [{\"code\": \"HX_DRINK_500\", \"name\": \"禾序瓶装饮料500mL\", \"unit\": \"瓶\", \"bookQty\": 5, \"nearQty\": 0, \"productId\": \"25\", \"expiredQty\": 0, \"saleableQty\": 5, \"categoryName\": \"饮料乳品\", \"recentNetQty\": 14, \"lastStockedAt\": \"2026-11-11T12:27:58.169611+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}], \"periodEnd\": \"2026-11-11\", \"periodStart\": \"2026-10-13\", \"businessDate\": \"2026-11-11\", \"periodNetQty\": 14, \"salesHistory\": \"所选区间有销售事件\", \"interpretation\": \"数量按各商品基本计数单位；仅为生成时快照，所有建议须人工核实，不能自动执行。\", \"currentInventory\": {\"bookQty\": 5, \"expiredQty\": 0, \"saleableQty\": 5, \"nearBatchCount\": 0, \"lowProductCount\": 1, \"expiredBatchCount\": 0}}', '2026-11-11 12:33:22.678102', 'DASHSCOPE', 'qwen-flash', 0, 'SUCCESS', '### 数据现象  \n当前库存账面数量为**5瓶**，可销售数量也为**5瓶**，无临期或过期商品。在2026年10月13日至11月11日的统计周期内，该商品净销售量为**14瓶**，表明实际出库远超账面库存，存在账实不符风险。尽管当前无临期或过期批次，但库存水平已触及最低安全下限（stockLowerLimit=5），需警惕补货延迟导致断货。\n\n### 风险优先级  \n高：**账实差异严重**，净销量达14瓶而账面仅5瓶，可能因未及时记账或系统延迟录入；中：库存已达安全下限，若无补货将面临缺货；低：暂无临期或过期风险，但若后续补货不及时，可能引发重复缺货。\n\n### 人工核查与行动建议  \n1. 核查近期内是否发生未录入的采购或调拨记录，确认是否存在**账外库存**或系统延迟问题。  \n2. 立即安排补货，目标库存至少提升至**15瓶**以上，以应对未来7天正常需求波动。  \n3. 检查该商品最近一次进货时间（2026-11-11）是否合理，若补货间隔过长，应调整采购频率。  \n4. 设置库存预警规则，当账面库存≤8瓶时触发提醒，避免再次逼近安全下限。', NULL, NULL, 2957, '2026-11-11 12:33:42.206270', '2026-11-11 12:33:45.194002', 476, 337, 813, 0, 'chatcmpl-6033c75f-1120-9820-8cf9-5ae9014cfc2c');
INSERT INTO `ai_suggestion` VALUES (8, 'a332968a-7151-4941-84b8-408f9979db0b', 'INVENTORY', 1, '系统管理员', '2026-10-13', '2026-11-11', '{\"productId\": \"25\"}', '{\"type\": \"INVENTORY\", \"coverage\": {\"selection\": \"最多50个商品，优先过期量、临期量，再按账面库存及商品ID；总计覆盖全部筛选范围。\", \"truncated\": false, \"includedProducts\": 1, \"matchingProducts\": 1}, \"products\": [{\"code\": \"HX_DRINK_500\", \"name\": \"禾序瓶装饮料500mL\", \"unit\": \"瓶\", \"bookQty\": 5, \"nearQty\": 0, \"productId\": \"25\", \"expiredQty\": 0, \"saleableQty\": 5, \"categoryName\": \"饮料乳品\", \"recentNetQty\": 14, \"lastStockedAt\": \"2026-11-11T12:27:58.169611+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}], \"periodEnd\": \"2026-11-11\", \"periodStart\": \"2026-10-13\", \"businessDate\": \"2026-11-11\", \"periodNetQty\": 14, \"salesHistory\": \"所选区间有销售事件\", \"interpretation\": \"数量按各商品基本计数单位。当前库存是快照余额，期间净销量是历史流量，销量大于当前库存不表示账实不符；摘要未提供完整库存收支或实物清点，不能据此对账。补货量须结合真实交期、需求另行核定；所有建议须人工核实，不能自动执行。\", \"currentInventory\": {\"bookQty\": 5, \"expiredQty\": 0, \"saleableQty\": 5, \"nearBatchCount\": 0, \"lowProductCount\": 1, \"expiredBatchCount\": 0}}', '2026-11-11 12:38:50.131128', 'DASHSCOPE', 'qwen-flash', 0, 'SUCCESS', '### 数据现象\n\n当前账面库存为**5瓶**，可售库存也为**5瓶**，无过期商品。在2026年10月13日至11月11日的统计周期内，该商品期间净销量达**14瓶**，远超当前库存数量。尽管历史销量超过账面余额，但因未提供期初库存、全部入库记录及实物盘点结果，无法判断是否存在账实不符或系统延迟问题。目前仅显示该商品处于“低库存”状态（账面库存等于最低限），且无临期或过期批次。\n\n### 风险优先级\n\n- **高风险：缺货风险**——当前库存仅为5瓶，而近一个月销量高达14瓶，若无及时补货，将面临断货可能。\n- **中风险：库存管理盲区**——缺乏期初数据与完整出入库记录，难以评估真实库存流转情况，存在潜在管理盲点。\n- **低风险：无临期/过期商品**——当前无临近或已过期批次，暂无处置压力。\n\n### 人工核查与行动建议\n\n1. 核查**最近一次入库时间**（2026-11-11 12:27）是否为实际补货，确认本次补货量是否满足需求。\n2. 手动比对**上期期初库存**与**全部入库记录**，验证“期间净销量14瓶”是否合理，排除漏记或重复计算。\n3. 若近期有采购计划，结合供应商交期，**尽快安排补货**，建议至少补货**15~20瓶**以覆盖后续需求波动。\n4. 建议设置动态预警阈值，当可售库存低于**8瓶**时触发补货提醒，避免再次陷入低库存状态。', NULL, NULL, 3257, '2026-11-11 12:38:50.259643', '2026-11-11 12:38:53.549554', 686, 403, 1089, 0, 'chatcmpl-427fdab6-36c4-9a6d-833a-98494013aa25');
INSERT INTO `ai_suggestion` VALUES (9, 'ecd590c8-1b6a-467d-a806-bd1dd6f6a7fc', 'INVENTORY', 1, '系统管理员', '2026-10-13', '2026-11-11', '{\"productId\": \"25\"}', '{\"type\": \"INVENTORY\", \"coverage\": {\"selection\": \"最多50个商品，优先过期量、临期量，再按账面库存及商品ID；总计覆盖全部筛选范围。\", \"truncated\": false, \"includedProducts\": 1, \"matchingProducts\": 1}, \"products\": [{\"code\": \"HX_DRINK_500\", \"name\": \"禾序瓶装饮料500mL\", \"unit\": \"瓶\", \"bookQty\": 15, \"nearQty\": 0, \"productId\": \"25\", \"expiredQty\": 0, \"saleableQty\": 15, \"categoryName\": \"饮料乳品\", \"recentNetQty\": 14, \"lastStockedAt\": \"2026-11-11T12:40:12.618016+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}], \"periodEnd\": \"2026-11-11\", \"periodStart\": \"2026-10-13\", \"businessDate\": \"2026-11-11\", \"periodNetQty\": 14, \"salesHistory\": \"所选区间有销售事件\", \"interpretation\": \"数量按各商品基本计数单位。当前库存是快照余额，期间净销量是历史流量，销量大于当前库存不表示账实不符；摘要未提供完整库存收支或实物清点，不能据此对账。补货量须结合真实交期、需求另行核定；所有建议须人工核实，不能自动执行。\", \"currentInventory\": {\"bookQty\": 15, \"expiredQty\": 0, \"saleableQty\": 15, \"nearBatchCount\": 0, \"lowProductCount\": 0, \"expiredBatchCount\": 0}}', '2026-11-11 12:41:38.550992', 'DASHSCOPE', 'qwen-flash', 0, 'FAILURE', NULL, 'AUTHENTICATION', '通义千问认证失败，请检查密钥、地域或模型授权', 53, '2026-11-11 12:41:38.611974', '2026-11-11 12:41:38.684145', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `ai_suggestion` VALUES (10, '201d2e68-0521-4a9c-bd19-d153aea1f49a', 'SALES', 1, '系统管理员', '2026-10-13', '2026-11-11', '{\"productId\": \"25\"}', '{\"type\": \"SALES\", \"periodEnd\": \"2026-11-11\", \"dailyTrend\": [{\"date\": \"2026-11-11\", \"netQty\": 14, \"netAmount\": \"70.00\"}], \"hotProducts\": [{\"code\": \"HX_DRINK_500\", \"name\": \"禾序瓶装饮料500mL\", \"unit\": \"瓶\", \"netQty\": 14, \"netAmount\": \"70.00\", \"productId\": \"25\", \"categoryId\": \"1\", \"categoryName\": \"饮料乳品\"}], \"periodStart\": \"2026-10-13\", \"salesTotals\": {\"netQty\": 14, \"netAmount\": \"70.00\", \"completedCount\": 2, \"correctedCount\": 0}, \"businessDate\": \"2026-11-11\", \"salesHistory\": \"所选区间有销售事件\", \"trendCoverage\": {\"note\": \"仅含最近最多366个事件日，区间总计仍覆盖完整筛选。\", \"eventDays\": 1, \"includedDays\": 1}, \"interpretation\": \"数量按各商品基本计数单位。当前库存是快照余额，期间净销量是历史流量，销量大于当前库存不表示账实不符；摘要未提供完整库存收支或实物清点，不能据此对账。补货量须结合真实交期、需求另行核定；所有建议须人工核实，不能自动执行。\", \"currentSlowWindow\": {\"end\": \"2026-11-11\", \"note\": \"当前有可售库存、窗口零净销量；首次有效入库不足30天单列观察不足，需结合缺货和到货人工判断。\", \"start\": \"2026-10-13\", \"products\": [{\"code\": \"HX_DRINK_500\", \"name\": \"禾序瓶装饮料500mL\", \"unit\": \"瓶\", \"netQty\": 14, \"productId\": \"25\", \"observation\": \"INSUFFICIENT_HISTORY\", \"saleableQty\": 15, \"firstStockedAt\": \"2026-11-11T12:27:58.169611+08:00\"}], \"matchingProducts\": 1}}', '2026-11-11 12:42:07.666479', 'DASHSCOPE', 'qwen-plus', 0, 'SUCCESS', '### 数据现象  \n本期（2026-10-13至2026-11-11）仅**1天有销售记录**（2026-11-11），当日净销量**14瓶**，总销售额70.00元，全部来自单一商品“禾序瓶装饮料500mL”。该商品当前可售库存为**15瓶**，首次入库时间为**2026-11-11 12:27**，即当天入库后即售出14瓶，历史销售窗口仅1天，观察期不足30天，系统标记为“INSUFFICIENT_HISTORY”。无其他商品产生销量，未体现滞销或缺货行为。\n\n### 风险优先级  \n**中高风险：数据代表性严重不足**。销售集中于单日、单品，无法反映常态动销；首次入库与首销同日，缺乏库存持有与周转观察基础；当前可售库存（15瓶）与单日销量（14瓶）接近，但因无前期库存、无多日趋势，**无法判断补货节奏是否合理或是否存在隐性缺货**。暂不构成紧急缺货风险，但经营决策缺乏依据。\n\n### 人工核查与行动建议  \n1. 核查2026-11-11实际入库单与销售小票，确认**15瓶是否全额到货、是否当日全数上架并完成扫码销售**，排除录入延迟或拆包损耗。  \n2. 调取该商品近30天（含入库前）的陈列照片、顾客咨询/询问记录或货架空置时段，辅助判断**真实动销意愿与潜在缺货感知**。  \n3. 确认供应商对该品项的常规交期及最小起订量，结合单日销量**14瓶**，人工试算安全库存与补货频次（如交期7天，则建议常备≥28瓶）。  \n4. 将该品纳入**30天重点观察清单**，每日记录可售数、销量、货架状态，待积累≥5个销售日后再评估是否列入常规动销模型。', NULL, NULL, 10963, '2026-11-11 12:42:07.729296', '2026-11-11 12:42:18.731973', 788, 484, 1272, 0, 'chatcmpl-e84a7755-2542-9d8f-96d3-7c5f135b3ba1');
INSERT INTO `ai_suggestion` VALUES (11, 'c279ed63-5354-464f-939e-34cc385baf2a', 'INVENTORY', 1, '系统管理员', '2026-10-13', '2026-11-11', '{\"productId\": \"1\"}', '{\"type\": \"INVENTORY\", \"coverage\": {\"selection\": \"最多50个商品，优先过期量、临期量，再按账面库存及商品ID；总计覆盖全部筛选范围。\", \"truncated\": false, \"includedProducts\": 1, \"matchingProducts\": 1}, \"products\": [{\"code\": \"P0001\", \"name\": \"演示瓶装饮料\", \"unit\": \"瓶\", \"bookQty\": 5, \"nearQty\": 0, \"productId\": \"1\", \"expiredQty\": 0, \"saleableQty\": 5, \"categoryName\": \"饮料乳品\", \"recentNetQty\": 14, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}], \"periodEnd\": \"2026-11-11\", \"periodStart\": \"2026-10-13\", \"businessDate\": \"2026-11-11\", \"periodNetQty\": 14, \"salesHistory\": \"所选区间有销售事件\", \"interpretation\": \"数量按各商品基本计数单位。当前库存是快照余额，期间净销量是历史流量，销量大于当前库存不表示账实不符；摘要未提供完整库存收支或实物清点，不能据此对账。补货量须结合真实交期、需求另行核定；所有建议须人工核实，不能自动执行。\", \"currentInventory\": {\"bookQty\": 5, \"expiredQty\": 0, \"saleableQty\": 5, \"nearBatchCount\": 0, \"lowProductCount\": 1, \"expiredBatchCount\": 0}}', '2026-11-11 12:55:14.988664', 'DASHSCOPE', 'qwen-plus', 0, 'SUCCESS', '### 数据现象  \n当前账面库存与可售库存均为**5瓶**，期间净销量达**14瓶**，远超当前余额，属正常历史销售累积结果；商品“演示瓶装饮料”是唯一被纳入分析的商品，其安全库存下限为**5瓶**，当前恰好处于临界值；该商品无临期或过期批次，最近一次入库时间为**2026-09-01**，距今已超70天；摘要中未体现任何补货记录或新入库动作。\n\n### 风险优先级  \n**高优先级：低库存风险**——当前可售量等于设定的库存下限，无缓冲余量，一旦发生销售或损耗即触发缺货；**中优先级：补货时效风险**——距上次入库已逾2个月，若交期较长或需求波动，易加剧断货可能；**低优先级：数据覆盖局限**——仅1个商品参与分析，整体经营态势代表性有限，无法反映品类结构或周转健康度。\n\n### 人工核查与行动建议  \n1. 核实该商品近30日实际销售节奏（如是否集中爆发、是否存在刷单或退货未冲销），判断**14瓶净销量是否具持续性**；  \n2. 查阅采购系统，确认**是否有待执行订单或已下单未到货情况**，结合供应商实际交期评估补货到位时间；  \n3. 现场清点实物，验证**5瓶是否全部可售（排除破损、标签脱落等不可售情形）**；  \n4. 审查“演示瓶装饮料”是否为测试用虚拟品，若非真实在售商品，应**从库存监控清单中剔除，避免干扰决策**。', NULL, NULL, 8982, '2026-11-11 12:55:15.098526', '2026-11-11 12:55:24.139810', 760, 386, 1146, 0, 'chatcmpl-f52db803-296b-922b-b52e-30875f95a13b');
INSERT INTO `ai_suggestion` VALUES (12, '09245504-cae9-4945-a2b0-4b3293fec395', 'INVENTORY', 1, '系统管理员', '2026-10-13', '2026-11-11', '{}', '{\"type\": \"INVENTORY\", \"coverage\": {\"selection\": \"最多50个商品，优先过期量、临期量，再按账面库存及商品ID；总计覆盖全部筛选范围。\", \"truncated\": false, \"includedProducts\": 36, \"matchingProducts\": 36}, \"products\": [{\"code\": \"P0018\", \"name\": \"到期当天食品\", \"unit\": \"件\", \"bookQty\": 4, \"nearQty\": 0, \"productId\": \"18\", \"expiredQty\": 4, \"saleableQty\": 0, \"categoryName\": \"糕点零食\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 2, \"expiredBatchCount\": 1}, {\"code\": \"P0003\", \"name\": \"临期酸奶\", \"unit\": \"件\", \"bookQty\": 12, \"nearQty\": 12, \"productId\": \"3\", \"expiredQty\": 0, \"saleableQty\": 12, \"categoryName\": \"饮料乳品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 1, \"stockLowerLimit\": 2, \"expiredBatchCount\": 0}, {\"code\": \"HX_BREAD_80\", \"name\": \"手作软面包80g\", \"unit\": \"袋\", \"bookQty\": 12, \"nearQty\": 12, \"productId\": \"27\", \"expiredQty\": 0, \"saleableQty\": 12, \"categoryName\": \"糕点零食\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-11T12:41:10.080332+08:00\", \"nearBatchCount\": 1, \"stockLowerLimit\": 6, \"expiredBatchCount\": 0}, {\"code\": \"P0019\", \"name\": \"明天到期食品\", \"unit\": \"件\", \"bookQty\": 6, \"nearQty\": 6, \"productId\": \"19\", \"expiredQty\": 0, \"saleableQty\": 6, \"categoryName\": \"糕点零食\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 1, \"stockLowerLimit\": 2, \"expiredBatchCount\": 0}, {\"code\": \"HX_MILK_250\", \"name\": \"晨光鲜牛奶250mL\", \"unit\": \"盒\", \"bookQty\": 30, \"nearQty\": 6, \"productId\": \"26\", \"expiredQty\": 0, \"saleableQty\": 30, \"categoryName\": \"饮料乳品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-11T12:41:10.080332+08:00\", \"nearBatchCount\": 1, \"stockLowerLimit\": 10, \"expiredBatchCount\": 0}, {\"code\": \"P0004\", \"name\": \"过期面包\", \"unit\": \"件\", \"bookQty\": 0, \"nearQty\": 0, \"productId\": \"4\", \"expiredQty\": 0, \"saleableQty\": 0, \"categoryName\": \"糕点零食\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-10-26T16:13:25.806904+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}, {\"code\": \"P0020\", \"name\": \"过期已清零食品\", \"unit\": \"件\", \"bookQty\": 0, \"nearQty\": 0, \"productId\": \"20\", \"expiredQty\": 0, \"saleableQty\": 0, \"categoryName\": \"糕点零食\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-10-26T16:13:25.806904+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 2, \"expiredBatchCount\": 0}, {\"code\": \"P0016\", \"name\": \"零库存商品\", \"unit\": \"件\", \"bookQty\": 0, \"nearQty\": 0, \"productId\": \"16\", \"expiredQty\": 0, \"saleableQty\": 0, \"categoryName\": \"文具用品\", \"recentNetQty\": 0, \"lastStockedAt\": null, \"nearBatchCount\": 0, \"stockLowerLimit\": 0, \"expiredBatchCount\": 0}, {\"code\": \"P0021\", \"name\": \"待审核采购商品\", \"unit\": \"件\", \"bookQty\": 0, \"nearQty\": 0, \"productId\": \"21\", \"expiredQty\": 0, \"saleableQty\": 0, \"categoryName\": \"文具用品\", \"recentNetQty\": 0, \"lastStockedAt\": null, \"nearBatchCount\": 0, \"stockLowerLimit\": 2, \"expiredBatchCount\": 0}, {\"code\": \"P0022\", \"name\": \"可删除未使用商品\", \"unit\": \"件\", \"bookQty\": 0, \"nearQty\": 0, \"productId\": \"22\", \"expiredQty\": 0, \"saleableQty\": 0, \"categoryName\": \"文具用品\", \"recentNetQty\": 0, \"lastStockedAt\": null, \"nearBatchCount\": 0, \"stockLowerLimit\": 0, \"expiredBatchCount\": 0}, {\"code\": \"P0024\", \"name\": \"缺日期驳回食品\", \"unit\": \"件\", \"bookQty\": 0, \"nearQty\": 0, \"productId\": \"24\", \"expiredQty\": 0, \"saleableQty\": 0, \"categoryName\": \"糕点零食\", \"recentNetQty\": 0, \"lastStockedAt\": null, \"nearBatchCount\": 0, \"stockLowerLimit\": 2, \"expiredBatchCount\": 0}, {\"code\": \"P0001\", \"name\": \"演示瓶装饮料\", \"unit\": \"瓶\", \"bookQty\": 5, \"nearQty\": 0, \"productId\": \"1\", \"expiredQty\": 0, \"saleableQty\": 5, \"categoryName\": \"饮料乳品\", \"recentNetQty\": 14, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}, {\"code\": \"P0009\", \"name\": \"并发测试矿泉水\", \"unit\": \"瓶\", \"bookQty\": 5, \"nearQty\": 0, \"productId\": \"9\", \"expiredQty\": 0, \"saleableQty\": 5, \"categoryName\": \"饮料乳品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}, {\"code\": \"P0014\", \"name\": \"禁止采购商品\", \"unit\": \"件\", \"bookQty\": 7, \"nearQty\": 0, \"productId\": \"14\", \"expiredQty\": 0, \"saleableQty\": 7, \"categoryName\": \"日用清洁\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 2, \"expiredBatchCount\": 0}, {\"code\": \"P0013\", \"name\": \"已停用历史商品\", \"unit\": \"件\", \"bookQty\": 8, \"nearQty\": 0, \"productId\": \"13\", \"expiredQty\": 0, \"saleableQty\": 0, \"categoryName\": \"日用清洁\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 2, \"expiredBatchCount\": 0}, {\"code\": \"P0015\", \"name\": \"禁止销售商品\", \"unit\": \"件\", \"bookQty\": 8, \"nearQty\": 0, \"productId\": \"15\", \"expiredQty\": 0, \"saleableQty\": 0, \"categoryName\": \"日用清洁\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 2, \"expiredBatchCount\": 0}, {\"code\": \"P0011\", \"name\": \"待审核盘点方便面\", \"unit\": \"件\", \"bookQty\": 10, \"nearQty\": 0, \"productId\": \"11\", \"expiredQty\": 0, \"saleableQty\": 10, \"categoryName\": \"粮油食品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 3, \"expiredBatchCount\": 0}, {\"code\": \"P0012\", \"name\": \"驳回盘点饼干\", \"unit\": \"件\", \"bookQty\": 12, \"nearQty\": 0, \"productId\": \"12\", \"expiredQty\": 0, \"saleableQty\": 12, \"categoryName\": \"糕点零食\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 3, \"expiredBatchCount\": 0}, {\"code\": \"HX_DRINK_500\", \"name\": \"禾序瓶装饮料500mL\", \"unit\": \"瓶\", \"bookQty\": 15, \"nearQty\": 0, \"productId\": \"25\", \"expiredQty\": 0, \"saleableQty\": 15, \"categoryName\": \"饮料乳品\", \"recentNetQty\": 14, \"lastStockedAt\": \"2026-11-11T12:40:12.618016+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}, {\"code\": \"P0010\", \"name\": \"盘点中文具笔\", \"unit\": \"件\", \"bookQty\": 20, \"nearQty\": 0, \"productId\": \"10\", \"expiredQty\": 0, \"saleableQty\": 20, \"categoryName\": \"文具用品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 3, \"expiredBatchCount\": 0}, {\"code\": \"P0017\", \"name\": \"新近入库商品\", \"unit\": \"件\", \"bookQty\": 20, \"nearQty\": 0, \"productId\": \"17\", \"expiredQty\": 0, \"saleableQty\": 20, \"categoryName\": \"文具用品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-09T16:13:25.806904+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 3, \"expiredBatchCount\": 0}, {\"code\": \"HX_TISSUE_3P\", \"name\": \"柔韧抽纸三包装\", \"unit\": \"件\", \"bookQty\": 24, \"nearQty\": 0, \"productId\": \"29\", \"expiredQty\": 0, \"saleableQty\": 24, \"categoryName\": \"日用清洁\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-11T12:41:10.712596+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 8, \"expiredBatchCount\": 0}, {\"code\": \"HX_BISCUIT_100\", \"name\": \"苏打饼干100g\", \"unit\": \"袋\", \"bookQty\": 24, \"nearQty\": 0, \"productId\": \"32\", \"expiredQty\": 0, \"saleableQty\": 24, \"categoryName\": \"糕点零食\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-11T12:41:10.513934+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 8, \"expiredBatchCount\": 0}, {\"code\": \"HX_DETERGENT_1KG\", \"name\": \"清新洗衣液1kg\", \"unit\": \"瓶\", \"bookQty\": 24, \"nearQty\": 0, \"productId\": \"36\", \"expiredQty\": 0, \"saleableQty\": 24, \"categoryName\": \"日用清洁\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-11T12:41:10.712596+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}, {\"code\": \"P0023\", \"name\": \"调价快照测试商品\", \"unit\": \"件\", \"bookQty\": 28, \"nearQty\": 0, \"productId\": \"23\", \"expiredQty\": 0, \"saleableQty\": 28, \"categoryName\": \"日用清洁\", \"recentNetQty\": 2, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 2, \"expiredBatchCount\": 0}, {\"code\": \"P0006\", \"name\": \"滞销食用油\", \"unit\": \"件\", \"bookQty\": 30, \"nearQty\": 0, \"productId\": \"6\", \"expiredQty\": 0, \"saleableQty\": 30, \"categoryName\": \"粮油食品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 3, \"expiredBatchCount\": 0}, {\"code\": \"P0008\", \"name\": \"香皂\", \"unit\": \"件\", \"bookQty\": 30, \"nearQty\": 0, \"productId\": \"8\", \"expiredQty\": 0, \"saleableQty\": 30, \"categoryName\": \"日用清洁\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-08T16:13:25.806904+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}, {\"code\": \"HX_RICE_5KG\", \"name\": \"东北珍珠米5kg\", \"unit\": \"袋\", \"bookQty\": 30, \"nearQty\": 0, \"productId\": \"28\", \"expiredQty\": 0, \"saleableQty\": 30, \"categoryName\": \"粮油食品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-11T12:41:10.513934+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}, {\"code\": \"HX_NOTE_A5\", \"name\": \"A5横线笔记本\", \"unit\": \"本\", \"bookQty\": 30, \"nearQty\": 0, \"productId\": \"35\", \"expiredQty\": 0, \"saleableQty\": 30, \"categoryName\": \"文具用品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-11T12:41:10.712596+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 8, \"expiredBatchCount\": 0}, {\"code\": \"HX_SOAP_100\", \"name\": \"清洁香皂100g\", \"unit\": \"件\", \"bookQty\": 36, \"nearQty\": 0, \"productId\": \"30\", \"expiredQty\": 0, \"saleableQty\": 36, \"categoryName\": \"日用清洁\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-11T12:41:10.712596+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 8, \"expiredBatchCount\": 0}, {\"code\": \"P0002\", \"name\": \"纯牛奶\", \"unit\": \"件\", \"bookQty\": 38, \"nearQty\": 0, \"productId\": \"2\", \"expiredQty\": 0, \"saleableQty\": 38, \"categoryName\": \"饮料乳品\", \"recentNetQty\": 2, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 10, \"expiredBatchCount\": 0}, {\"code\": \"P0007\", \"name\": \"抽纸三包装\", \"unit\": \"件\", \"bookQty\": 40, \"nearQty\": 0, \"productId\": \"7\", \"expiredQty\": 0, \"saleableQty\": 40, \"categoryName\": \"日用清洁\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}, {\"code\": \"HX_NOODLE_5P\", \"name\": \"香菇炖鸡面五连包\", \"unit\": \"袋\", \"bookQty\": 40, \"nearQty\": 0, \"productId\": \"31\", \"expiredQty\": 0, \"saleableQty\": 40, \"categoryName\": \"粮油食品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-11T12:41:10.513934+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 10, \"expiredBatchCount\": 0}, {\"code\": \"P0005\", \"name\": \"大米五公斤\", \"unit\": \"件\", \"bookQty\": 45, \"nearQty\": 0, \"productId\": \"5\", \"expiredQty\": 0, \"saleableQty\": 45, \"categoryName\": \"粮油食品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-09-01T00:00+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 5, \"expiredBatchCount\": 0}, {\"code\": \"HX_PEN_05\", \"name\": \"顺滑中性笔0.5mm\", \"unit\": \"支\", \"bookQty\": 60, \"nearQty\": 0, \"productId\": \"34\", \"expiredQty\": 0, \"saleableQty\": 60, \"categoryName\": \"文具用品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-11T12:41:10.712596+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 12, \"expiredBatchCount\": 0}, {\"code\": \"HX_WATER_550\", \"name\": \"天然饮用水550mL\", \"unit\": \"瓶\", \"bookQty\": 96, \"nearQty\": 0, \"productId\": \"33\", \"expiredQty\": 0, \"saleableQty\": 96, \"categoryName\": \"饮料乳品\", \"recentNetQty\": 0, \"lastStockedAt\": \"2026-11-11T12:41:10.712596+08:00\", \"nearBatchCount\": 0, \"stockLowerLimit\": 10, \"expiredBatchCount\": 0}], \"periodEnd\": \"2026-11-11\", \"periodStart\": \"2026-10-13\", \"businessDate\": \"2026-11-11\", \"periodNetQty\": 32, \"salesHistory\": \"所选区间有销售事件\", \"interpretation\": \"数量按各商品基本计数单位。当前库存是快照余额，期间净销量是历史流量，销量大于当前库存不表示账实不符；摘要未提供完整库存收支或实物清点，不能据此对账。补货量须结合真实交期、需求另行核定；所有建议须人工核实，不能自动执行。\", \"currentInventory\": {\"bookQty\": 749, \"expiredQty\": 4, \"saleableQty\": 729, \"nearBatchCount\": 4, \"lowProductCount\": 9, \"expiredBatchCount\": 1}}', '2026-11-11 14:09:04.467696', 'DASHSCOPE', 'qwen-flash', 0, 'SUCCESS', '### 数据现象\n\n当前快照显示，可售库存总量为**729**件，账面库存为**749**件，其中过期库存**4**件，临期批次共**4**批。商品“到期当天食品”（P0018）账面4件、可售0件，且已过期1批；“临期酸奶”（P0003）有12件处于临期状态，但无销售记录更新。部分商品如“演示瓶装饮料”和“禾序瓶装饮料500mL”近期净销量分别为14件，但均未触发补货动作。新近入库商品（如HX_BREAD_80、HX_MILK_250等）在当日完成入库，但未体现销售波动。\n\n### 风险优先级\n\n**高风险：**  \n- “到期当天食品”（P0018）账面4件全部过期，且不可售，存在损失与管理疏漏风险。  \n- “临期酸奶”（P0003）12件处于临期状态，若未及时促销或下架，可能转为过期库存。  \n\n**中风险：**  \n- 多个商品（如手作软面包、晨光鲜牛奶）虽库存充足，但临近保质期，需关注后续周转。  \n- 低库存商品共**9**种，其中部分如“纯牛奶”、“苏打饼干”等为高频销售品，需警惕断货。\n\n### 人工核查与行动建议\n\n1. **立即核查并处理过期商品**：对“到期当天食品”（P0018）的4件过期库存进行实物清点与报废流程确认，避免影响陈列与顾客信任。\n2. **启动临期商品促销计划**：针对“临期酸奶”（P0003）及“明天到期食品”（P0019）等共**6**件临期商品，制定限时折扣或捆绑促销方案，加速出清。\n3. **检查低库存商品补货节奏**：对账面库存低于安全线的**9**类商品，结合实际销售趋势与供应商交期，评估是否需紧急补货。\n4. **复核新入库商品的销售表现**：对2026年11月11日新入库的“手作软面包”“晨光鲜牛奶”等商品，观察其销售转化率，判断是否需调整采购量或陈列策略。', NULL, NULL, 4129, '2026-11-11 14:09:06.495666', '2026-11-11 14:09:10.749936', 4575, 552, 5127, 0, 'chatcmpl-3c27434d-ab92-9a13-a006-2a38927738e2');
INSERT INTO `ai_suggestion` VALUES (13, 'd17bfbc3-da8b-4390-a2fe-a8e0e1731400', 'SALES', 1, '系统管理员', '2026-10-13', '2026-11-11', '{\"productId\": \"25\"}', '{\"type\": \"SALES\", \"periodEnd\": \"2026-11-11\", \"dailyTrend\": [{\"date\": \"2026-11-11\", \"netQty\": 14, \"netAmount\": \"70.00\"}], \"hotProducts\": [{\"code\": \"HX_DRINK_500\", \"name\": \"禾序瓶装饮料500mL\", \"unit\": \"瓶\", \"netQty\": 14, \"netAmount\": \"70.00\", \"productId\": \"25\", \"categoryId\": \"1\", \"categoryName\": \"饮料乳品\"}], \"periodStart\": \"2026-10-13\", \"salesTotals\": {\"netQty\": 14, \"netAmount\": \"70.00\", \"completedCount\": 2, \"correctedCount\": 0}, \"businessDate\": \"2026-11-11\", \"salesHistory\": \"所选区间有销售事件\", \"trendCoverage\": {\"note\": \"仅含最近最多366个事件日，区间总计仍覆盖完整筛选。\", \"eventDays\": 1, \"includedDays\": 1}, \"interpretation\": \"数量按各商品基本计数单位。当前库存是快照余额，期间净销量是历史流量，销量大于当前库存不表示账实不符；摘要未提供完整库存收支或实物清点，不能据此对账。补货量须结合真实交期、需求另行核定；所有建议须人工核实，不能自动执行。\", \"currentSlowWindow\": {\"end\": \"2026-11-11\", \"note\": \"当前有可售库存、窗口零净销量；首次有效入库不足30天单列观察不足，需结合缺货和到货人工判断。\", \"start\": \"2026-10-13\", \"products\": [{\"code\": \"HX_DRINK_500\", \"name\": \"禾序瓶装饮料500mL\", \"unit\": \"瓶\", \"netQty\": 14, \"productId\": \"25\", \"observation\": \"INSUFFICIENT_HISTORY\", \"saleableQty\": 15, \"firstStockedAt\": \"2026-11-11T12:27:58.169611+08:00\"}], \"matchingProducts\": 1}}', '2026-11-11 16:58:46.718494', 'DASHSCOPE', 'qwen-plus', 0, 'SUCCESS', '### 数据现象  \n所选周期（2026-10-13至2026-11-11）内，仅**2026-11-11当日**发生销售，净销量**14瓶**，销售额70.00元，全部集中于单一商品“禾序瓶装饮料500mL”。该商品当前可售库存为**15瓶**，首次入库时间为**2026-11-11 12:27**，即当天入库后即产生销售；因观察期不足30天且仅1天有销，系统标记为“INSUFFICIENT_HISTORY”，销售数据代表性弱。\n\n### 风险优先级  \n当前无明确高风险项：无临期/过期商品提示，无可售库存为零的缺货记录，账面与可售库存差额未体现（快照中未提供账面库存值，仅知可售库存15瓶）。但存在**经营观察盲区风险**——单日销售无法判断真实动销趋势，若该商品实为新上架试销或临时促销，持续性存疑；若为常态需求，则15瓶可售库存仅覆盖约1天销量，补货响应压力隐性存在。\n\n### 人工核查与行动建议  \n1. 核查“禾序瓶装饮料500mL”**实际入库时间与数量**，确认11月11日是否为首批量到货，以及本次到货是否含此前未计入可售库存的在途或待上架部分。  \n2. 调取该商品**近7日进销存明细及收银小票**，验证14瓶销售是否真实、集中于某时段/顾客，排除录入误差或异常交易。  \n3. 结合门店客流、竞品动向及季节因素，评估该饮料是否具备**持续热销基础**，避免因单日数据误判补货节奏。  \n4. 若确认为常规需求，须立即**人工核定供应商交期与最小起订量**，测算安全库存阈值，不可依赖当前15瓶余额推断供应裕度。', NULL, NULL, 13147, '2026-11-11 16:58:50.996141', '2026-11-11 16:59:04.201147', 881, 476, 1357, 0, 'chatcmpl-419713f8-117d-9971-aabc-9d9f9f27c191');

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
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '一级商品分类，引用后限制删除' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 179 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '只追加操作历史；JSON 保存不参与库存计算的审计快照' ROW_FORMAT = Dynamic;

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
INSERT INTO `operation_log` VALUES (29, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '25', 'HX_DRINK_500', 'SUCCESS', NULL, NULL, NULL, '3e1a5c2d-d333-48e7-b285-e1d9cd097b08', '2026-11-11 12:04:36.477081');
INSERT INTO `operation_log` VALUES (30, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '26', 'HX_MILK_250', 'SUCCESS', NULL, NULL, NULL, '2a1478ad-78e9-4a2b-a299-dc19304b9824', '2026-11-11 12:05:49.191327');
INSERT INTO `operation_log` VALUES (31, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '27', 'HX_BREAD_80', 'SUCCESS', NULL, NULL, NULL, 'efacf9e4-74b9-4698-88a9-d078bc8b81dd', '2026-11-11 12:06:07.382839');
INSERT INTO `operation_log` VALUES (32, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '28', 'HX_RICE_5KG', 'SUCCESS', NULL, NULL, NULL, '30b54b8e-9c87-48fe-8fd2-772fbcbc7237', '2026-11-11 12:06:25.553940');
INSERT INTO `operation_log` VALUES (33, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '29', 'HX_TISSUE_3P', 'SUCCESS', NULL, NULL, NULL, '607bdfc0-3ff1-4945-a62c-9c6023cce6b3', '2026-11-11 12:06:37.707237');
INSERT INTO `operation_log` VALUES (34, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '30', 'HX_SOAP_100', 'SUCCESS', NULL, NULL, NULL, '6e8562cf-80fa-4b16-bb7d-31add533de3c', '2026-11-11 12:07:20.176708');
INSERT INTO `operation_log` VALUES (35, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '31', 'HX_NOODLE_5P', 'SUCCESS', NULL, NULL, NULL, 'a518ae35-62e2-47e3-9777-a3cec25814e9', '2026-11-11 12:07:38.335115');
INSERT INTO `operation_log` VALUES (36, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '32', 'HX_BISCUIT_100', 'SUCCESS', NULL, NULL, NULL, 'eaa63315-57dd-40fc-bb4a-f87774f4521f', '2026-11-11 12:07:56.482890');
INSERT INTO `operation_log` VALUES (37, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '33', 'HX_WATER_550', 'SUCCESS', NULL, NULL, NULL, 'a4bae311-d3c8-4799-9bc3-91e03556b142', '2026-11-11 12:08:14.708709');
INSERT INTO `operation_log` VALUES (38, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '34', 'HX_PEN_05', 'SUCCESS', NULL, NULL, NULL, '5ff17c41-9c98-435d-981c-2127228591ff', '2026-11-11 12:10:59.497701');
INSERT INTO `operation_log` VALUES (39, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '35', 'HX_NOTE_A5', 'SUCCESS', NULL, NULL, NULL, 'f0ad3b61-953e-4de8-8ee0-b090b224b4c8', '2026-11-11 12:11:15.645981');
INSERT INTO `operation_log` VALUES (40, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '36', 'HX_DETERGENT_1KG', 'SUCCESS', NULL, NULL, NULL, '7e1d671f-5b3e-468b-88f1-0480003e99b1', '2026-11-11 12:11:31.770412');
INSERT INTO `operation_log` VALUES (41, 1, '系统管理员', 'SUPPLIER_CREATE', 'SUPPLIER', '9', 'SUPa4ba67a85acf41b1b87fa0a2', 'SUCCESS', NULL, NULL, NULL, '74fc553e-ed71-47e7-a658-0180d5cbab09', '2026-11-11 12:12:55.490724');
INSERT INTO `operation_log` VALUES (42, 3, '采购张三', 'SUPPLIER_CREATE', 'SUPPLIER', '10', 'SUP608701e13df9419686ddf9af', 'SUCCESS', NULL, NULL, NULL, 'f6cfb3f5-f787-4dbc-b4fb-ee7b26bed121', '2026-11-11 12:15:47.004527');
INSERT INTO `operation_log` VALUES (43, 3, '采购张三', 'SUPPLIER_CREATE', 'SUPPLIER', '11', 'SUPd06c829007074167bc39d1e0', 'SUCCESS', NULL, NULL, NULL, 'e528457b-ec55-47e5-a7d6-e8cee391bc55', '2026-11-11 12:15:53.051425');
INSERT INTO `operation_log` VALUES (44, 3, '采购张三', 'SUPPLIER_EDIT', 'SUPPLIER', '10', 'SUP608701e13df9419686ddf9af', 'SUCCESS', '更新当前档案，保留历史单据快照', NULL, NULL, '0d458c04-3fe0-4a67-b3bb-80ff761fd7b2', '2026-11-11 12:17:25.980540');
INSERT INTO `operation_log` VALUES (45, 3, '采购张三', 'SUPPLIER_STATUS_CHANGE', 'SUPPLIER', '10', 'SUP608701e13df9419686ddf9af', 'SUCCESS', '停用', NULL, NULL, 'eca8dc19-1ba5-460b-b723-ecced16baf6e', '2026-11-11 12:17:34.083130');
INSERT INTO `operation_log` VALUES (46, 3, '采购张三', 'SUPPLIER_STATUS_CHANGE', 'SUPPLIER', '10', 'SUP608701e13df9419686ddf9af', 'SUCCESS', '启用', NULL, NULL, '72bd5dbc-c06e-47cc-ac0f-0115a56e4cef', '2026-11-11 12:17:43.175535');
INSERT INTO `operation_log` VALUES (47, 3, '采购张三', 'PURCHASE_CREATE', 'PURCHASE', '11', 'CG20261111-bb6deb4160294acd8a2912e8', 'SUCCESS', NULL, NULL, NULL, 'b2ca8adf-239d-4527-b34f-7ad120b91887', '2026-11-11 12:19:12.113616');
INSERT INTO `operation_log` VALUES (48, 3, '采购张三', 'PURCHASE_EDIT', 'PURCHASE', '11', 'CG20261111-bb6deb4160294acd8a2912e8', 'SUCCESS', NULL, NULL, NULL, '6067ea14-518b-4a51-9bee-08aab493ef8b', '2026-11-11 12:21:25.475316');
INSERT INTO `operation_log` VALUES (49, 3, '采购张三', 'PURCHASE_EDIT', 'PURCHASE', '11', 'CG20261111-bb6deb4160294acd8a2912e8', 'SUCCESS', NULL, NULL, NULL, '6be5d83d-40d5-4d3e-af40-5d5407194174', '2026-11-11 12:21:53.741385');
INSERT INTO `operation_log` VALUES (50, 3, '采购张三', 'PURCHASE_EDIT', 'PURCHASE', '11', 'CG20261111-bb6deb4160294acd8a2912e8', 'SUCCESS', NULL, NULL, NULL, 'f2f28d6a-0c49-4727-bf0b-1794baa89b3e', '2026-11-11 12:27:19.834766');
INSERT INTO `operation_log` VALUES (51, 3, '采购张三', 'PURCHASE_SUBMIT', 'PURCHASE', '11', 'CG20261111-bb6deb4160294acd8a2912e8', 'SUCCESS', NULL, NULL, NULL, 'd3901200-ef2c-4028-8c85-363a3731fe4d', '2026-11-11 12:27:25.871632');
INSERT INTO `operation_log` VALUES (52, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '11', 'CG20261111-bb6deb4160294acd8a2912e8', 'SUCCESS', NULL, NULL, NULL, '5e3d1163-a341-4024-9e36-a119ca862289', '2026-11-11 12:27:58.174673');
INSERT INTO `operation_log` VALUES (53, 3, '采购张三', 'RECONCILE_MATCH', 'PURCHASE', '11', 'CG20261111-bb6deb4160294acd8a2912e8', 'SUCCESS', NULL, '{\"lines\": [{\"note\": null, \"itemId\": \"35\", \"externalQty\": null, \"externalUnitCost\": null}, {\"note\": null, \"itemId\": \"36\", \"externalQty\": null, \"externalUnitCost\": null}], \"status\": \"UNRECONCILED\", \"reference\": null, \"difference\": null, \"resolution\": null, \"externalTotal\": null}', '{\"lines\": [{\"note\": null, \"itemId\": \"35\", \"externalQty\": 10, \"externalUnitCost\": \"3.00\"}, {\"note\": null, \"itemId\": \"36\", \"externalQty\": 10, \"externalUnitCost\": \"3.00\"}], \"status\": \"MATCHED\", \"reference\": \"HX-20261111-001\", \"difference\": null, \"resolution\": null, \"externalTotal\": \"60.00\"}', '028b8796-c270-45ca-a851-0e7f73f1128e', '2026-11-11 12:28:27.401570');
INSERT INTO `operation_log` VALUES (54, 5, '收银王五', 'SALE_COMPLETE', 'SALE', '8', 'XS20261111-2e0c4715094e4cc1a1c5c80d', 'SUCCESS', NULL, NULL, NULL, '83cbccec-bf5b-49b5-a1e6-bfe9dd4687ae', '2026-11-11 12:28:47.801881');
INSERT INTO `operation_log` VALUES (55, 3, '采购张三', 'STOCKTAKE_START', 'STOCKTAKE', '6', 'PD20261111-c6484bc69f0845349b0e38ec', 'SUCCESS', NULL, NULL, NULL, 'c95e4866-3115-4562-93c0-c1e606f02136', '2026-11-11 12:29:16.642825');
INSERT INTO `operation_log` VALUES (56, 3, '采购张三', 'STOCKTAKE_SAVE', 'STOCKTAKE', '6', 'PD20261111-c6484bc69f0845349b0e38ec', 'SUCCESS', NULL, NULL, NULL, 'b0f82169-b383-487a-a6e8-b991a7350169', '2026-11-11 12:30:01.551587');
INSERT INTO `operation_log` VALUES (57, 3, '采购张三', 'STOCKTAKE_PENDING', 'STOCKTAKE', '6', 'PD20261111-c6484bc69f0845349b0e38ec', 'SUCCESS', NULL, NULL, NULL, '873096f2-3633-4403-a7e5-bdcc663697d7', '2026-11-11 12:31:06.893611');
INSERT INTO `operation_log` VALUES (58, 1, '系统管理员', 'STOCKTAKE_COMPLETED', 'STOCKTAKE', '6', 'PD20261111-c6484bc69f0845349b0e38ec', 'SUCCESS', NULL, NULL, NULL, '868d0766-4734-485c-8abf-76408aef0de2', '2026-11-11 12:31:19.109449');
INSERT INTO `operation_log` VALUES (59, 5, '收银王五', 'SALE_COMPLETE', 'SALE', '9', 'XS20261111-3a8fe78e7b7e429c95ea7652', 'SUCCESS', NULL, NULL, NULL, '66d5ad63-c92b-4b10-bcba-c8fc0ddbe433', '2026-11-11 12:31:32.295192');
INSERT INTO `operation_log` VALUES (60, 3, '采购张三', 'AI_GENERATE_SUCCESS', 'AI', '7', '9018d931-dc63-43fb-89c9-70bbec35cefa', 'SUCCESS', NULL, NULL, NULL, NULL, '2026-11-11 12:33:45.195653');
INSERT INTO `operation_log` VALUES (61, 1, '系统管理员', 'AI_CONFIG_UPDATE', 'CONFIG', 'ai.provider_settings', 'ai.provider_settings', 'SUCCESS', NULL, '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 0, \"keySource\": \"LOCAL\", \"defaultModel\": \"qwen-plus\", \"keyConfigured\": true}', '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 1, \"keySource\": \"MANAGED\", \"defaultModel\": \"qwen-flash\", \"keyConfigured\": true}', 'd35d02fd-6a20-476a-a232-2728daf5d354', '2026-11-11 12:37:24.601390');
INSERT INTO `operation_log` VALUES (62, 1, '系统管理员', 'AI_CONFIG_UPDATE', 'CONFIG', 'ai.provider_settings', 'ai.provider_settings', 'SUCCESS', NULL, '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 1, \"keySource\": \"MANAGED\", \"defaultModel\": \"qwen-flash\", \"keyConfigured\": true}', '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 2, \"keySource\": \"MANAGED\", \"defaultModel\": \"qwen-flash\", \"keyConfigured\": true}', '85ae59c0-d12a-47c2-b9c3-66eee76c6b15', '2026-11-11 12:37:42.058649');
INSERT INTO `operation_log` VALUES (63, 1, '系统管理员', 'AI_GENERATE_SUCCESS', 'AI', '8', 'a332968a-7151-4941-84b8-408f9979db0b', 'SUCCESS', NULL, NULL, NULL, NULL, '2026-11-11 12:38:53.552057');
INSERT INTO `operation_log` VALUES (64, 3, '采购张三', 'PURCHASE_CREATE', 'PURCHASE', '12', 'CG20261111-69ff3b05ca644b9bbfb40941', 'SUCCESS', NULL, NULL, NULL, 'f2e9ebf9-8e1c-4006-8ac5-16f357267331', '2026-11-11 12:39:59.241021');
INSERT INTO `operation_log` VALUES (65, 3, '采购张三', 'PURCHASE_SUBMIT', 'PURCHASE', '12', 'CG20261111-69ff3b05ca644b9bbfb40941', 'SUCCESS', NULL, NULL, NULL, '575caca6-653d-4719-9e89-215b629b5aaa', '2026-11-11 12:39:59.625197');
INSERT INTO `operation_log` VALUES (66, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '12', 'CG20261111-69ff3b05ca644b9bbfb40941', 'SUCCESS', NULL, NULL, NULL, '47739cd8-a50e-4545-b005-c070a7d6afb4', '2026-11-11 12:40:12.623470');
INSERT INTO `operation_log` VALUES (67, 3, '采购张三', 'PURCHASE_CREATE', 'PURCHASE', '13', 'CG20261111-906bebcb72b046dba9954207', 'SUCCESS', NULL, NULL, NULL, '4565161e-44fe-4bd8-9327-88fe1f9fd09f', '2026-11-11 12:40:27.890428');
INSERT INTO `operation_log` VALUES (68, 3, '采购张三', 'PURCHASE_SUBMIT', 'PURCHASE', '13', 'CG20261111-906bebcb72b046dba9954207', 'SUCCESS', NULL, NULL, NULL, 'b9dea354-44ae-4e0b-9cc4-c726b1ca9237', '2026-11-11 12:40:28.270401');
INSERT INTO `operation_log` VALUES (69, 3, '采购张三', 'PURCHASE_CREATE', 'PURCHASE', '14', 'CG20261111-d04da38a7f4a43a29910ef96', 'SUCCESS', NULL, NULL, NULL, '8b51683a-09a0-48e6-8513-969ff639f4cf', '2026-11-11 12:40:39.703272');
INSERT INTO `operation_log` VALUES (70, 3, '采购张三', 'PURCHASE_SUBMIT', 'PURCHASE', '14', 'CG20261111-d04da38a7f4a43a29910ef96', 'SUCCESS', NULL, NULL, NULL, 'ced81fcc-b898-40f9-b523-aae2b1769b7d', '2026-11-11 12:40:40.071669');
INSERT INTO `operation_log` VALUES (71, 3, '采购张三', 'PURCHASE_CREATE', 'PURCHASE', '15', 'CG20261111-3684ad613a9a416fa76442ee', 'SUCCESS', NULL, NULL, NULL, '72ea6f1e-3837-41f8-b306-a57ecac92c8b', '2026-11-11 12:40:57.588270');
INSERT INTO `operation_log` VALUES (72, 3, '采购张三', 'PURCHASE_SUBMIT', 'PURCHASE', '15', 'CG20261111-3684ad613a9a416fa76442ee', 'SUCCESS', NULL, NULL, NULL, '6f451232-a7f7-439c-bbde-4bdd7cdfe1c5', '2026-11-11 12:40:57.966713');
INSERT INTO `operation_log` VALUES (73, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '13', 'CG20261111-906bebcb72b046dba9954207', 'SUCCESS', NULL, NULL, NULL, '2ed680ed-ad16-4c84-ae30-19b7a485bae4', '2026-11-11 12:41:10.086028');
INSERT INTO `operation_log` VALUES (74, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '14', 'CG20261111-d04da38a7f4a43a29910ef96', 'SUCCESS', NULL, NULL, NULL, 'ebbfadac-413d-4cc4-8cbf-cb081b70c051', '2026-11-11 12:41:10.520935');
INSERT INTO `operation_log` VALUES (75, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '15', 'CG20261111-3684ad613a9a416fa76442ee', 'SUCCESS', NULL, NULL, NULL, 'bcf8cb9f-8104-4ff8-9de9-45e6212505f6', '2026-11-11 12:41:10.720188');
INSERT INTO `operation_log` VALUES (76, 1, '系统管理员', 'AI_CONFIG_UPDATE', 'CONFIG', 'ai.provider_settings', 'ai.provider_settings', 'SUCCESS', NULL, '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 2, \"keySource\": \"MANAGED\", \"defaultModel\": \"qwen-flash\", \"keyConfigured\": true}', '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 3, \"keySource\": \"MANAGED\", \"defaultModel\": \"qwen-flash\", \"keyConfigured\": true}', '835252e8-d034-4dd7-9eba-fba3f512da16', '2026-11-11 12:41:38.281799');
INSERT INTO `operation_log` VALUES (77, 1, '系统管理员', 'AI_GENERATE_FAILURE', 'AI', '9', 'ecd590c8-1b6a-467d-a806-bd1dd6f6a7fc', 'FAILURE', '通义千问认证失败，请检查密钥、地域或模型授权', NULL, NULL, NULL, '2026-11-11 12:41:38.685153');
INSERT INTO `operation_log` VALUES (78, 1, '系统管理员', 'AI_CONFIG_UPDATE', 'CONFIG', 'ai.provider_settings', 'ai.provider_settings', 'SUCCESS', NULL, '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 3, \"keySource\": \"MANAGED\", \"defaultModel\": \"qwen-flash\", \"keyConfigured\": true}', '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 4, \"keySource\": \"MANAGED\", \"defaultModel\": \"qwen-plus\", \"keyConfigured\": true}', '02c72fac-6570-447c-8320-a4b4ae030852', '2026-11-11 12:41:53.165428');
INSERT INTO `operation_log` VALUES (79, 1, '系统管理员', 'AI_GENERATE_SUCCESS', 'AI', '10', '201d2e68-0521-4a9c-bd19-d153aea1f49a', 'SUCCESS', NULL, NULL, NULL, NULL, '2026-11-11 12:42:18.733972');
INSERT INTO `operation_log` VALUES (80, 1, '系统管理员', 'USER_CREATE', 'USER', '9', 'qa_clerk_1111', 'SUCCESS', NULL, 'null', '{\"id\": \"9\", \"enabled\": true, \"realName\": \"验收临时收银\", \"roleCode\": \"CASHIER\", \"username\": \"qa_clerk_1111\", \"phoneMasked\": null}', 'a0f05972-5d0d-4d0d-adcc-47ea8fae0fc6', '2026-11-11 12:46:02.868539');
INSERT INTO `operation_log` VALUES (81, 1, '系统管理员', 'USER_RESET_PASSWORD', 'USER', '9', 'qa_clerk_1111', 'SUCCESS', '管理员重置密码，原登录凭证已失效；请线下告知使用者', NULL, NULL, '7e08b395-1baa-4002-b463-d1bd7cddb83b', '2026-11-11 12:47:40.586054');
INSERT INTO `operation_log` VALUES (82, 1, '系统管理员', 'USER_STATUS', 'USER', '9', 'qa_clerk_1111', 'SUCCESS', '停用账号，原登录凭证已失效', '{\"id\": \"9\", \"enabled\": true, \"realName\": \"验收临时收银\", \"roleCode\": \"CASHIER\", \"username\": \"qa_clerk_1111\", \"phoneMasked\": null}', '{\"id\": \"9\", \"enabled\": false, \"realName\": \"验收临时收银\", \"roleCode\": \"CASHIER\", \"username\": \"qa_clerk_1111\", \"phoneMasked\": null}', 'c9e3bd4d-91f2-4557-8b14-f17100828364', '2026-11-11 12:47:49.533464');
INSERT INTO `operation_log` VALUES (83, 1, '系统管理员', 'USER_STATUS', 'USER', '9', 'qa_clerk_1111', 'SUCCESS', '启用账号', '{\"id\": \"9\", \"enabled\": false, \"realName\": \"验收临时收银\", \"roleCode\": \"CASHIER\", \"username\": \"qa_clerk_1111\", \"phoneMasked\": null}', '{\"id\": \"9\", \"enabled\": true, \"realName\": \"验收临时收银\", \"roleCode\": \"CASHIER\", \"username\": \"qa_clerk_1111\", \"phoneMasked\": null}', 'a1c09903-d891-4d75-bf40-24533e68c246', '2026-11-11 12:47:53.058523');
INSERT INTO `operation_log` VALUES (84, 1, '系统管理员', 'USER_EDIT', 'USER', '9', 'qa_clerk_1111', 'SUCCESS', '岗位变更，原登录凭证已失效', '{\"id\": \"9\", \"enabled\": true, \"realName\": \"验收临时收银\", \"roleCode\": \"CASHIER\", \"username\": \"qa_clerk_1111\", \"phoneMasked\": null}', '{\"id\": \"9\", \"enabled\": true, \"realName\": \"验收临时收银\", \"roleCode\": \"PURCHASER\", \"username\": \"qa_clerk_1111\", \"phoneMasked\": null}', 'c847204c-943a-4757-903e-26fde18f79ff', '2026-11-11 12:48:06.291376');
INSERT INTO `operation_log` VALUES (85, 1, '系统管理员', 'USER_DELETE', 'USER', '9', 'qa_clerk_1111', 'SUCCESS', '删除从未产生业务关联的账号', '{\"id\": \"9\", \"enabled\": true, \"realName\": \"验收临时收银\", \"roleCode\": \"PURCHASER\", \"username\": \"qa_clerk_1111\", \"phoneMasked\": null}', 'null', '9134ccec-f3d5-45d2-8035-fb7f44c551d3', '2026-11-11 12:48:16.859008');
INSERT INTO `operation_log` VALUES (86, 1, '系统管理员', 'ROLE_SAVE', 'ROLE', 'CASHIER', 'CASHIER', 'SUCCESS', '岗位授权版本已更新，相关账号原登录凭证失效', '{\"role\": \"CASHIER\", \"permissions\": [\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"product.sale_lookup\", \"sale\", \"sale.order\", \"sale.order.complete\", \"inventory\", \"inventory.query\", \"statistics\", \"statistics.sales\"]}', '{\"role\": \"CASHIER\", \"permissions\": [\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"product.sale_lookup\", \"sale\", \"sale.order\", \"sale.order.complete\", \"inventory\", \"inventory.query\", \"statistics\"]}', '84e5a6e0-4a4d-42f3-bff7-94eec0962bf6', '2026-11-11 12:49:08.743518');
INSERT INTO `operation_log` VALUES (87, 1, '系统管理员', 'ROLE_RESTORE', 'ROLE', 'CASHIER', 'CASHIER', 'SUCCESS', '岗位授权版本已更新，相关账号原登录凭证失效', '{\"role\": \"CASHIER\", \"permissions\": [\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"product.sale_lookup\", \"sale\", \"sale.order\", \"sale.order.complete\", \"inventory\", \"inventory.query\", \"statistics\"]}', '{\"role\": \"CASHIER\", \"permissions\": [\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"product.sale_lookup\", \"sale\", \"sale.order\", \"sale.order.complete\", \"inventory\", \"inventory.query\", \"statistics\", \"statistics.sales\"]}', 'a7b6876e-dbca-43c8-8e8e-9f49d32761a8', '2026-11-11 12:49:12.918014');
INSERT INTO `operation_log` VALUES (88, 1, '系统管理员', 'USER_CREATE', 'USER', '10', 'qa_buyer_1111', 'SUCCESS', NULL, 'null', '{\"id\": \"10\", \"enabled\": true, \"realName\": \"验收任务接续\", \"roleCode\": \"PURCHASER\", \"username\": \"qa_buyer_1111\", \"phoneMasked\": null}', '29c1e5da-a0a1-4b81-a9d6-1f649a6a8ca5', '2026-11-11 12:50:15.742082');
INSERT INTO `operation_log` VALUES (89, 10, '验收任务接续', 'STOCKTAKE_START', 'STOCKTAKE', '7', 'PD20261111-bbb12005b86d4fda87b647b9', 'SUCCESS', NULL, NULL, NULL, '65abce9f-a926-473e-8a3d-e724d1f81b94', '2026-11-11 12:50:17.944501');
INSERT INTO `operation_log` VALUES (90, 10, '验收任务接续', 'STOCKTAKE_SAVE', 'STOCKTAKE', '7', 'PD20261111-bbb12005b86d4fda87b647b9', 'SUCCESS', NULL, NULL, NULL, '4d324185-a21e-4242-aa8b-24ec15f90320', '2026-11-11 12:50:18.315000');
INSERT INTO `operation_log` VALUES (91, 10, '验收任务接续', 'STOCKTAKE_PENDING', 'STOCKTAKE', '7', 'PD20261111-bbb12005b86d4fda87b647b9', 'SUCCESS', NULL, NULL, NULL, '26d82840-a14f-4a45-be2f-0b6b6d8d87b1', '2026-11-11 12:50:18.709607');
INSERT INTO `operation_log` VALUES (92, 1, '系统管理员', 'STOCKTAKE_REJECTED', 'STOCKTAKE', '7', 'PD20261111-bbb12005b86d4fda87b647b9', 'SUCCESS', '验收演示：请经办人再次逐批核对，盘点冻结继续保留。', NULL, NULL, '6e33e411-ce07-4de3-9f47-7ac1476099e9', '2026-11-11 12:50:36.075955');
INSERT INTO `operation_log` VALUES (93, 10, '验收任务接续', 'STOCKTAKE_SAVE', 'STOCKTAKE', '7', 'PD20261111-bbb12005b86d4fda87b647b9', 'SUCCESS', NULL, NULL, NULL, '1f3e9b66-dbe2-419e-bcce-ed242c116709', '2026-11-11 12:51:00.674390');
INSERT INTO `operation_log` VALUES (94, 10, '验收任务接续', 'STOCKTAKE_PENDING', 'STOCKTAKE', '7', 'PD20261111-bbb12005b86d4fda87b647b9', 'SUCCESS', NULL, NULL, NULL, 'bff546c5-89ac-4738-a2db-d5809fef9607', '2026-11-11 12:51:01.105908');
INSERT INTO `operation_log` VALUES (95, 10, '验收任务接续', 'STOCKTAKE_COUNTING', 'STOCKTAKE', '7', 'PD20261111-bbb12005b86d4fda87b647b9', 'SUCCESS', NULL, NULL, NULL, '50501ea1-65cf-4554-909d-bdde2c3b8849', '2026-11-11 12:51:01.874487');
INSERT INTO `operation_log` VALUES (96, 1, '系统管理员', 'STOCKTAKE_CLOSED', 'STOCKTAKE', '7', 'PD20261111-bbb12005b86d4fda87b647b9', 'SUCCESS', '验收演示完成，管理员接续关闭未生效盘点，保留原库存。', NULL, NULL, '844c72c2-a03b-40f4-9f2b-b1fb031dcc0d', '2026-11-11 12:51:04.498340');
INSERT INTO `operation_log` VALUES (97, 1, '系统管理员', 'USER_STATUS', 'USER', '10', 'qa_buyer_1111', 'SUCCESS', '停用账号，原登录凭证已失效', '{\"id\": \"10\", \"enabled\": true, \"realName\": \"验收任务接续\", \"roleCode\": \"PURCHASER\", \"username\": \"qa_buyer_1111\", \"phoneMasked\": null}', '{\"id\": \"10\", \"enabled\": false, \"realName\": \"验收任务接续\", \"roleCode\": \"PURCHASER\", \"username\": \"qa_buyer_1111\", \"phoneMasked\": null}', '1848ee4d-5ed2-4012-8be7-719a5d0ad7c0', '2026-11-11 12:51:11.081261');
INSERT INTO `operation_log` VALUES (98, 1, '系统管理员', 'CATEGORY_CREATE', 'CATEGORY', '8', 'QA_TEMP1111', 'SUCCESS', NULL, NULL, NULL, 'db4df1a9-ef8b-4942-a758-d49b7813ecb1', '2026-11-11 12:51:39.372582');
INSERT INTO `operation_log` VALUES (99, 1, '系统管理员', 'CATEGORY_EDIT', 'CATEGORY', '8', 'QA_TEMP1111', 'SUCCESS', NULL, NULL, NULL, 'eed4517a-cbbe-497a-9ab3-ad75e3202523', '2026-11-11 12:51:41.128315');
INSERT INTO `operation_log` VALUES (100, 1, '系统管理员', 'CATEGORY_STATUS', 'CATEGORY', '8', 'QA_TEMP1111', 'SUCCESS', '停用', NULL, NULL, 'fcafdfe1-6edb-44fa-ac33-ae0ca88ac1a8', '2026-11-11 12:51:42.228396');
INSERT INTO `operation_log` VALUES (101, 1, '系统管理员', 'CATEGORY_STATUS', 'CATEGORY', '8', 'QA_TEMP1111', 'SUCCESS', '启用', NULL, NULL, '60588108-ed37-4228-904c-09317443a027', '2026-11-11 12:51:42.842415');
INSERT INTO `operation_log` VALUES (102, 1, '系统管理员', 'CATEGORY_DELETE', 'CATEGORY', '8', 'QA_TEMP1111', 'SUCCESS', NULL, NULL, NULL, '9c6f5cd6-baca-4b41-b9f5-2c6ac64d67e0', '2026-11-11 12:51:44.138469');
INSERT INTO `operation_log` VALUES (103, 1, '系统管理员', 'PRODUCT_CREATE', 'PRODUCT', '39', 'QA_ZERO1111', 'SUCCESS', NULL, NULL, NULL, '7f97cd8c-5307-4ab2-8428-737fdc327a54', '2026-11-11 12:52:46.127885');
INSERT INTO `operation_log` VALUES (104, 1, '系统管理员', 'PRODUCT_EDIT', 'PRODUCT', '39', 'QA_ZERO1111', 'SUCCESS', '档案编辑；历史快照保持原值', NULL, NULL, '813a785a-b281-4500-ab16-ab3e5eaaae32', '2026-11-11 12:52:47.507707');
INSERT INTO `operation_log` VALUES (105, 1, '系统管理员', 'PRODUCT_IMAGE_CHANGE', 'PRODUCT', '39', 'QA_ZERO1111', 'SUCCESS', '更新商品图片', NULL, NULL, '6d11b550-8035-49f0-a2db-e19bffb1f679', '2026-11-11 12:53:12.527690');
INSERT INTO `operation_log` VALUES (106, 1, '系统管理员', 'PRODUCT_STATUS_CHANGE', 'PRODUCT', '39', 'QA_ZERO1111', 'SUCCESS', '停用', NULL, NULL, '077362ea-ecfe-40c0-b7f3-04a93dddb0b5', '2026-11-11 12:53:14.372942');
INSERT INTO `operation_log` VALUES (107, 1, '系统管理员', 'PRODUCT_STATUS_CHANGE', 'PRODUCT', '39', 'QA_ZERO1111', 'SUCCESS', '启用', NULL, NULL, 'fca6b884-fd61-4950-be86-5a610476a438', '2026-11-11 12:53:14.852366');
INSERT INTO `operation_log` VALUES (108, 1, '系统管理员', 'PRODUCT_DELETE', 'PRODUCT', '39', 'QA_ZERO1111', 'SUCCESS', NULL, NULL, NULL, 'aaeb1910-0995-4b04-b840-616de211da44', '2026-11-11 12:53:16.270891');
INSERT INTO `operation_log` VALUES (109, 1, '系统管理员', 'AI_GENERATE_SUCCESS', 'AI', '11', 'c279ed63-5354-464f-939e-34cc385baf2a', 'SUCCESS', NULL, NULL, NULL, NULL, '2026-11-11 12:55:24.144082');
INSERT INTO `operation_log` VALUES (110, 6, '收银赵六', 'SALE_COMPLETE', 'SALE', '10', 'XS20261111-80a6d4e2d1ee4c6191169706', 'SUCCESS', NULL, NULL, NULL, 'b9667e3e-0c39-44a4-8cc4-5fae2561cd4e', '2026-11-11 12:57:48.633624');
INSERT INTO `operation_log` VALUES (111, 1, '系统管理员', 'SALE_CORRECT', 'SALE', '10', 'XS20261111-80a6d4e2d1ee4c6191169706', 'SUCCESS', '验收演示：断网查单流程完成，实物完整在店，整单撤销模拟错录。', NULL, NULL, 'adea0759-65bb-42d3-9a7c-db65c5e126b9', '2026-11-11 12:58:06.276876');
INSERT INTO `operation_log` VALUES (112, 6, '收银赵六', 'SALE_COMPLETE', 'SALE', '11', 'XS20261111-320744f63f284c629ac317d6', 'SUCCESS', NULL, NULL, NULL, '5732e0ef-3ec4-4ca7-be57-3ce2e5b7e8fd', '2026-11-11 12:59:04.260210');
INSERT INTO `operation_log` VALUES (113, 1, '系统管理员', 'SALE_CORRECT', 'SALE', '11', 'XS20261111-320744f63f284c629ac317d6', 'SUCCESS', '验收演示：两收银员并发验证后，实物完整，整单纠错恢复原批次。', NULL, NULL, '8defd0bd-fae7-4c64-b51b-138361656a36', '2026-11-11 12:59:51.252095');
INSERT INTO `operation_log` VALUES (114, 1, '系统管理员', 'PRODUCT_EDIT', 'PRODUCT', '30', 'HX_SOAP_100', 'SUCCESS', '档案编辑；历史快照保持原值', NULL, NULL, '92ad8e5b-2fcc-4195-9c43-fb234735ba3f', '2026-11-11 13:42:29.050942');
INSERT INTO `operation_log` VALUES (115, 5, '收银王五', 'SALE_COMPLETE', 'SALE', '12', 'XS20261111-89df136b8e2f400f945b4d8c', 'SUCCESS', NULL, NULL, NULL, '3799a941-ec79-49d1-a256-a2ceee4aa881', '2026-11-11 13:42:31.620940');
INSERT INTO `operation_log` VALUES (116, 1, '系统管理员', 'PRODUCT_EDIT', 'PRODUCT', '30', 'HX_SOAP_100', 'SUCCESS', '档案编辑；历史快照保持原值', NULL, NULL, '432d7824-18ef-4938-8baa-cbd1d28b6c52', '2026-11-11 13:42:32.193896');
INSERT INTO `operation_log` VALUES (117, 1, '系统管理员', 'SALE_CORRECT', 'SALE', '12', 'XS20261111-89df136b8e2f400f945b4d8c', 'SUCCESS', '验收演示：调价二次确认完成，实物完整，整单纠错恢复原库存。', NULL, NULL, '47c64ca3-86b0-4a67-a266-bfb284d1e7a9', '2026-11-11 13:42:33.665696');
INSERT INTO `operation_log` VALUES (118, 5, '收银王五', 'SALE_COMPLETE', 'SALE', '14', 'XS20261111-73d230811e114f149db42dde', 'SUCCESS', NULL, NULL, NULL, 'dfcbb707-b71d-46ce-b22a-8cf2542b0130', '2026-11-11 13:44:23.164902');
INSERT INTO `operation_log` VALUES (119, 1, '系统管理员', 'SALE_CORRECT', 'SALE', '14', 'XS20261111-73d230811e114f149db42dde', 'SUCCESS', '验收演示：事务故障撤除后同键成功，实物完整，整单纠错恢复库存。', NULL, NULL, '6f115740-dccf-4141-99b1-3291a1da28d8', '2026-11-11 13:44:24.687209');
INSERT INTO `operation_log` VALUES (120, 3, '采购张三', 'PURCHASE_CREATE', 'PURCHASE', '16', 'CG20261111-e50e2ce3d3554a11852c767b', 'SUCCESS', NULL, NULL, NULL, '9154a4f8-58c8-4e47-bc44-bc0263ecd12c', '2026-11-11 13:45:56.402154');
INSERT INTO `operation_log` VALUES (121, 3, '采购张三', 'PURCHASE_SUBMIT', 'PURCHASE', '16', 'CG20261111-e50e2ce3d3554a11852c767b', 'SUCCESS', NULL, NULL, NULL, 'd107cd24-cd1e-4b2f-8499-3a37ba3711ea', '2026-11-11 13:46:00.404535');
INSERT INTO `operation_log` VALUES (122, 1, '系统管理员', 'PURCHASE_REJECT', 'PURCHASE', '16', 'CG20261111-e50e2ce3d3554a11852c767b', 'SUCCESS', '验收演示：到货备注需经办人核实。', NULL, NULL, '22e50d7c-a9c1-4cbc-b9bf-a6d1d99ff52a', '2026-11-11 13:46:12.971921');
INSERT INTO `operation_log` VALUES (123, 3, '采购张三', 'PURCHASE_EDIT', 'PURCHASE', '16', 'CG20261111-e50e2ce3d3554a11852c767b', 'SUCCESS', NULL, NULL, NULL, '5d2366f1-1a43-44df-874c-2f4c9dbff99a', '2026-11-11 13:46:30.300038');
INSERT INTO `operation_log` VALUES (124, 3, '采购张三', 'PURCHASE_SUBMIT', 'PURCHASE', '16', 'CG20261111-e50e2ce3d3554a11852c767b', 'SUCCESS', NULL, NULL, NULL, '25b6b2c2-ebb9-4eca-8dc8-501b418f0636', '2026-11-11 13:46:38.233351');
INSERT INTO `operation_log` VALUES (125, 3, '采购张三', 'PURCHASE_WITHDRAW', 'PURCHASE', '16', 'CG20261111-e50e2ce3d3554a11852c767b', 'SUCCESS', NULL, NULL, NULL, '17dfc44e-6f6f-4c17-8ee0-3fa3428ba717', '2026-11-11 13:46:40.975367');
INSERT INTO `operation_log` VALUES (126, 3, '采购张三', 'PURCHASE_CLOSE', 'PURCHASE', '16', 'CG20261111-e50e2ce3d3554a11852c767b', 'SUCCESS', '验收演示完成，关闭未生效采购，保留业务历史。', NULL, NULL, '274d077d-cba8-4779-a874-1c9c7c5b92e6', '2026-11-11 13:46:42.275391');
INSERT INTO `operation_log` VALUES (127, 3, '采购张三', 'PURCHASE_CREATE', 'PURCHASE', '17', 'CG20261111-719878c34de940709cdff199', 'SUCCESS', NULL, NULL, NULL, '36080393-541f-461a-b5fc-36c7113e9051', '2026-11-11 13:46:54.977985');
INSERT INTO `operation_log` VALUES (128, 3, '采购张三', 'PURCHASE_SUBMIT', 'PURCHASE', '17', 'CG20261111-719878c34de940709cdff199', 'SUCCESS', NULL, NULL, NULL, 'c220ee9a-9d67-450c-b2ed-0186777a4a5b', '2026-11-11 13:47:17.159562');
INSERT INTO `operation_log` VALUES (129, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '17', 'CG20261111-719878c34de940709cdff199', 'SUCCESS', NULL, NULL, NULL, '692f653c-7400-4027-ab53-f4824cb2ec86', '2026-11-11 13:47:19.741573');
INSERT INTO `operation_log` VALUES (130, 3, '采购张三', 'RECONCILE_DIFFERENCE', 'PURCHASE', '17', 'CG20261111-719878c34de940709cdff199', 'SUCCESS', '验收演示：总额相同但数量与单价不同，应逐行判差异。', '{\"lines\": [{\"note\": null, \"itemId\": \"52\", \"externalQty\": null, \"externalUnitCost\": null}], \"status\": \"UNRECONCILED\", \"reference\": null, \"difference\": null, \"resolution\": null, \"externalTotal\": null}', '{\"lines\": [{\"note\": null, \"itemId\": \"52\", \"externalQty\": 3, \"externalUnitCost\": \"1.60\"}], \"status\": \"DIFFERENCE\", \"reference\": null, \"difference\": \"验收演示：总额相同但数量与单价不同，应逐行判差异。\", \"resolution\": null, \"externalTotal\": \"4.80\"}', '3e52d877-763b-4acd-b8ac-4ede1e26e697', '2026-11-11 13:47:43.147188');
INSERT INTO `operation_log` VALUES (131, 3, '采购张三', 'RECONCILE_MATCH', 'PURCHASE', '17', 'CG20261111-719878c34de940709cdff199', 'SUCCESS', '验收演示：已核对原始凭据，以四支每支1.20元修正。', '{\"lines\": [{\"note\": null, \"itemId\": \"52\", \"externalQty\": 3, \"externalUnitCost\": \"1.60\"}], \"status\": \"DIFFERENCE\", \"reference\": null, \"difference\": \"验收演示：总额相同但数量与单价不同，应逐行判差异。\", \"resolution\": null, \"externalTotal\": \"4.80\"}', '{\"lines\": [{\"note\": null, \"itemId\": \"52\", \"externalQty\": 4, \"externalUnitCost\": \"1.20\"}], \"status\": \"MATCHED\", \"reference\": null, \"difference\": \"验收演示：总额相同但数量与单价不同，应逐行判差异。\", \"resolution\": \"验收演示：已核对原始凭据，以四支每支1.20元修正。\", \"externalTotal\": \"4.80\"}', 'b2d7d9ab-b8c8-4b32-8056-1b63b1e43d58', '2026-11-11 13:47:44.468140');
INSERT INTO `operation_log` VALUES (132, 3, '采购张三', 'RECONCILE_WITHDRAW', 'PURCHASE', '17', 'CG20261111-719878c34de940709cdff199', 'SUCCESS', '验收演示：本单未用批次将撤销入库，先撤回核对。', '{\"lines\": [{\"note\": null, \"itemId\": \"52\", \"externalQty\": 4, \"externalUnitCost\": \"1.20\"}], \"status\": \"MATCHED\", \"reference\": null, \"difference\": \"验收演示：总额相同但数量与单价不同，应逐行判差异。\", \"resolution\": \"验收演示：已核对原始凭据，以四支每支1.20元修正。\", \"externalTotal\": \"4.80\"}', '{\"lines\": [{\"note\": null, \"itemId\": \"52\", \"externalQty\": null, \"externalUnitCost\": null}], \"status\": \"UNRECONCILED\", \"reference\": null, \"difference\": null, \"resolution\": null, \"externalTotal\": null}', 'c2820984-093c-48bd-9d0e-381ceea8a8cb', '2026-11-11 13:47:45.657990');
INSERT INTO `operation_log` VALUES (133, 1, '系统管理员', 'PURCHASE_REVERSE', 'PURCHASE', '17', 'CG20261111-719878c34de940709cdff199', 'SUCCESS', '验收演示结束，本批未消耗、已撤回核对，整单撤销。', NULL, NULL, '6840524f-b060-4868-b39e-d6bd72820566', '2026-11-11 13:47:47.070516');
INSERT INTO `operation_log` VALUES (134, 1, '系统管理员', 'RECONCILE_WITHDRAW', 'PURCHASE', '11', 'CG20261111-bb6deb4160294acd8a2912e8', 'SUCCESS', '验收演示：核实已销售来源的撤销保护，之后重新核对。', '{\"lines\": [{\"note\": null, \"itemId\": \"35\", \"externalQty\": 10, \"externalUnitCost\": \"3.00\"}, {\"note\": null, \"itemId\": \"36\", \"externalQty\": 10, \"externalUnitCost\": \"3.00\"}], \"status\": \"MATCHED\", \"reference\": \"HX-20261111-001\", \"difference\": null, \"resolution\": null, \"externalTotal\": \"60.00\"}', '{\"lines\": [{\"note\": null, \"itemId\": \"35\", \"externalQty\": null, \"externalUnitCost\": null}, {\"note\": null, \"itemId\": \"36\", \"externalQty\": null, \"externalUnitCost\": null}], \"status\": \"UNRECONCILED\", \"reference\": null, \"difference\": null, \"resolution\": null, \"externalTotal\": null}', 'd25c7886-4dac-43f7-85ce-f212bd4b246a', '2026-11-11 13:48:04.096679');
INSERT INTO `operation_log` VALUES (135, 1, '系统管理员', 'RECONCILE_MATCH', 'PURCHASE', '11', 'CG20261111-bb6deb4160294acd8a2912e8', 'SUCCESS', NULL, '{\"lines\": [{\"note\": null, \"itemId\": \"35\", \"externalQty\": null, \"externalUnitCost\": null}, {\"note\": null, \"itemId\": \"36\", \"externalQty\": null, \"externalUnitCost\": null}], \"status\": \"UNRECONCILED\", \"reference\": null, \"difference\": null, \"resolution\": null, \"externalTotal\": null}', '{\"lines\": [{\"note\": null, \"itemId\": \"35\", \"externalQty\": 10, \"externalUnitCost\": \"3.00\"}, {\"note\": null, \"itemId\": \"36\", \"externalQty\": 10, \"externalUnitCost\": \"3.00\"}], \"status\": \"MATCHED\", \"reference\": \"HX-20261111-001\", \"difference\": null, \"resolution\": null, \"externalTotal\": \"60.00\"}', '898b6b87-9ee2-4c71-b03f-e68faa574cca', '2026-11-11 13:48:06.047743');
INSERT INTO `operation_log` VALUES (136, 1, '系统管理员', 'CONFIG_UPDATE', 'CONFIG', 'inventory.defaults', 'inventory.defaults', 'SUCCESS', NULL, '{\"nearExpiryDays\": 7, \"stockLowerLimit\": 5}', '{\"nearExpiryDays\": 0, \"stockLowerLimit\": 0}', '1ef77f93-d115-4f3b-9407-17bbb0765c82', '2026-11-11 13:50:16.918582');
INSERT INTO `operation_log` VALUES (137, 1, '系统管理员', 'CONFIG_UPDATE', 'CONFIG', 'inventory.defaults', 'inventory.defaults', 'SUCCESS', NULL, '{\"nearExpiryDays\": 0, \"stockLowerLimit\": 0}', '{\"nearExpiryDays\": 7, \"stockLowerLimit\": 5}', '02af8aa9-c30e-48c1-a3e6-e47c138e4d0b', '2026-11-11 13:50:19.159008');
INSERT INTO `operation_log` VALUES (138, 6, '收银赵六', 'SALE_COMPLETE', 'SALE', '15', 'XS20261111-126ef997a16444b8ae200630', 'SUCCESS', NULL, NULL, NULL, '178d114b-2b44-4b76-8913-9ba3c7cc42b0', '2026-11-11 13:53:26.325405');
INSERT INTO `operation_log` VALUES (139, 1, '系统管理员', 'SALE_CORRECT', 'SALE', '15', 'XS20261111-126ef997a16444b8ae200630', 'SUCCESS', '验收演示：验证缺货整单失败后，实物完整，纠错恢复矿泉水。', NULL, NULL, 'bee4f438-0fa5-4887-b910-a1edc40a6345', '2026-11-11 13:53:55.247648');
INSERT INTO `operation_log` VALUES (140, 1, '系统管理员', 'STOCKTAKE_START', 'STOCKTAKE', '8', 'PD20261111-225ccd1bd48a47be81c0dc68', 'SUCCESS', NULL, NULL, NULL, '87620ada-b6b2-4653-bf51-fc55bf80b13a', '2026-11-11 13:53:57.287334');
INSERT INTO `operation_log` VALUES (141, 1, '系统管理员', 'STOCKTAKE_SAVE', 'STOCKTAKE', '8', 'PD20261111-225ccd1bd48a47be81c0dc68', 'SUCCESS', NULL, NULL, NULL, '2cc5203c-6e97-402c-826f-3cdbb01b647a', '2026-11-11 13:53:58.039706');
INSERT INTO `operation_log` VALUES (142, 1, '系统管理员', 'STOCKTAKE_PENDING', 'STOCKTAKE', '8', 'PD20261111-225ccd1bd48a47be81c0dc68', 'SUCCESS', NULL, NULL, NULL, 'e22dc0e0-b029-4cd6-a397-33dd4b59f91e', '2026-11-11 13:53:58.494711');
INSERT INTO `operation_log` VALUES (143, 1, '系统管理员', 'STOCKTAKE_COMPLETED', 'STOCKTAKE', '8', 'PD20261111-225ccd1bd48a47be81c0dc68', 'SUCCESS', NULL, NULL, NULL, '5b8e3bcd-d980-4a1f-ac59-463efe98d138', '2026-11-11 13:53:59.351340');
INSERT INTO `operation_log` VALUES (144, 1, '系统管理员', 'PURCHASE_CREATE', 'PURCHASE', '18', 'CG20261111-0a90e6b9f2bb4ffb8c342970', 'SUCCESS', NULL, NULL, NULL, 'e48ca8cd-df7c-4865-b7b9-a39b9862bb21', '2026-11-11 13:54:24.896117');
INSERT INTO `operation_log` VALUES (145, 1, '系统管理员', 'PURCHASE_SUBMIT', 'PURCHASE', '18', 'CG20261111-0a90e6b9f2bb4ffb8c342970', 'SUCCESS', NULL, NULL, NULL, 'f9722054-7bde-405c-baba-c917fe2421a2', '2026-11-11 13:54:25.327403');
INSERT INTO `operation_log` VALUES (146, 5, '收银王五', 'SALE_COMPLETE', 'SALE', '16', 'XS20261111-6d83d65961f04f1cad9ed9b1', 'SUCCESS', NULL, NULL, NULL, 'e307e0fb-8ea6-4241-9885-d6c1ba2bb05d', '2026-11-11 13:54:27.354114');
INSERT INTO `operation_log` VALUES (147, 1, '系统管理员', 'STOCKTAKE_START', 'STOCKTAKE', '9', 'PD20261111-e5e0c2ec2803459c94fb0cc3', 'SUCCESS', NULL, NULL, NULL, 'f6d197d3-3d62-476e-8ac3-d543bb0ac64d', '2026-11-11 13:54:28.514356');
INSERT INTO `operation_log` VALUES (148, 1, '系统管理员', 'STOCKTAKE_CLOSED', 'STOCKTAKE', '9', 'PD20261111-e5e0c2ec2803459c94fb0cc3', 'SUCCESS', '验收演示：冻结限制测试完成，关闭未填写盘点，不调整库存。', NULL, NULL, '13d78f4f-c8c6-4c09-a843-4661897e7ea5', '2026-11-11 13:54:59.973673');
INSERT INTO `operation_log` VALUES (149, 1, '系统管理员', 'PURCHASE_APPROVE', 'PURCHASE', '18', 'CG20261111-0a90e6b9f2bb4ffb8c342970', 'SUCCESS', NULL, NULL, NULL, '8a191344-d67c-4e5a-b1f8-dd8f15bbdc84', '2026-11-11 13:55:00.539733');
INSERT INTO `operation_log` VALUES (150, 1, '系统管理员', 'PURCHASE_REVERSE', 'PURCHASE', '18', 'CG20261111-0a90e6b9f2bb4ffb8c342970', 'SUCCESS', '验收演示结束，冻结释放后验证入库正常，本批未使用撤销。', NULL, NULL, 'ef409cc1-56ce-4949-838d-2687624b4154', '2026-11-11 13:55:01.212746');
INSERT INTO `operation_log` VALUES (151, 1, '系统管理员', 'SALE_CORRECT', 'SALE', '16', 'XS20261111-6d83d65961f04f1cad9ed9b1', 'SUCCESS', '验收演示：冻结释放后恢复原销售，实物完整在店。', NULL, NULL, '845afca8-9944-4a9e-8131-106a4e16ba6b', '2026-11-11 13:55:01.929597');
INSERT INTO `operation_log` VALUES (152, 1, '系统管理员', 'USER_STATUS', 'USER', '2', 'admin2', 'SUCCESS', '停用账号，原登录凭证已失效', '{\"id\": \"2\", \"enabled\": true, \"realName\": \"备用管理员\", \"roleCode\": \"ADMIN\", \"username\": \"admin2\", \"phoneMasked\": \"138****0002\"}', '{\"id\": \"2\", \"enabled\": false, \"realName\": \"备用管理员\", \"roleCode\": \"ADMIN\", \"username\": \"admin2\", \"phoneMasked\": \"138****0002\"}', 'cada584f-f924-4234-9fdf-95055ae7a557', '2026-11-11 13:57:49.758434');
INSERT INTO `operation_log` VALUES (153, 1, '系统管理员', 'USER_STATUS', 'USER', '2', 'admin2', 'SUCCESS', '启用账号', '{\"id\": \"2\", \"enabled\": false, \"realName\": \"备用管理员\", \"roleCode\": \"ADMIN\", \"username\": \"admin2\", \"phoneMasked\": \"138****0002\"}', '{\"id\": \"2\", \"enabled\": true, \"realName\": \"备用管理员\", \"roleCode\": \"ADMIN\", \"username\": \"admin2\", \"phoneMasked\": \"138****0002\"}', 'b367734b-f26d-4a5f-99e6-482e5f231d6d', '2026-11-11 13:57:50.508459');
INSERT INTO `operation_log` VALUES (154, 1, '系统管理员', 'AI_GENERATE_SUCCESS', 'AI', '12', '09245504-cae9-4945-a2b0-4b3293fec395', 'SUCCESS', NULL, NULL, NULL, NULL, '2026-11-11 14:09:10.776547');
INSERT INTO `operation_log` VALUES (155, 1, '系统管理员', 'PURCHASE_CREATE', 'PURCHASE', '19', 'CG20261111-480289fc2dbe46878e93218a', 'SUCCESS', NULL, NULL, NULL, 'c5f1ec5c-c9a5-41ae-9db0-93ec917b1f1f', '2026-11-11 14:12:18.374140');
INSERT INTO `operation_log` VALUES (156, 3, '采购张三', 'STOCKTAKE_START', 'STOCKTAKE', '10', 'PD20261111-0c693d0066ba4cdfadf403a8', 'SUCCESS', NULL, NULL, NULL, '581a24e1-1f7e-41a0-9ecd-9848c8811cde', '2026-11-11 14:19:57.443400');
INSERT INTO `operation_log` VALUES (157, 3, '采购张三', 'STOCKTAKE_CLOSED', 'STOCKTAKE', '10', 'PD20261111-0c693d0066ba4cdfadf403a8', 'SUCCESS', '验收演示：丢失成功响应查单完成，关闭盘点释放冻结，库存不变。', NULL, NULL, 'dbf5524a-a83e-49f3-a379-a6cc37d1f4a2', '2026-11-11 14:20:19.995990');
INSERT INTO `operation_log` VALUES (158, 1, '系统管理员', 'PURCHASE_DELETE_DRAFT', 'PURCHASE', '19', 'CG20261111-480289fc2dbe46878e93218a', 'SUCCESS', NULL, NULL, NULL, '67a6f265-c1f3-4f54-bdf5-c87e7c861892', '2026-11-11 14:20:21.213214');
INSERT INTO `operation_log` VALUES (159, 1, '系统管理员', 'SUPPLIER_CREATE', 'SUPPLIER', '12', 'SUP50619589638b44e9b71def38', 'SUCCESS', NULL, NULL, NULL, '2723a389-27d7-41c3-bc94-ca0f182a4329', '2026-11-11 14:20:42.115821');
INSERT INTO `operation_log` VALUES (160, 1, '系统管理员', 'SUPPLIER_DELETE', 'SUPPLIER', '12', 'SUP50619589638b44e9b71def38', 'SUCCESS', NULL, NULL, NULL, 'c8e5d23a-09ea-4910-aeca-9083e80f95d6', '2026-11-11 14:20:44.235576');
INSERT INTO `operation_log` VALUES (161, 1, '系统管理员', 'PRODUCT_EDIT', 'PRODUCT', '30', 'HX_SOAP_100', 'SUCCESS', '档案编辑；历史快照保持原值', NULL, NULL, '2fd0d708-151b-40c8-adf3-046bb58d68e5', '2026-11-11 14:21:09.745678');
INSERT INTO `operation_log` VALUES (162, 1, '系统管理员', 'PRODUCT_EDIT', 'PRODUCT', '30', 'HX_SOAP_100', 'SUCCESS', '档案编辑；历史快照保持原值', NULL, NULL, 'c27b8473-a623-4504-9049-c133e9d89327', '2026-11-11 14:21:58.172012');
INSERT INTO `operation_log` VALUES (163, 1, '系统管理员', 'ROLE_SAVE', 'ROLE', 'ADMIN', 'ADMIN', 'SUCCESS', '岗位授权版本已更新，相关账号原登录凭证失效', '{\"role\": \"ADMIN\", \"permissions\": [\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"system\", \"system.user\", \"system.role\", \"system.config\", \"system.log\", \"system.user.create\", \"system.user.edit\", \"system.user.enable\", \"system.user.reset_password\", \"system.user.delete\", \"system.role.save\", \"system.role.restore\", \"system.config.save\", \"base\", \"base.category\", \"base.category.create\", \"base.category.edit\", \"base.category.enable\", \"base.category.delete\", \"base.product\", \"base.product.create\", \"base.product.edit\", \"base.product.enable\", \"base.product.delete\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"base.supplier.delete\", \"base.product.image\", \"product.purchase_lookup\", \"product.sale_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"purchase.order.review\", \"purchase.order.reject\", \"purchase.order.reverse\", \"sale\", \"sale.order\", \"sale.order.complete\", \"sale.order.correct\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"inventory.stocktake.review\", \"inventory.stocktake.reject\", \"statistics\", \"statistics.sales\", \"statistics.inventory\", \"ai\", \"ai.inventory\", \"ai.sales\"]}', '{\"role\": \"ADMIN\", \"permissions\": [\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"system\", \"system.user\", \"system.role\", \"system.config\", \"system.log\", \"system.user.create\", \"system.user.edit\", \"system.user.enable\", \"system.user.reset_password\", \"system.user.delete\", \"system.role.save\", \"system.role.restore\", \"system.config.save\", \"base\", \"base.category\", \"base.category.create\", \"base.category.edit\", \"base.category.enable\", \"base.category.delete\", \"base.product\", \"base.product.create\", \"base.product.edit\", \"base.product.enable\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"base.supplier.delete\", \"base.product.image\", \"product.purchase_lookup\", \"product.sale_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"purchase.order.review\", \"purchase.order.reject\", \"purchase.order.reverse\", \"sale\", \"sale.order\", \"sale.order.complete\", \"sale.order.correct\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"inventory.stocktake.review\", \"inventory.stocktake.reject\", \"statistics\", \"statistics.sales\", \"statistics.inventory\", \"ai\", \"ai.inventory\", \"ai.sales\"]}', '6fe9eac1-c434-4e66-a296-4f04c115d19d', '2026-11-11 14:22:29.526595');
INSERT INTO `operation_log` VALUES (164, 1, '系统管理员', 'ROLE_RESTORE', 'ROLE', 'ADMIN', 'ADMIN', 'SUCCESS', '岗位授权版本已更新，相关账号原登录凭证失效', '{\"role\": \"ADMIN\", \"permissions\": [\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"system\", \"system.user\", \"system.role\", \"system.config\", \"system.log\", \"system.user.create\", \"system.user.edit\", \"system.user.enable\", \"system.user.reset_password\", \"system.user.delete\", \"system.role.save\", \"system.role.restore\", \"system.config.save\", \"base\", \"base.category\", \"base.category.create\", \"base.category.edit\", \"base.category.enable\", \"base.category.delete\", \"base.product\", \"base.product.create\", \"base.product.edit\", \"base.product.enable\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"base.supplier.delete\", \"base.product.image\", \"product.purchase_lookup\", \"product.sale_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"purchase.order.review\", \"purchase.order.reject\", \"purchase.order.reverse\", \"sale\", \"sale.order\", \"sale.order.complete\", \"sale.order.correct\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"inventory.stocktake.review\", \"inventory.stocktake.reject\", \"statistics\", \"statistics.sales\", \"statistics.inventory\", \"ai\", \"ai.inventory\", \"ai.sales\"]}', '{\"role\": \"ADMIN\", \"permissions\": [\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"system\", \"system.user\", \"system.role\", \"system.config\", \"system.log\", \"system.user.create\", \"system.user.edit\", \"system.user.enable\", \"system.user.reset_password\", \"system.user.delete\", \"system.role.save\", \"system.role.restore\", \"system.config.save\", \"base\", \"base.category\", \"base.category.create\", \"base.category.edit\", \"base.category.enable\", \"base.category.delete\", \"base.product\", \"base.product.create\", \"base.product.edit\", \"base.product.enable\", \"base.product.delete\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"base.supplier.delete\", \"base.product.image\", \"product.purchase_lookup\", \"product.sale_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"purchase.order.review\", \"purchase.order.reject\", \"purchase.order.reverse\", \"sale\", \"sale.order\", \"sale.order.complete\", \"sale.order.correct\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"inventory.stocktake.review\", \"inventory.stocktake.reject\", \"statistics\", \"statistics.sales\", \"statistics.inventory\", \"ai\", \"ai.inventory\", \"ai.sales\"]}', '70e0d514-ca7f-4e93-8fab-1482579ec4ec', '2026-11-11 14:22:32.424321');
INSERT INTO `operation_log` VALUES (165, 1, '系统管理员', 'PRODUCT_STATUS_CHANGE', 'PRODUCT', '30', 'HX_SOAP_100', 'SUCCESS', '停用', NULL, NULL, '6c1a6057-7a31-4f6e-bf06-a3f4dd42d15d', '2026-11-11 14:23:34.392985');
INSERT INTO `operation_log` VALUES (166, 1, '系统管理员', 'PRODUCT_STATUS_CHANGE', 'PRODUCT', '30', 'HX_SOAP_100', 'SUCCESS', '启用', NULL, NULL, '3db3de58-6a27-496e-936a-c42f26bf4a21', '2026-11-11 14:23:48.444785');
INSERT INTO `operation_log` VALUES (167, 1, '系统管理员', 'STOCKTAKE_START', 'STOCKTAKE', '11', 'PD20261111-3c4d38e55b32426d8e2e6c22', 'SUCCESS', NULL, NULL, NULL, '13649105-f198-4fad-b2f2-3a8f6c1ae8d7', '2026-11-11 14:25:24.558950');
INSERT INTO `operation_log` VALUES (168, 1, '系统管理员', 'STOCKTAKE_SAVE', 'STOCKTAKE', '11', 'PD20261111-3c4d38e55b32426d8e2e6c22', 'SUCCESS', NULL, NULL, NULL, 'aa5e992e-80a0-416b-be8b-a71151dc4d93', '2026-11-11 14:25:34.681946');
INSERT INTO `operation_log` VALUES (169, 1, '系统管理员', 'STOCKTAKE_PENDING', 'STOCKTAKE', '11', 'PD20261111-3c4d38e55b32426d8e2e6c22', 'SUCCESS', NULL, NULL, NULL, 'c93fb712-d747-46c3-baa7-b7021c6c5f4d', '2026-11-11 14:25:38.714656');
INSERT INTO `operation_log` VALUES (170, 1, '系统管理员', 'STOCKTAKE_COMPLETED', 'STOCKTAKE', '11', 'PD20261111-3c4d38e55b32426d8e2e6c22', 'SUCCESS', NULL, NULL, NULL, '48024902-6051-4f57-aaec-3543202c2ee5', '2026-11-11 14:25:44.749351');
INSERT INTO `operation_log` VALUES (171, 1, '系统管理员', 'STOCKTAKE_START', 'STOCKTAKE', '12', 'PD20261111-4f09ceceb2284b4c906dd3b3', 'SUCCESS', NULL, NULL, NULL, '533c1b8b-12d2-4c7c-baef-9881636cb44b', '2026-11-11 14:26:08.443406');
INSERT INTO `operation_log` VALUES (172, 1, '系统管理员', 'STOCKTAKE_SAVE', 'STOCKTAKE', '12', 'PD20261111-4f09ceceb2284b4c906dd3b3', 'SUCCESS', NULL, NULL, NULL, 'b4f97042-734a-40d2-a683-310a2fc5c196', '2026-11-11 14:26:58.849295');
INSERT INTO `operation_log` VALUES (173, 1, '系统管理员', 'STOCKTAKE_PENDING', 'STOCKTAKE', '12', 'PD20261111-4f09ceceb2284b4c906dd3b3', 'SUCCESS', NULL, NULL, NULL, 'e401ccf7-fda0-4ce7-b865-c778f8bded44', '2026-11-11 14:27:00.401386');
INSERT INTO `operation_log` VALUES (174, 1, '系统管理员', 'STOCKTAKE_COMPLETED', 'STOCKTAKE', '12', 'PD20261111-4f09ceceb2284b4c906dd3b3', 'SUCCESS', NULL, NULL, NULL, 'fe4fd400-b02a-4d6b-9c58-c42b97c90b7c', '2026-11-11 14:27:01.434223');
INSERT INTO `operation_log` VALUES (175, 1, '系统管理员', 'AI_CONFIG_UPDATE', 'CONFIG', 'ai.provider_settings', 'ai.provider_settings', 'SUCCESS', NULL, '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 4, \"keySource\": \"MANAGED\", \"defaultModel\": \"qwen-plus\", \"keyConfigured\": true}', '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 5, \"keySource\": \"LOCAL\", \"defaultModel\": \"qwen-plus\", \"keyConfigured\": true}', 'ba3f4cce-6514-4756-8d4f-0c763bbaf409', '2026-11-11 14:28:21.090234');
INSERT INTO `operation_log` VALUES (176, 1, '系统管理员', 'AI_CONFIG_UPDATE', 'CONFIG', 'ai.provider_settings', 'ai.provider_settings', 'SUCCESS', NULL, '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 5, \"keySource\": \"LOCAL\", \"defaultModel\": \"qwen-plus\", \"keyConfigured\": true}', '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 6, \"keySource\": \"MANAGED\", \"defaultModel\": \"qwen-plus\", \"keyConfigured\": true}', 'ea707698-d53f-4914-8d0d-7a0d239d8344', '2026-11-11 14:28:35.312461');
INSERT INTO `operation_log` VALUES (177, 1, '系统管理员', 'PRODUCT_IMAGE_CHANGE', 'PRODUCT', '36', 'HX_DETERGENT_1KG', 'SUCCESS', '更新商品图片', NULL, NULL, 'c5804551-03a0-480a-9071-010e1027565e', '2026-11-11 16:57:15.262374');
INSERT INTO `operation_log` VALUES (178, 1, '系统管理员', 'AI_GENERATE_SUCCESS', 'AI', '13', 'd17bfbc3-da8b-4390-a2fe-a8e0e1731400', 'SUCCESS', NULL, NULL, NULL, NULL, '2026-11-11 16:59:04.207147');

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
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品档案、库存余额及唯一活动冻结；不单建库存余额和冻结表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (1, 'P0001', '6900000000001', '演示瓶装饮料', 1, '基本单位包装', '瓶', 3.00, 5.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 5, 1, 1, 1, 5, NULL, 6, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (2, 'P0002', '6900000000002', '纯牛奶', 1, '基本单位包装', '件', 4.00, 6.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 10, 1, 1, 1, 38, NULL, 2, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (3, 'P0003', '6900000000003', '临期酸奶', 1, '基本单位包装', '件', 5.00, 8.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 2, 1, 1, 1, 12, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (4, 'P0004', '6900000000004', '过期面包', 5, '基本单位包装', '件', 2.00, 5.00, '测试图片.jpg', '{\"fixture\": true}', 1, 180, 7, 5, 1, 1, 1, 0, NULL, 4, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-11 13:53:59.349796');
INSERT INTO `product` VALUES (5, 'P0005', '6900000000005', '大米五公斤', 2, '基本单位包装', '件', 48.00, 68.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 5, 1, 1, 1, 45, NULL, 3, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (6, 'P0006', '6900000000006', '滞销食用油', 2, '基本单位包装', '件', 40.00, 58.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 3, 1, 1, 1, 30, NULL, 1, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (7, 'P0007', '6900000000007', '抽纸三包装', 3, '基本单位包装', '件', 8.00, 12.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 5, 1, 1, 1, 40, NULL, 7, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (8, 'P0008', '6900000000008', '香皂', 3, '基本单位包装', '件', 3.50, 6.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 5, 1, 1, 1, 30, NULL, 3, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-10 16:13:25.806904');
INSERT INTO `product` VALUES (9, 'P0009', '6900000000009', '并发测试矿泉水', 1, '基本单位包装', '瓶', 1.00, 2.00, '测试图片.jpg', '{\"fixture\": true}', 0, NULL, 7, 5, 1, 1, 1, 5, NULL, 5, '见README测试场景映射', 1, '2026-09-01 00:00:00.000000', '2026-11-11 13:53:55.244856');
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
INSERT INTO `product` VALUES (25, 'HX_DRINK_500', '6902026111101', '禾序瓶装饮料500mL', 1, '500mL/瓶', '瓶', 3.00, 5.00, NULL, NULL, 1, 180, 7, 5, 1, 1, 1, 15, NULL, 9, NULL, 1, '2026-11-11 12:04:36.475644', '2026-11-11 12:40:12.622140');
INSERT INTO `product` VALUES (26, 'HX_MILK_250', '6902026111102', '晨光鲜牛奶250mL', 1, '250mL/盒', '盒', 3.20, 4.50, NULL, NULL, 1, 7, 2, 10, 1, 1, 1, 30, NULL, 2, NULL, 1, '2026-11-11 12:05:49.191206', '2026-11-11 12:41:10.084795');
INSERT INTO `product` VALUES (27, 'HX_BREAD_80', '6902026111103', '手作软面包80g', 5, '80g/袋', '袋', 3.50, 5.50, NULL, NULL, 1, 5, 2, 6, 1, 1, 1, 12, NULL, 1, NULL, 1, '2026-11-11 12:06:07.383091', '2026-11-11 12:41:10.085601');
INSERT INTO `product` VALUES (28, 'HX_RICE_5KG', '6902026111104', '东北珍珠米5kg', 2, '5kg/袋', '袋', 39.00, 49.90, NULL, NULL, 1, 365, 30, 5, 1, 1, 1, 30, NULL, 1, NULL, 1, '2026-11-11 12:06:25.553425', '2026-11-11 12:41:10.517846');
INSERT INTO `product` VALUES (29, 'HX_TISSUE_3P', '6902026111105', '柔韧抽纸三包装', 3, '100抽×3包', '件', 7.50, 12.00, NULL, NULL, 0, NULL, 0, 8, 1, 1, 1, 24, NULL, 1, NULL, 1, '2026-11-11 12:06:37.706587', '2026-11-11 12:41:10.715403');
INSERT INTO `product` VALUES (30, 'HX_SOAP_100', '6902026111106', '清洁香皂100g', 3, '100g/块', '件', 3.80, 6.50, NULL, NULL, 0, NULL, 0, 8, 1, 1, 1, 36, NULL, 5, NULL, 1, '2026-11-11 12:07:20.176986', '2026-11-11 14:23:48.444838');
INSERT INTO `product` VALUES (31, 'HX_NOODLE_5P', '6902026111107', '香菇炖鸡面五连包', 2, '五连包', '袋', 9.80, 14.50, NULL, NULL, 1, 180, 15, 10, 1, 1, 1, 40, NULL, 1, NULL, 1, '2026-11-11 12:07:38.334909', '2026-11-11 12:41:10.519315');
INSERT INTO `product` VALUES (32, 'HX_BISCUIT_100', '6902026111108', '苏打饼干100g', 5, '100g/袋', '袋', 4.20, 6.90, NULL, NULL, 1, 270, 15, 8, 1, 1, 1, 24, NULL, 1, NULL, 1, '2026-11-11 12:07:56.483249', '2026-11-11 12:41:10.520370');
INSERT INTO `product` VALUES (33, 'HX_WATER_550', '6902026111109', '天然饮用水550mL', 1, '550mL/瓶', '瓶', 0.90, 2.00, NULL, NULL, 1, 365, 10, 10, 1, 1, 1, 96, NULL, 1, NULL, 1, '2026-11-11 12:08:14.708932', '2026-11-11 12:41:10.717327');
INSERT INTO `product` VALUES (34, 'HX_PEN_05', '6902026111110', '顺滑中性笔0.5mm', 4, '黑色0.5mm', '支', 1.20, 2.50, NULL, NULL, 0, NULL, 0, 12, 1, 1, 1, 60, NULL, 13, NULL, 1, '2026-11-11 12:10:59.497768', '2026-11-11 13:55:01.927076');
INSERT INTO `product` VALUES (35, 'HX_NOTE_A5', '6902026111111', 'A5横线笔记本', 4, 'A5/60页', '本', 2.60, 5.00, NULL, NULL, 0, NULL, 0, 8, 1, 1, 1, 30, NULL, 11, NULL, 1, '2026-11-11 12:11:15.645800', '2026-11-11 14:27:01.433719');
INSERT INTO `product` VALUES (36, 'HX_DETERGENT_1KG', '6902026111112', '清新洗衣液1kg', 3, '1kg/瓶', '瓶', 12.80, 19.90, '5c696457-8470-4241-a33e-d05b70985140.png', '{\"mime\": \"image/png\", \"size\": 12681, \"width\": 190, \"height\": 153, \"uploadedAt\": \"2026-11-11T08:57:15.256366200Z\"}', 0, NULL, 0, 5, 1, 1, 1, 24, NULL, 1, NULL, 1, '2026-11-11 12:11:31.770405', '2026-11-11 16:57:15.262091');

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
) ENGINE = InnoDB AUTO_INCREMENT = 55 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购明细与其唯一库存批次合并，业务追溯仍使用真实外键' ROW_FORMAT = Dynamic;

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
INSERT INTO `purchase_item` VALUES (19, 2, 1, 4, 'P0004', '6900000000004', '过期面包', 5, '糕点零食', '基本单位包装', '件', 1, 6, 6, 2.00, DEFAULT, 'SUPLOT-0002-1', '2026-08-22', '2026-11-09', 'BATCH-00019', 0, NULL, NULL, DEFAULT, NULL, '2026-10-26 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (20, 2, 2, 20, 'P0020', '6900000000020', '过期已清零食品', 5, '糕点零食', '基本单位包装', '件', 1, 5, 5, 5.00, DEFAULT, 'SUPLOT-0002-2', '2026-08-22', '2026-11-08', 'BATCH-00020', 0, NULL, NULL, DEFAULT, NULL, '2026-10-26 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (21, 3, 1, 17, 'P0017', '6900000000017', '新近入库商品', 4, '文具用品', '基本单位包装', '件', 0, 20, 20, 4.00, DEFAULT, 'SUPLOT-0003-1', NULL, NULL, 'BATCH-00021', 20, 21, 4.00, DEFAULT, '凭据数量多一件，待核查', '2026-11-09 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (22, 4, 1, 7, 'P0007', '6900000000007', '抽纸三包装', 3, '日用清洁', '基本单位包装', '件', 0, 4, 4, 8.00, DEFAULT, 'SUPLOT-0004-1', NULL, NULL, 'BATCH-00022', 0, NULL, NULL, DEFAULT, NULL, '2026-11-07 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (23, 5, 1, 8, 'P0008', '6900000000008', '香皂', 3, '日用清洁', '基本单位包装', '件', 0, 3, 3, 3.50, DEFAULT, 'SUPLOT-0005-1', NULL, NULL, 'BATCH-00023', 3, NULL, NULL, DEFAULT, NULL, '2026-11-08 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (24, 6, 1, 21, 'P0021', '6900000000021', '待审核采购商品', 4, '文具用品', '基本单位包装', '件', 0, 10, 10, 2.00, DEFAULT, 'SUPLOT-0006-1', NULL, NULL, NULL, NULL, NULL, NULL, DEFAULT, NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (25, 7, 1, 24, 'P0024', '6900000000024', '缺日期驳回食品', 5, '糕点零食', '基本单位包装', '件', 1, 8, 8, 5.00, DEFAULT, 'SUPLOT-0007-1', NULL, NULL, NULL, NULL, NULL, NULL, DEFAULT, NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (26, 8, 1, 21, 'P0021', '6900000000021', '待审核采购商品', 4, '文具用品', '基本单位包装', '件', 0, 2, NULL, 2.00, DEFAULT, 'SUPLOT-0008-1', NULL, NULL, NULL, NULL, NULL, NULL, DEFAULT, NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (27, 9, 1, 21, 'P0021', '6900000000021', '待审核采购商品', 4, '文具用品', '基本单位包装', '件', 0, 3, 3, 2.00, DEFAULT, 'SUPLOT-0009-1', NULL, NULL, NULL, NULL, NULL, NULL, DEFAULT, NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (28, 10, 1, 5, 'P0005', '6900000000005', '大米五公斤', 2, '粮油食品', '基本单位包装', '件', 0, 6, NULL, 48.00, DEFAULT, 'SUPLOT-0010-1', NULL, NULL, NULL, NULL, NULL, NULL, DEFAULT, NULL, '2026-11-10 16:13:25.806904');
INSERT INTO `purchase_item` VALUES (35, 11, 1, 25, 'HX_DRINK_500', '6902026111101', '禾序瓶装饮料500mL', 1, '饮料乳品', '500mL/瓶', '瓶', 1, 10, 10, 3.00, DEFAULT, 'HX-DRINK-NEAR-1113', '2026-05-17', '2026-11-13', 'PC20261111-1f952d52213943da8b454291', 0, 10, 3.00, DEFAULT, NULL, '2026-11-11 12:27:19.830754');
INSERT INTO `purchase_item` VALUES (36, 11, 2, 25, 'HX_DRINK_500', '6902026111101', '禾序瓶装饮料500mL', 1, '饮料乳品', '500mL/瓶', '瓶', 1, 10, 10, 3.00, DEFAULT, 'HX-DRINK-FRESH-1201', '2026-06-04', '2026-12-01', 'PC20261111-2ca5930de9a248aeaead7951', 5, 10, 3.00, DEFAULT, NULL, '2026-11-11 12:27:19.830754');
INSERT INTO `purchase_item` VALUES (37, 12, 1, 25, 'HX_DRINK_500', '6902026111101', '禾序瓶装饮料500mL', 1, '饮料乳品', '500mL/瓶', '瓶', 1, 10, 10, 3.00, DEFAULT, NULL, '2026-06-14', '2026-12-11', 'PC20261111-ca9865d6dfbb4f0693d0c3d9', 10, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:39:59.231775');
INSERT INTO `purchase_item` VALUES (38, 13, 1, 26, 'HX_MILK_250', '6902026111102', '晨光鲜牛奶250mL', 1, '饮料乳品', '250mL/盒', '盒', 1, 6, 6, 3.20, DEFAULT, NULL, '2026-11-04', '2026-11-11', 'PC20261111-d0fbaca213944542b709fdf3', 6, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:27.885427');
INSERT INTO `purchase_item` VALUES (39, 13, 2, 26, 'HX_MILK_250', '6902026111102', '晨光鲜牛奶250mL', 1, '饮料乳品', '250mL/盒', '盒', 1, 24, 24, 3.20, DEFAULT, NULL, '2026-11-11', '2026-11-18', 'PC20261111-cb7bc267c74f473a8d1eb0cf', 24, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:27.885427');
INSERT INTO `purchase_item` VALUES (40, 13, 3, 27, 'HX_BREAD_80', '6902026111103', '手作软面包80g', 5, '糕点零食', '80g/袋', '袋', 1, 12, 12, 3.50, DEFAULT, NULL, '2026-11-07', '2026-11-12', 'PC20261111-478f8e1227704de9a2bcdc48', 12, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:27.885427');
INSERT INTO `purchase_item` VALUES (41, 14, 1, 28, 'HX_RICE_5KG', '6902026111104', '东北珍珠米5kg', 2, '粮油食品', '5kg/袋', '袋', 1, 30, 30, 39.00, DEFAULT, NULL, '2026-10-01', '2027-10-01', 'PC20261111-dd9fc749ff9d4dd9b7611a13', 30, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:39.700274');
INSERT INTO `purchase_item` VALUES (42, 14, 2, 31, 'HX_NOODLE_5P', '6902026111107', '香菇炖鸡面五连包', 2, '粮油食品', '五连包', '袋', 1, 40, 40, 9.80, DEFAULT, NULL, '2026-11-01', '2027-04-30', 'PC20261111-b320edb92f4847389d30acf5', 40, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:39.700274');
INSERT INTO `purchase_item` VALUES (43, 14, 3, 32, 'HX_BISCUIT_100', '6902026111108', '苏打饼干100g', 5, '糕点零食', '100g/袋', '袋', 1, 24, 24, 4.20, DEFAULT, NULL, '2026-11-01', '2027-07-29', 'PC20261111-58c18fc590814040b009914b', 24, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:39.700274');
INSERT INTO `purchase_item` VALUES (44, 15, 1, 29, 'HX_TISSUE_3P', '6902026111105', '柔韧抽纸三包装', 3, '日用清洁', '100抽×3包', '件', 0, 24, 24, 7.50, DEFAULT, NULL, NULL, NULL, 'PC20261111-e8cd41bd4a2e419698e7129d', 24, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:57.583158');
INSERT INTO `purchase_item` VALUES (45, 15, 2, 30, 'HX_SOAP_100', '6902026111106', '清洁香皂100g', 3, '日用清洁', '100g/块', '件', 0, 36, 36, 3.80, DEFAULT, NULL, NULL, NULL, 'PC20261111-1ba2013c548041e8bd190cb4', 36, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:57.583158');
INSERT INTO `purchase_item` VALUES (46, 15, 3, 33, 'HX_WATER_550', '6902026111109', '天然饮用水550mL', 1, '饮料乳品', '550mL/瓶', '瓶', 1, 96, 96, 0.90, DEFAULT, NULL, '2026-11-01', '2027-11-01', 'PC20261111-1835f173b33e42b4844fdd82', 96, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:57.583158');
INSERT INTO `purchase_item` VALUES (47, 15, 4, 34, 'HX_PEN_05', '6902026111110', '顺滑中性笔0.5mm', 4, '文具用品', '黑色0.5mm', '支', 0, 60, 60, 1.20, DEFAULT, NULL, NULL, NULL, 'PC20261111-7ae0d5b7becd42de90634b31', 60, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:57.583158');
INSERT INTO `purchase_item` VALUES (48, 15, 5, 35, 'HX_NOTE_A5', '6902026111111', 'A5横线笔记本', 4, '文具用品', 'A5/60页', '本', 0, 30, 30, 2.60, DEFAULT, NULL, NULL, NULL, 'PC20261111-c8342314cad146d5a287925b', 30, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:57.583158');
INSERT INTO `purchase_item` VALUES (49, 15, 6, 36, 'HX_DETERGENT_1KG', '6902026111112', '清新洗衣液1kg', 3, '日用清洁', '1kg/瓶', '瓶', 0, 24, 24, 12.80, DEFAULT, NULL, NULL, NULL, 'PC20261111-d7aa22831d384cf88fa88407', 24, NULL, NULL, DEFAULT, NULL, '2026-11-11 12:40:57.583158');
INSERT INTO `purchase_item` VALUES (51, 16, 1, 34, 'HX_PEN_05', '6902026111110', '顺滑中性笔0.5mm', 4, '文具用品', '黑色0.5mm', '支', 0, 4, 4, 1.20, DEFAULT, NULL, NULL, NULL, NULL, NULL, NULL, NULL, DEFAULT, NULL, '2026-11-11 13:46:30.295376');
INSERT INTO `purchase_item` VALUES (52, 17, 1, 34, 'HX_PEN_05', '6902026111110', '顺滑中性笔0.5mm', 4, '文具用品', '黑色0.5mm', '支', 0, 4, 4, 1.20, DEFAULT, NULL, NULL, NULL, 'PC20261111-8d81cb399de1481ba4ae86ce', 0, NULL, NULL, DEFAULT, NULL, '2026-11-11 13:46:54.974983');
INSERT INTO `purchase_item` VALUES (53, 18, 1, 34, 'HX_PEN_05', '6902026111110', '顺滑中性笔0.5mm', 4, '文具用品', '黑色0.5mm', '支', 0, 2, 2, 1.20, DEFAULT, NULL, NULL, NULL, 'PC20261111-316ceec8a86b49db9180f225', 0, NULL, NULL, DEFAULT, NULL, '2026-11-11 13:54:24.892028');

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
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购单与当前对账摘要；核对撤回历史不覆盖，通过operation_log保留' ROW_FORMAT = Dynamic;

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
INSERT INTO `purchase_order` VALUES (11, 'CG20261111-bb6deb4160294acd8a2912e8', 9, '禾序民生商贸（演示）', '门店配送', '027-00000000', '演示地址：本地商贸配送中心', 3, '采购张三', 'IN_STOCK', 20, 20, 60.00, NULL, '2026-11-11 12:21:23.000000', '2026-11-11 12:27:25.870636', '2026-11-11 12:27:25.870636', 1, '系统管理员', '2026-11-11 12:27:58.169611', NULL, '2026-11-11 12:27:58.169611', NULL, NULL, NULL, NULL, NULL, NULL, 'MATCHED', 60.00, 'HX-20261111-001', NULL, NULL, 1, '2026-11-11 13:48:06.044235', 3, '正式库用户验收：饮料20→销售12→盘亏1→再售2→补货10的完整闭环。', 8, '2026-11-11 12:19:12.106414', '2026-11-11 13:48:06.044834');
INSERT INTO `purchase_order` VALUES (12, 'CG20261111-69ff3b05ca644b9bbfb40941', 9, '禾序民生商贸（演示）', '门店配送', '027-00000000', '演示地址：本地商贸配送中心', 3, '采购张三', 'IN_STOCK', 10, 10, 30.00, NULL, '2026-11-11 12:39:59.000000', '2026-11-11 12:39:59.624100', '2026-11-11 12:39:59.624100', 1, '系统管理员', '2026-11-11 12:40:12.618016', NULL, '2026-11-11 12:40:12.618016', NULL, NULL, NULL, NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 0, '正式库业务闭环：人工确认补货10瓶，预警消除验证。', 2, '2026-11-11 12:39:59.231775', '2026-11-11 12:40:12.623615');
INSERT INTO `purchase_order` VALUES (13, 'CG20261111-906bebcb72b046dba9954207', 10, '晨光乳品配送（演示）', '乳品配送', '027-00000001', '演示地址：乳品配送中心', 3, '采购张三', 'IN_STOCK', 42, 42, 138.00, NULL, '2026-11-11 12:40:27.000000', '2026-11-11 12:40:28.269402', '2026-11-11 12:40:28.269402', 1, '系统管理员', '2026-11-11 12:41:10.080332', NULL, '2026-11-11 12:41:10.080332', NULL, NULL, NULL, NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 0, '正式库演示：鲜奶当天到期与新鲜批次、面包临期，按实物标签登记。', 2, '2026-11-11 12:40:27.885427', '2026-11-11 12:41:10.086156');
INSERT INTO `purchase_order` VALUES (14, 'CG20261111-d04da38a7f4a43a29910ef96', 11, '诚信粮油配送（演示）', '粮油配送', '027-00000002', '演示地址：粮油配送中心', 3, '采购张三', 'IN_STOCK', 94, 94, 1662.80, NULL, '2026-11-11 12:40:39.000000', '2026-11-11 12:40:40.070667', '2026-11-11 12:40:40.070667', 1, '系统管理员', '2026-11-11 12:41:10.513934', NULL, '2026-11-11 12:41:10.513934', NULL, NULL, NULL, NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 0, '正式库演示：粮油及常温食品正常备货，通过采购审核生成来源批次。', 2, '2026-11-11 12:40:39.700274', '2026-11-11 12:41:10.521325');
INSERT INTO `purchase_order` VALUES (15, 'CG20261111-3684ad613a9a416fa76442ee', 9, '禾序民生商贸（演示）', '门店配送', '027-00000000', '演示地址：本地商贸配送中心', 3, '采购张三', 'IN_STOCK', 270, 270, 860.40, NULL, '2026-11-11 12:40:57.000000', '2026-11-11 12:40:57.965713', '2026-11-11 12:40:57.965713', 1, '系统管理员', '2026-11-11 12:41:10.712596', NULL, '2026-11-11 12:41:10.712596', NULL, NULL, NULL, NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 0, '正式库演示：日用清洁、饮水及文具常规备货；数量以各自基本单位计。', 2, '2026-11-11 12:40:57.583158', '2026-11-11 12:41:10.720359');
INSERT INTO `purchase_order` VALUES (16, 'CG20261111-e50e2ce3d3554a11852c767b', 9, '禾序民生商贸（演示）', '门店配送', '027-00000000', '演示地址：本地商贸配送中心', 3, '采购张三', 'CLOSED', 4, 4, 4.80, NULL, '2026-11-11 13:45:54.000000', '2026-11-11 13:46:00.402431', NULL, NULL, NULL, NULL, NULL, NULL, 3, '2026-11-11 13:46:42.275391', '验收演示完成，关闭未生效采购，保留业务历史。', NULL, NULL, NULL, 'UNRECONCILED', NULL, NULL, NULL, NULL, NULL, NULL, 0, '验收演示：已核实到货备注；本次测试撤回后关闭，不生效。', 6, '2026-11-11 13:45:56.389953', '2026-11-11 13:46:42.275695');
INSERT INTO `purchase_order` VALUES (17, 'CG20261111-719878c34de940709cdff199', 9, '禾序民生商贸（演示）', '门店配送', '027-00000000', '演示地址：本地商贸配送中心', 3, '采购张三', 'REVERSED', 4, 4, 4.80, NULL, '2026-11-11 13:46:54.000000', '2026-11-11 13:47:17.159562', '2026-11-11 13:47:17.159562', 1, '系统管理员', '2026-11-11 13:47:19.739365', NULL, '2026-11-11 13:47:19.739365', NULL, NULL, NULL, 1, '2026-11-11 13:47:47.068351', '验收演示结束，本批未消耗、已撤回核对，整单撤销。', 'NA', NULL, NULL, NULL, NULL, NULL, NULL, 3, '验收演示：对账逐行差异、撤回核对与撤销入库；本批未售出。', 6, '2026-11-11 13:46:54.974983', '2026-11-11 13:47:47.071125');
INSERT INTO `purchase_order` VALUES (18, 'CG20261111-0a90e6b9f2bb4ffb8c342970', 9, '禾序民生商贸（演示）', '门店配送', '027-00000000', '演示地址：本地商贸配送中心', 1, '系统管理员', 'REVERSED', 2, 2, 2.40, NULL, '2026-11-11 13:54:24.000000', '2026-11-11 13:54:25.326410', '2026-11-11 13:54:25.326410', 1, '系统管理员', '2026-11-11 13:55:00.533736', NULL, '2026-11-11 13:55:00.533736', NULL, NULL, NULL, 1, '2026-11-11 13:55:01.207966', '验收演示结束，冻结释放后验证入库正常，本批未使用撤销。', 'NA', NULL, NULL, NULL, NULL, NULL, NULL, 0, '验收演示：验证盘点冻结阻止采购审核，释放后撤销此批。', 3, '2026-11-11 13:54:24.892028', '2026-11-11 13:55:01.212564');

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
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '按FEFO/FIFO扣库明细，也是销售纠错恢复批次的依据' ROW_FORMAT = Dynamic;

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
INSERT INTO `sale_allocation` VALUES (11, 10, 25, 35, 10);
INSERT INTO `sale_allocation` VALUES (12, 10, 25, 36, 2);
INSERT INTO `sale_allocation` VALUES (13, 11, 25, 36, 2);
INSERT INTO `sale_allocation` VALUES (14, 12, 34, 47, 1);
INSERT INTO `sale_allocation` VALUES (15, 13, 9, 9, 4);
INSERT INTO `sale_allocation` VALUES (16, 14, 30, 45, 1);
INSERT INTO `sale_allocation` VALUES (19, 17, 34, 47, 1);
INSERT INTO `sale_allocation` VALUES (20, 18, 35, 48, 1);
INSERT INTO `sale_allocation` VALUES (21, 19, 9, 9, 4);
INSERT INTO `sale_allocation` VALUES (22, 20, 34, 47, 1);

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
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '销售商品明细，不包含收银角色无权查看的采购成本' ROW_FORMAT = Dynamic;

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
INSERT INTO `sale_item` VALUES (10, 8, 1, 25, 'HX_DRINK_500', '6902026111101', '禾序瓶装饮料500mL', 1, '饮料乳品', '500mL/瓶', '瓶', 5.00, 12, DEFAULT);
INSERT INTO `sale_item` VALUES (11, 9, 1, 25, 'HX_DRINK_500', '6902026111101', '禾序瓶装饮料500mL', 1, '饮料乳品', '500mL/瓶', '瓶', 5.00, 2, DEFAULT);
INSERT INTO `sale_item` VALUES (12, 10, 1, 34, 'HX_PEN_05', '6902026111110', '顺滑中性笔0.5mm', 4, '文具用品', '黑色0.5mm', '支', 2.50, 1, DEFAULT);
INSERT INTO `sale_item` VALUES (13, 11, 1, 9, 'P0009', '6900000000009', '并发测试矿泉水', 1, '饮料乳品', '基本单位包装', '瓶', 2.00, 4, DEFAULT);
INSERT INTO `sale_item` VALUES (14, 12, 1, 30, 'HX_SOAP_100', '6902026111106', '清洁香皂100g', 3, '日用清洁', '100g/块', '件', 7.25, 1, DEFAULT);
INSERT INTO `sale_item` VALUES (17, 14, 1, 34, 'HX_PEN_05', '6902026111110', '顺滑中性笔0.5mm', 4, '文具用品', '黑色0.5mm', '支', 2.50, 1, DEFAULT);
INSERT INTO `sale_item` VALUES (18, 14, 2, 35, 'HX_NOTE_A5', '6902026111111', 'A5横线笔记本', 4, '文具用品', 'A5/60页', '本', 5.00, 1, DEFAULT);
INSERT INTO `sale_item` VALUES (19, 15, 1, 9, 'P0009', '6900000000009', '并发测试矿泉水', 1, '饮料乳品', '基本单位包装', '瓶', 2.00, 4, DEFAULT);
INSERT INTO `sale_item` VALUES (20, 16, 1, 34, 'HX_PEN_05', '6902026111110', '顺滑中性笔0.5mm', 4, '文具用品', '黑色0.5mm', '支', 2.50, 1, DEFAULT);

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
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '已完成销售及最多一次当天整单纠错，保留原金额和时间' ROW_FORMAT = Dynamic;

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
INSERT INTO `sale_order` VALUES (8, 'XS20261111-2e0c4715094e4cc1a1c5c80d', '5ea42c6b-90e2-41ec-af6e-77c95ccf81bd', '7eab070ddcba89a23ce065497dff86723771f5f083fdae34bb15816ceb35e595', 5, '收银王五', 'COMPLETED', 12, 60.00, 'CASH', 100.00, DEFAULT, '2026-11-11 12:28:47.794875', NULL, NULL, NULL, NULL, 0, '2026-11-11 12:28:47.794875');
INSERT INTO `sale_order` VALUES (9, 'XS20261111-3a8fe78e7b7e429c95ea7652', 'da412276-baf8-4c36-9131-84fb43678dbc', '49c949bde223eb9b91ed9a8b2535c71e74d503a1a1978e59bed52c4b33328f99', 5, '收银王五', 'COMPLETED', 2, 10.00, 'MOCK_EPAY', 10.00, DEFAULT, '2026-11-11 12:31:32.291192', NULL, NULL, NULL, NULL, 0, '2026-11-11 12:31:32.291192');
INSERT INTO `sale_order` VALUES (10, 'XS20261111-80a6d4e2d1ee4c6191169706', 'f36e307c-17e7-4fbc-bba5-b0a62b7b930e', '9bbed26584230757cad77d0b85c49c608f30c81293efa511a2121d8f04b5a98f', 6, '收银赵六', 'CORRECTED', 1, 2.50, 'MOCK_EPAY', 2.50, DEFAULT, '2026-11-11 12:57:48.622765', 1, '系统管理员', '2026-11-11 12:58:06.274174', '验收演示：断网查单流程完成，实物完整在店，整单撤销模拟错录。', 1, '2026-11-11 12:57:48.622765');
INSERT INTO `sale_order` VALUES (11, 'XS20261111-320744f63f284c629ac317d6', '8faa8166-4680-4e6e-8a47-a37e7b84ef4a', '77ad95e7a2d35582299930be1f783248823c3e17352fe2023b353513dc28d487', 6, '收银赵六', 'CORRECTED', 4, 8.00, 'MOCK_EPAY', 8.00, DEFAULT, '2026-11-11 12:59:04.253260', 1, '系统管理员', '2026-11-11 12:59:51.250517', '验收演示：两收银员并发验证后，实物完整，整单纠错恢复原批次。', 1, '2026-11-11 12:59:04.253260');
INSERT INTO `sale_order` VALUES (12, 'XS20261111-89df136b8e2f400f945b4d8c', '70a293ac-c804-4efc-bb35-e7e8c09a2d19', '244d8e90cf8455a7ed4c43c6b0fd7cf90d025eef09a01c9e1de0789bc601d3b0', 5, '收银王五', 'CORRECTED', 1, 7.25, 'MOCK_EPAY', 7.25, DEFAULT, '2026-11-11 13:42:31.615526', 1, '系统管理员', '2026-11-11 13:42:33.664696', '验收演示：调价二次确认完成，实物完整，整单纠错恢复原库存。', 1, '2026-11-11 13:42:31.615526');
INSERT INTO `sale_order` VALUES (14, 'XS20261111-73d230811e114f149db42dde', '52710215-0b11-4082-a8be-87419ba56c2f', '2b39e8dc48d2a89265913d63208464d1230e94be0ec33c328eb14eed32df5300', 5, '收银王五', 'CORRECTED', 2, 7.50, 'MOCK_EPAY', 7.50, DEFAULT, '2026-11-11 13:44:23.158348', 1, '系统管理员', '2026-11-11 13:44:24.682363', '验收演示：事务故障撤除后同键成功，实物完整，整单纠错恢复库存。', 1, '2026-11-11 13:44:23.158348');
INSERT INTO `sale_order` VALUES (15, 'XS20261111-126ef997a16444b8ae200630', '41b3aab8-0097-4531-b08e-df72acee7ae7', '77ad95e7a2d35582299930be1f783248823c3e17352fe2023b353513dc28d487', 6, '收银赵六', 'CORRECTED', 4, 8.00, 'MOCK_EPAY', 8.00, DEFAULT, '2026-11-11 13:53:26.315521', 1, '系统管理员', '2026-11-11 13:53:55.242652', '验收演示：验证缺货整单失败后，实物完整，纠错恢复矿泉水。', 1, '2026-11-11 13:53:26.315521');
INSERT INTO `sale_order` VALUES (16, 'XS20261111-6d83d65961f04f1cad9ed9b1', 'f0981c92-8d82-4bc3-be22-ebb51f2b989a', '30675032db4ea3d5f1213c1b903551decff8064c602c038180390368795f505b', 5, '收银王五', 'CORRECTED', 1, 2.50, 'MOCK_EPAY', 2.50, DEFAULT, '2026-11-11 13:54:27.346669', 1, '系统管理员', '2026-11-11 13:55:01.925357', '验收演示：冻结释放后恢复原销售，实物完整在店。', 1, '2026-11-11 13:54:27.346669');

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
) ENGINE = InnoDB AUTO_INCREMENT = 80 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '不可任意编辑删除的库存流水，正反事件构建统计视图，防止冲销覆盖历史' ROW_FORMAT = Dynamic;

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
INSERT INTO `stock_movement` VALUES (39, 'PURCHASE_IN', 25, 35, NULL, NULL, DEFAULT, 'CG20261111-bb6deb4160294acd8a2912e8', 10, 0, 10, 0, 10, 1, '系统管理员', NULL, '2026-11-11 12:27:58.169611');
INSERT INTO `stock_movement` VALUES (40, 'PURCHASE_IN', 25, 36, NULL, NULL, DEFAULT, 'CG20261111-bb6deb4160294acd8a2912e8', 10, 0, 10, 10, 20, 1, '系统管理员', NULL, '2026-11-11 12:27:58.169611');
INSERT INTO `stock_movement` VALUES (41, 'SALE_OUT', 25, 35, 11, NULL, DEFAULT, 'XS20261111-2e0c4715094e4cc1a1c5c80d', -10, 10, 0, 20, 10, 5, '收银王五', NULL, '2026-11-11 12:28:47.794875');
INSERT INTO `stock_movement` VALUES (42, 'SALE_OUT', 25, 36, 12, NULL, DEFAULT, 'XS20261111-2e0c4715094e4cc1a1c5c80d', -2, 10, 8, 10, 8, 5, '收银王五', NULL, '2026-11-11 12:28:47.794875');
INSERT INTO `stock_movement` VALUES (43, 'STOCKTAKE_LOSS', 25, 36, NULL, 10, DEFAULT, 'PD20261111-c6484bc69f0845349b0e38ec', -1, 8, 7, 8, 7, 1, '系统管理员', '验收演示：逐批清点，第二批少一瓶，按实盘登记。', '2026-11-11 12:31:19.107944');
INSERT INTO `stock_movement` VALUES (44, 'SALE_OUT', 25, 36, 13, NULL, DEFAULT, 'XS20261111-3a8fe78e7b7e429c95ea7652', -2, 7, 5, 7, 5, 5, '收银王五', NULL, '2026-11-11 12:31:32.291192');
INSERT INTO `stock_movement` VALUES (45, 'PURCHASE_IN', 25, 37, NULL, NULL, DEFAULT, 'CG20261111-69ff3b05ca644b9bbfb40941', 10, 0, 10, 5, 15, 1, '系统管理员', NULL, '2026-11-11 12:40:12.618016');
INSERT INTO `stock_movement` VALUES (46, 'PURCHASE_IN', 26, 38, NULL, NULL, DEFAULT, 'CG20261111-906bebcb72b046dba9954207', 6, 0, 6, 0, 6, 1, '系统管理员', NULL, '2026-11-11 12:41:10.080332');
INSERT INTO `stock_movement` VALUES (47, 'PURCHASE_IN', 26, 39, NULL, NULL, DEFAULT, 'CG20261111-906bebcb72b046dba9954207', 24, 0, 24, 6, 30, 1, '系统管理员', NULL, '2026-11-11 12:41:10.080332');
INSERT INTO `stock_movement` VALUES (48, 'PURCHASE_IN', 27, 40, NULL, NULL, DEFAULT, 'CG20261111-906bebcb72b046dba9954207', 12, 0, 12, 0, 12, 1, '系统管理员', NULL, '2026-11-11 12:41:10.080332');
INSERT INTO `stock_movement` VALUES (49, 'PURCHASE_IN', 28, 41, NULL, NULL, DEFAULT, 'CG20261111-d04da38a7f4a43a29910ef96', 30, 0, 30, 0, 30, 1, '系统管理员', NULL, '2026-11-11 12:41:10.513934');
INSERT INTO `stock_movement` VALUES (50, 'PURCHASE_IN', 31, 42, NULL, NULL, DEFAULT, 'CG20261111-d04da38a7f4a43a29910ef96', 40, 0, 40, 0, 40, 1, '系统管理员', NULL, '2026-11-11 12:41:10.513934');
INSERT INTO `stock_movement` VALUES (51, 'PURCHASE_IN', 32, 43, NULL, NULL, DEFAULT, 'CG20261111-d04da38a7f4a43a29910ef96', 24, 0, 24, 0, 24, 1, '系统管理员', NULL, '2026-11-11 12:41:10.513934');
INSERT INTO `stock_movement` VALUES (52, 'PURCHASE_IN', 29, 44, NULL, NULL, DEFAULT, 'CG20261111-3684ad613a9a416fa76442ee', 24, 0, 24, 0, 24, 1, '系统管理员', NULL, '2026-11-11 12:41:10.712596');
INSERT INTO `stock_movement` VALUES (53, 'PURCHASE_IN', 30, 45, NULL, NULL, DEFAULT, 'CG20261111-3684ad613a9a416fa76442ee', 36, 0, 36, 0, 36, 1, '系统管理员', NULL, '2026-11-11 12:41:10.712596');
INSERT INTO `stock_movement` VALUES (54, 'PURCHASE_IN', 33, 46, NULL, NULL, DEFAULT, 'CG20261111-3684ad613a9a416fa76442ee', 96, 0, 96, 0, 96, 1, '系统管理员', NULL, '2026-11-11 12:41:10.712596');
INSERT INTO `stock_movement` VALUES (55, 'PURCHASE_IN', 34, 47, NULL, NULL, DEFAULT, 'CG20261111-3684ad613a9a416fa76442ee', 60, 0, 60, 0, 60, 1, '系统管理员', NULL, '2026-11-11 12:41:10.712596');
INSERT INTO `stock_movement` VALUES (56, 'PURCHASE_IN', 35, 48, NULL, NULL, DEFAULT, 'CG20261111-3684ad613a9a416fa76442ee', 30, 0, 30, 0, 30, 1, '系统管理员', NULL, '2026-11-11 12:41:10.712596');
INSERT INTO `stock_movement` VALUES (57, 'PURCHASE_IN', 36, 49, NULL, NULL, DEFAULT, 'CG20261111-3684ad613a9a416fa76442ee', 24, 0, 24, 0, 24, 1, '系统管理员', NULL, '2026-11-11 12:41:10.712596');
INSERT INTO `stock_movement` VALUES (58, 'SALE_OUT', 34, 47, 14, NULL, DEFAULT, 'XS20261111-80a6d4e2d1ee4c6191169706', -1, 60, 59, 60, 59, 6, '收银赵六', NULL, '2026-11-11 12:57:48.622765');
INSERT INTO `stock_movement` VALUES (59, 'SALE_CORRECTION', 34, 47, 14, NULL, DEFAULT, 'XS20261111-80a6d4e2d1ee4c6191169706', 1, 59, 60, 59, 60, 1, '系统管理员', '验收演示：断网查单流程完成，实物完整在店，整单撤销模拟错录。', '2026-11-11 12:58:06.274174');
INSERT INTO `stock_movement` VALUES (60, 'SALE_OUT', 9, 9, 15, NULL, DEFAULT, 'XS20261111-320744f63f284c629ac317d6', -4, 5, 1, 5, 1, 6, '收银赵六', NULL, '2026-11-11 12:59:04.253260');
INSERT INTO `stock_movement` VALUES (61, 'SALE_CORRECTION', 9, 9, 15, NULL, DEFAULT, 'XS20261111-320744f63f284c629ac317d6', 4, 1, 5, 1, 5, 1, '系统管理员', '验收演示：两收银员并发验证后，实物完整，整单纠错恢复原批次。', '2026-11-11 12:59:51.250517');
INSERT INTO `stock_movement` VALUES (62, 'SALE_OUT', 30, 45, 16, NULL, DEFAULT, 'XS20261111-89df136b8e2f400f945b4d8c', -1, 36, 35, 36, 35, 5, '收银王五', NULL, '2026-11-11 13:42:31.615526');
INSERT INTO `stock_movement` VALUES (63, 'SALE_CORRECTION', 30, 45, 16, NULL, DEFAULT, 'XS20261111-89df136b8e2f400f945b4d8c', 1, 35, 36, 35, 36, 1, '系统管理员', '验收演示：调价二次确认完成，实物完整，整单纠错恢复原库存。', '2026-11-11 13:42:33.664696');
INSERT INTO `stock_movement` VALUES (65, 'SALE_OUT', 34, 47, 19, NULL, DEFAULT, 'XS20261111-73d230811e114f149db42dde', -1, 60, 59, 60, 59, 5, '收银王五', NULL, '2026-11-11 13:44:23.158348');
INSERT INTO `stock_movement` VALUES (66, 'SALE_OUT', 35, 48, 20, NULL, DEFAULT, 'XS20261111-73d230811e114f149db42dde', -1, 30, 29, 30, 29, 5, '收银王五', NULL, '2026-11-11 13:44:23.158348');
INSERT INTO `stock_movement` VALUES (67, 'SALE_CORRECTION', 34, 47, 19, NULL, DEFAULT, 'XS20261111-73d230811e114f149db42dde', 1, 59, 60, 59, 60, 1, '系统管理员', '验收演示：事务故障撤除后同键成功，实物完整，整单纠错恢复库存。', '2026-11-11 13:44:24.682363');
INSERT INTO `stock_movement` VALUES (68, 'SALE_CORRECTION', 35, 48, 20, NULL, DEFAULT, 'XS20261111-73d230811e114f149db42dde', 1, 29, 30, 29, 30, 1, '系统管理员', '验收演示：事务故障撤除后同键成功，实物完整，整单纠错恢复库存。', '2026-11-11 13:44:24.682363');
INSERT INTO `stock_movement` VALUES (69, 'PURCHASE_IN', 34, 52, NULL, NULL, DEFAULT, 'CG20261111-719878c34de940709cdff199', 4, 0, 4, 60, 64, 1, '系统管理员', NULL, '2026-11-11 13:47:19.739365');
INSERT INTO `stock_movement` VALUES (70, 'PURCHASE_REVERSE', 34, 52, NULL, NULL, DEFAULT, 'CG20261111-719878c34de940709cdff199', -4, 4, 0, 64, 60, 1, '系统管理员', '验收演示结束，本批未消耗、已撤回核对，整单撤销。', '2026-11-11 13:47:47.068351');
INSERT INTO `stock_movement` VALUES (71, 'SALE_OUT', 9, 9, 21, NULL, DEFAULT, 'XS20261111-126ef997a16444b8ae200630', -4, 5, 1, 5, 1, 6, '收银赵六', NULL, '2026-11-11 13:53:26.315521');
INSERT INTO `stock_movement` VALUES (72, 'SALE_CORRECTION', 9, 9, 21, NULL, DEFAULT, 'XS20261111-126ef997a16444b8ae200630', 4, 1, 5, 1, 5, 1, '系统管理员', '验收演示：验证缺货整单失败后，实物完整，纠错恢复矿泉水。', '2026-11-11 13:53:55.242652');
INSERT INTO `stock_movement` VALUES (73, 'STOCKTAKE_LOSS', 4, 19, NULL, 12, DEFAULT, 'PD20261111-225ccd1bd48a47be81c0dc68', -6, 6, 0, 6, 0, 1, '系统管理员', '正式库演示过期清理：6件过期面包按批次清理，实盘为0。', '2026-11-11 13:53:59.344914');
INSERT INTO `stock_movement` VALUES (74, 'SALE_OUT', 34, 47, 22, NULL, DEFAULT, 'XS20261111-6d83d65961f04f1cad9ed9b1', -1, 60, 59, 60, 59, 5, '收银王五', NULL, '2026-11-11 13:54:27.346669');
INSERT INTO `stock_movement` VALUES (75, 'PURCHASE_IN', 34, 53, NULL, NULL, DEFAULT, 'CG20261111-0a90e6b9f2bb4ffb8c342970', 2, 0, 2, 59, 61, 1, '系统管理员', NULL, '2026-11-11 13:55:00.533736');
INSERT INTO `stock_movement` VALUES (76, 'PURCHASE_REVERSE', 34, 53, NULL, NULL, DEFAULT, 'CG20261111-0a90e6b9f2bb4ffb8c342970', -2, 2, 0, 61, 59, 1, '系统管理员', '验收演示结束，冻结释放后验证入库正常，本批未使用撤销。', '2026-11-11 13:55:01.207966');
INSERT INTO `stock_movement` VALUES (77, 'SALE_CORRECTION', 34, 47, 22, NULL, DEFAULT, 'XS20261111-6d83d65961f04f1cad9ed9b1', 1, 59, 60, 59, 60, 1, '系统管理员', '验收演示：冻结释放后恢复原销售，实物完整在店。', '2026-11-11 13:55:01.925357');
INSERT INTO `stock_movement` VALUES (78, 'STOCKTAKE_GAIN', 35, 48, NULL, 15, DEFAULT, 'PD20261111-3c4d38e55b32426d8e2e6c22', 1, 30, 31, 30, 31, 1, '系统管理员', '验收演示：同来源批次清点盘盈1本，核实库存增加流水。', '2026-11-11 14:25:44.747846');
INSERT INTO `stock_movement` VALUES (79, 'STOCKTAKE_LOSS', 35, 48, NULL, 16, DEFAULT, 'PD20261111-4f09ceceb2284b4c906dd3b3', -1, 31, 30, 31, 30, 1, '系统管理员', '验收演示：复核原实物30本，正常盘亏1本还原演示差异。', '2026-11-11 14:27:01.432225');

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
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '批次账面快照与实盘差异；不允许未知来源盘盈' ROW_FORMAT = Dynamic;

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
INSERT INTO `stocktake_item` VALUES (9, 6, 35, 25, 0, 0, DEFAULT, NULL, NULL);
INSERT INTO `stocktake_item` VALUES (10, 6, 36, 25, 8, 7, DEFAULT, 'COUNT_ERROR', '验收演示：逐批清点，第二批少一瓶，按实盘登记。');
INSERT INTO `stocktake_item` VALUES (11, 7, 45, 30, 36, 0, DEFAULT, 'COUNT_ERROR', '验收草稿：测试零实盘保存及撤回关闭，本单不审核调整。');
INSERT INTO `stocktake_item` VALUES (12, 8, 19, 4, 6, 0, DEFAULT, 'EXPIRY', '正式库演示过期清理：6件过期面包按批次清理，实盘为0。');
INSERT INTO `stocktake_item` VALUES (13, 9, 47, 34, 59, NULL, DEFAULT, NULL, NULL);
INSERT INTO `stocktake_item` VALUES (14, 10, 48, 35, 30, NULL, DEFAULT, NULL, NULL);
INSERT INTO `stocktake_item` VALUES (15, 11, 48, 35, 30, 31, DEFAULT, 'COUNT_ERROR', '验收演示：同来源批次清点盘盈1本，核实库存增加流水。');
INSERT INTO `stocktake_item` VALUES (16, 12, 48, 35, 31, 30, DEFAULT, 'COUNT_ERROR', '验收演示：复核原实物30本，正常盘亏1本还原演示差异。');

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
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '盘点任务、审核及冻结生命周期' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stocktake_order
-- ----------------------------
INSERT INTO `stocktake_order` VALUES (1, 'PD-DEMO-0001', 3, '采购张三', 'COMPLETED', '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', 1, '系统管理员', '2026-11-10 16:13:25.806904', NULL, '2026-11-10 16:13:25.806904', NULL, NULL, NULL, '测试冻结、空值、差异、审核和关闭', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `stocktake_order` VALUES (2, 'PD-DEMO-0002', 3, '采购张三', 'COUNTING', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '测试冻结、空值、差异、审核和关闭', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `stocktake_order` VALUES (3, 'PD-DEMO-0003', 4, '采购李四', 'PENDING', '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '测试冻结、空值、差异、审核和关闭', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `stocktake_order` VALUES (4, 'PD-DEMO-0004', 3, '采购张三', 'REJECTED', '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904', 1, '系统管理员', '2026-11-10 16:13:25.806904', '实盘说明待补充核实', NULL, NULL, NULL, NULL, '测试冻结、空值、差异、审核和关闭', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `stocktake_order` VALUES (5, 'PD-DEMO-0005', 4, '采购李四', 'CLOSED', '2026-11-10 16:13:25.806904', NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-11-10 16:13:25.806904', '取消本次盘点并释放冻结', '测试冻结、空值、差异、审核和关闭', 0, '2026-11-10 16:13:25.806904', '2026-11-10 16:13:25.806904');
INSERT INTO `stocktake_order` VALUES (6, 'PD20261111-c6484bc69f0845349b0e38ec', 3, '采购张三', 'COMPLETED', '2026-11-11 12:29:16.639825', '2026-11-11 12:31:06.892605', 1, '系统管理员', '2026-11-11 12:31:19.107944', NULL, '2026-11-11 12:31:19.107944', NULL, NULL, NULL, '', 3, '2026-11-11 12:29:16.639825', '2026-11-11 12:31:19.110132');
INSERT INTO `stocktake_order` VALUES (7, 'PD20261111-bbb12005b86d4fda87b647b9', 10, '验收任务接续', 'CLOSED', '2026-11-11 12:50:17.942348', '2026-11-11 12:51:01.105908', 1, '系统管理员', '2026-11-11 12:50:36.074949', '验收演示：请经办人再次逐批核对，盘点冻结继续保留。', NULL, 1, '2026-11-11 12:51:04.498340', '验收演示完成，管理员接续关闭未生效盘点，保留原库存。', '', 7, '2026-11-11 12:50:17.942348', '2026-11-11 12:51:04.498992');
INSERT INTO `stocktake_order` VALUES (8, 'PD20261111-225ccd1bd48a47be81c0dc68', 1, '系统管理员', 'COMPLETED', '2026-11-11 13:53:57.282335', '2026-11-11 13:53:58.492710', 1, '系统管理员', '2026-11-11 13:53:59.344914', NULL, '2026-11-11 13:53:59.344914', NULL, NULL, NULL, '', 3, '2026-11-11 13:53:57.282335', '2026-11-11 13:53:59.350939');
INSERT INTO `stocktake_order` VALUES (9, 'PD20261111-e5e0c2ec2803459c94fb0cc3', 1, '系统管理员', 'CLOSED', '2026-11-11 13:54:28.513356', NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-11-11 13:54:59.973166', '验收演示：冻结限制测试完成，关闭未填写盘点，不调整库存。', '', 1, '2026-11-11 13:54:28.513356', '2026-11-11 13:54:59.974155');
INSERT INTO `stocktake_order` VALUES (10, 'PD20261111-0c693d0066ba4cdfadf403a8', 3, '采购张三', 'CLOSED', '2026-11-11 14:19:57.440402', NULL, NULL, NULL, NULL, NULL, NULL, 3, '2026-11-11 14:20:19.994851', '验收演示：丢失成功响应查单完成，关闭盘点释放冻结，库存不变。', '', 1, '2026-11-11 14:19:57.440402', '2026-11-11 14:20:19.996253');
INSERT INTO `stocktake_order` VALUES (11, 'PD20261111-3c4d38e55b32426d8e2e6c22', 1, '系统管理员', 'COMPLETED', '2026-11-11 14:25:24.557006', '2026-11-11 14:25:38.713655', 1, '系统管理员', '2026-11-11 14:25:44.747846', NULL, '2026-11-11 14:25:44.747846', NULL, NULL, NULL, '', 3, '2026-11-11 14:25:24.557006', '2026-11-11 14:25:44.749981');
INSERT INTO `stocktake_order` VALUES (12, 'PD20261111-4f09ceceb2284b4c906dd3b3', 1, '系统管理员', 'COMPLETED', '2026-11-11 14:26:08.441903', '2026-11-11 14:27:00.401386', 1, '系统管理员', '2026-11-11 14:27:01.432225', NULL, '2026-11-11 14:27:01.432225', NULL, NULL, NULL, '', 3, '2026-11-11 14:26:08.441903', '2026-11-11 14:27:01.434028');

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
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '供应商；历史采购使用单头快照' ROW_FORMAT = Dynamic;

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
INSERT INTO `supplier` VALUES (9, 'SUPa4ba67a85acf41b1b87fa0a2', '禾序民生商贸（演示）', '门店配送', '027-00000000', '演示地址：本地商贸配送中心', 1, '正式库用户验收所需演示档案；非真实外部联系信息。', 1, '2026-11-11 12:12:55.490221', '2026-11-11 12:12:55.490221');
INSERT INTO `supplier` VALUES (10, 'SUP608701e13df9419686ddf9af', '晨光乳品配送（演示）', '乳品配送', '027-00000001', '演示地址：乳品配送中心', 1, '正式库演示数据，联系方式为示例，不用于外部联系。', 3, '2026-11-11 12:15:47.004161', '2026-11-11 12:17:43.175642');
INSERT INTO `supplier` VALUES (11, 'SUPd06c829007074167bc39d1e0', '诚信粮油配送（演示）', '粮油配送', '027-00000002', '演示地址：粮油配送中心', 1, '正式库演示数据，联系方式为示例，不用于外部联系。', 3, '2026-11-11 12:15:53.051398', '2026-11-11 12:15:53.051398');

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
INSERT INTO `sys_config` VALUES ('ai.provider_settings', '{\"models\": [\"qwen-plus\", \"qwen-flash\", \"qwen-max\"], \"version\": 6, \"defaultModel\": \"qwen-plus\", \"encryptedKey\": \"+CRcjYhACKRuAsg25G8VdGB/pLOY4/HlChmrwOaU9+2yeqfz52bSSJAz2LmDtWVao5WAY2V8qud+pqNUeqmfLg2uh0LK3pyd3J/H0npRxsHZm4bD705WjcLH7Z7b7O7k4WOGq6LOwjZDkuvMSqG51QpaHn6RZ0HQ7a84KXPRwTLhUe3PRYeMMvCjjhUMAfWm\"}', '模型配置；密钥仅保存AES-GCM密文', 1, 1, '2026-11-11 14:28:35.001989');
INSERT INTO `sys_config` VALUES ('demo.seed_info', '{\"image\": \"测试图片.jpg\", \"fixture\": true, \"password\": \"见README，不在配置中存口令\"}', '测试数据说明', 0, 1, '2026-11-10 16:13:25.806904');
INSERT INTO `sys_config` VALUES ('inventory.default_lower_limit', '5', '新建商品默认库存下限，不覆盖已有商品', 1, 1, '2026-11-11 13:50:19.158012');
INSERT INTO `sys_config` VALUES ('inventory.default_near_expiry_days', '7', '新建商品默认临期天数', 1, 1, '2026-11-11 13:50:19.158012');
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
INSERT INTO `sys_role` VALUES ('ADMIN', '超市管理员', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"system\", \"system.user\", \"system.role\", \"system.config\", \"system.log\", \"system.user.create\", \"system.user.edit\", \"system.user.enable\", \"system.user.reset_password\", \"system.user.delete\", \"system.role.save\", \"system.role.restore\", \"system.config.save\", \"base\", \"base.category\", \"base.category.create\", \"base.category.edit\", \"base.category.enable\", \"base.category.delete\", \"base.product\", \"base.product.create\", \"base.product.edit\", \"base.product.enable\", \"base.product.delete\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"base.supplier.delete\", \"base.product.image\", \"product.purchase_lookup\", \"product.sale_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"purchase.order.review\", \"purchase.order.reject\", \"purchase.order.reverse\", \"sale\", \"sale.order\", \"sale.order.complete\", \"sale.order.correct\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"inventory.stocktake.review\", \"inventory.stocktake.reject\", \"statistics\", \"statistics.sales\", \"statistics.inventory\", \"ai\", \"ai.inventory\", \"ai.sales\"]', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"system\", \"system.user\", \"system.role\", \"system.config\", \"system.log\", \"system.user.create\", \"system.user.edit\", \"system.user.enable\", \"system.user.reset_password\", \"system.user.delete\", \"system.role.save\", \"system.role.restore\", \"system.config.save\", \"base\", \"base.category\", \"base.category.create\", \"base.category.edit\", \"base.category.enable\", \"base.category.delete\", \"base.product\", \"base.product.create\", \"base.product.edit\", \"base.product.enable\", \"base.product.delete\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"base.supplier.delete\", \"base.product.image\", \"product.purchase_lookup\", \"product.sale_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"purchase.order.review\", \"purchase.order.reject\", \"purchase.order.reverse\", \"sale\", \"sale.order\", \"sale.order.complete\", \"sale.order.correct\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"inventory.stocktake.review\", \"inventory.stocktake.reject\", \"statistics\", \"statistics.sales\", \"statistics.inventory\", \"ai\", \"ai.inventory\", \"ai.sales\"]', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"system\", \"system.user\", \"system.role\", \"system.config\", \"system.log\", \"system.user.create\", \"system.user.edit\", \"system.user.enable\", \"system.user.reset_password\", \"system.user.delete\", \"system.role.save\", \"system.role.restore\", \"system.config.save\", \"base\", \"base.category\", \"base.category.create\", \"base.category.edit\", \"base.category.enable\", \"base.category.delete\", \"base.product\", \"base.product.create\", \"base.product.edit\", \"base.product.enable\", \"base.product.delete\", \"base.supplier\", \"base.supplier.create\", \"base.supplier.edit\", \"base.supplier.enable\", \"base.supplier.delete\", \"base.product.image\", \"product.purchase_lookup\", \"product.sale_lookup\", \"purchase\", \"purchase.order\", \"purchase.order.create\", \"purchase.order.edit\", \"purchase.order.delete_draft\", \"purchase.order.submit\", \"purchase.order.withdraw\", \"purchase.order.close\", \"purchase.order.reconcile\", \"purchase.order.review\", \"purchase.order.reject\", \"purchase.order.reverse\", \"sale\", \"sale.order\", \"sale.order.complete\", \"sale.order.correct\", \"inventory\", \"inventory.query\", \"inventory.batch\", \"inventory.movement\", \"inventory.warning\", \"inventory.stocktake\", \"inventory.stocktake.start\", \"inventory.stocktake.save\", \"inventory.stocktake.submit\", \"inventory.stocktake.withdraw\", \"inventory.stocktake.close\", \"inventory.stocktake.review\", \"inventory.stocktake.reject\", \"statistics\", \"statistics.sales\", \"statistics.inventory\", \"ai\", \"ai.inventory\", \"ai.sales\"]', 'ALL', 2, '2026-09-01 00:00:00.000000', '2026-11-11 14:22:32.424321');
INSERT INTO `sys_role` VALUES ('CASHIER', '前台收银操作员', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"product.sale_lookup\", \"sale\", \"sale.order\", \"sale.order.complete\", \"inventory\", \"inventory.query\", \"statistics\", \"statistics.sales\"]', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"product.sale_lookup\", \"sale\", \"sale.order\", \"sale.order.complete\", \"inventory\", \"inventory.query\", \"statistics\", \"statistics.sales\"]', '[\"common\", \"common.dashboard\", \"common.profile\", \"common.avatar\", \"common.password\", \"common.logout\", \"product.sale_lookup\", \"sale\", \"sale.order\", \"sale.order.complete\", \"inventory\", \"inventory.query\", \"statistics\", \"statistics.sales\"]', 'OWN', 2, '2026-09-01 00:00:00.000000', '2026-11-11 12:49:12.917015');
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
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与本人资料；最后管理员保护及账号状态变更在服务事务内实现' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '系统管理员', '13800000001', 'd8d42649-6e06-40f3-b59e-1a98afbe6fbb.png', '{\"mime\": \"image/png\", \"size\": 1020988, \"width\": 1185, \"height\": 931, \"uploadedAt\": \"2026-11-11T08:59:50.795641800Z\"}', 'ADMIN', 1, 19, '2026-11-11 16:55:30.568743', '2026-09-01 00:00:00.000000', '2026-11-11 16:59:55.429318');
INSERT INTO `sys_user` VALUES (2, 'admin2', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '备用管理员', '13800000002', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'ADMIN', 1, 2, '2026-11-11 12:37:41.021443', '2026-09-01 00:00:00.000000', '2026-11-11 13:57:50.507453');
INSERT INTO `sys_user` VALUES (3, 'buyer01', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '采购张三', '13800000003', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'PURCHASER', 1, 16, '2026-11-11 17:00:04.753213', '2026-09-01 00:00:00.000000', '2026-11-11 17:00:24.278614');
INSERT INTO `sys_user` VALUES (4, 'buyer02', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '采购李四', '13800000004', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'PURCHASER', 1, 4, '2026-11-11 13:49:16.778668', '2026-09-01 00:00:00.000000', '2026-11-11 13:50:15.638265');
INSERT INTO `sys_user` VALUES (5, 'cashier01', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '收银王五', '13800000005', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'CASHIER', 1, 9, '2026-11-11 17:00:26.569689', '2026-09-01 00:00:00.000000', '2026-11-11 17:00:53.716870');
INSERT INTO `sys_user` VALUES (6, 'cashier02', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '收银赵六', '13800000006', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'CASHIER', 1, 1, '2026-11-11 13:53:24.923570', '2026-09-01 00:00:00.000000', '2026-11-11 13:53:24.924741');
INSERT INTO `sys_user` VALUES (7, 'disabled01', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '停用收银', '13800000007', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'CASHIER', 0, 0, NULL, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `sys_user` VALUES (8, 'unused01', '$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm', '待删除用户', '13800000008', '测试图片.jpg', '{\"note\": \"共用示例相对路径，未伪造文件尺寸\", \"fixture\": true}', 'CASHIER', 1, 0, NULL, '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000');
INSERT INTO `sys_user` VALUES (10, 'qa_buyer_1111', '$2a$10$QEAlbgwgIsdaEzVYJ9XUTeYIH3Q5gljj/Ux9VLxKpHdq8VNtNEDjK', '验收任务接续', NULL, NULL, NULL, 'PURCHASER', 0, 1, '2026-11-11 12:50:16.843307', '2026-11-11 12:50:15.740029', '2026-11-11 12:51:11.080605');

-- ----------------------------
-- View structure for v_batch_inventory
-- ----------------------------
DROP VIEW IF EXISTS `v_batch_inventory`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY INVOKER VIEW `v_batch_inventory` AS select `i`.`id` AS `batch_id`,`i`.`batch_no` AS `batch_no`,`i`.`product_id` AS `product_id`,`i`.`purchase_id` AS `purchase_id`,`o`.`order_no` AS `purchase_no`,`o`.`supplier_id` AS `supplier_id`,`o`.`operator_id` AS `purchase_operator_id`,`o`.`stocked_at` AS `stocked_at`,`i`.`supplier_batch_no` AS `supplier_batch_no`,`i`.`production_date` AS `production_date`,`i`.`expiry_date` AS `expiry_date`,`i`.`unit_cost` AS `unit_cost`,`i`.`received_qty` AS `received_qty`,`i`.`stock_qty` AS `stock_qty`,(case when (`i`.`manages_expiry` = 1) then (to_days(`i`.`expiry_date`) - to_days(curdate())) else NULL end) AS `remaining_days`,(case when ((`i`.`manages_expiry` = 1) and (`i`.`expiry_date` < curdate())) then 1 else 0 end) AS `is_expired`,(case when ((`i`.`manages_expiry` = 1) and (`i`.`stock_qty` > 0) and (`i`.`expiry_date` between curdate() and (curdate() + interval `p`.`near_expiry_days` day))) then 1 else 0 end) AS `is_near_expiry` from ((`purchase_item` `i` join `purchase_order` `o` on((`o`.`id` = `i`.`purchase_id`))) join `product` `p` on((`p`.`id` = `i`.`product_id`))) where ((`o`.`status` = 'IN_STOCK') and (`i`.`batch_no` is not null));

-- ----------------------------
-- View structure for v_category_inventory
-- ----------------------------
DROP VIEW IF EXISTS `v_category_inventory`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY INVOKER VIEW `v_category_inventory` AS select `c`.`id` AS `category_id`,`c`.`name` AS `category_name`,coalesce(sum(`p`.`stock_qty`),0) AS `book_qty` from (`category` `c` left join `product` `p` on((`p`.`category_id` = `c`.`id`))) group by `c`.`id`,`c`.`name`;

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
-- View structure for v_low_stock
-- ----------------------------
DROP VIEW IF EXISTS `v_low_stock`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY INVOKER VIEW `v_low_stock` AS select `v_product_inventory`.`id` AS `id`,`v_product_inventory`.`code` AS `code`,`v_product_inventory`.`barcode` AS `barcode`,`v_product_inventory`.`name` AS `name`,`v_product_inventory`.`category_id` AS `category_id`,`v_product_inventory`.`category_name` AS `category_name`,`v_product_inventory`.`specification` AS `specification`,`v_product_inventory`.`unit` AS `unit`,`v_product_inventory`.`sale_price` AS `sale_price`,`v_product_inventory`.`image_path` AS `image_path`,`v_product_inventory`.`enabled` AS `enabled`,`v_product_inventory`.`can_sell` AS `can_sell`,`v_product_inventory`.`can_purchase` AS `can_purchase`,`v_product_inventory`.`book_qty` AS `book_qty`,`v_product_inventory`.`batch_qty` AS `batch_qty`,`v_product_inventory`.`saleable_qty` AS `saleable_qty`,`v_product_inventory`.`expired_qty` AS `expired_qty`,`v_product_inventory`.`near_batch_count` AS `near_batch_count`,`v_product_inventory`.`expired_batch_count` AS `expired_batch_count`,`v_product_inventory`.`stock_lower_limit` AS `stock_lower_limit`,`v_product_inventory`.`frozen_stocktake_id` AS `frozen_stocktake_id`,`v_product_inventory`.`is_frozen` AS `is_frozen`,`v_product_inventory`.`last_stocked_at` AS `last_stocked_at`,`v_product_inventory`.`first_stocked_at` AS `first_stocked_at` from `v_product_inventory` where ((`v_product_inventory`.`enabled` = 1) and (`v_product_inventory`.`can_sell` = 1) and (`v_product_inventory`.`saleable_qty` <= `v_product_inventory`.`stock_lower_limit`));

-- BEGIN BUNDLED MEDIA SEED
-- Fresh database seed only; install files with scripts/initialize-media.py --install-only.
-- Existing databases must use the incremental --apply command, never re-import this SQL.
UPDATE product SET image_path='dfac9770-c5c4-5b5b-a19b-e26c9372888f.jpg', image_meta='{"mime":"image/jpeg","size":136772,"width":1200,"height":1800,"sha256":"d8141ea648d11b883dcc253fc5a2793a144e1c3bb0cc075181c72f7e12da4099","sourceUrl":"https://www.pexels.com/photo/5946790/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"drink"}' WHERE code IN ('P0001','HX_DRINK_500');
UPDATE product SET image_path='43c5bb41-a9db-5686-b072-daa7c1cd2c69.jpg', image_meta='{"mime":"image/jpeg","size":113014,"width":1200,"height":1800,"sha256":"3ff7722189808f5f80330c2cd91a86a6a7f6da09be6f22d982484b48a3ba72ee","sourceUrl":"https://www.pexels.com/photo/5946733/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"milk"}' WHERE code IN ('P0002','HX_MILK_250');
UPDATE product SET image_path='e1b1a95b-32b7-5509-8b84-62cd1925886a.jpg', image_meta='{"mime":"image/jpeg","size":113365,"width":1200,"height":1797,"sha256":"b5b31200430b86f4752cd42a0b838decba26d07e10f103fc1dfeeaa91906495d","sourceUrl":"https://www.pexels.com/photo/10809259/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"yogurt"}' WHERE code IN ('P0003');
UPDATE product SET image_path='58ca9274-94cc-5f49-beb3-53d6c3e6dfe2.jpg', image_meta='{"mime":"image/jpeg","size":273102,"width":1200,"height":1600,"sha256":"a18da4c1b2626d45e1ebba7f8ed232fd353d8d663307ba14c528edaeeaf3a0f9","sourceUrl":"https://www.pexels.com/photo/11214715/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"bread"}' WHERE code IN ('P0004','P0018','P0019','P0020','P0024','HX_BREAD_80');
UPDATE product SET image_path='54737c4a-31a9-5d49-9610-60adfcd5cc1a.jpg', image_meta='{"mime":"image/jpeg","size":101685,"width":1200,"height":800,"sha256":"7286149b3b52d106b9e16cacaecf7fe50b01d21f3aae16e8409303f112933f27","sourceUrl":"https://www.pexels.com/photo/7779908/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"rice"}' WHERE code IN ('P0005','HX_RICE_5KG');
UPDATE product SET image_path='121f0b1f-c972-526a-afb8-ee04faf69790.jpg', image_meta='{"mime":"image/jpeg","size":344417,"width":1200,"height":1800,"sha256":"d82a87c83fdcb7bbe496f4aa1d345cd2d8e08a23a6fbfc83ea0a6eb25acdf9d1","sourceUrl":"https://www.pexels.com/photo/6213754/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"oil"}' WHERE code IN ('P0006');
UPDATE product SET image_path='7d26380d-2202-573f-a2e5-8cb39ab31b46.jpg', image_meta='{"mime":"image/jpeg","size":133342,"width":1200,"height":900,"sha256":"4fd23941f13e5579621a5683626852efa6347c6b92b96658c28dfb409e3d7069","sourceUrl":"https://www.pexels.com/photo/35539861/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"tissue"}' WHERE code IN ('P0007','HX_TISSUE_3P');
UPDATE product SET image_path='2bcab468-83a1-547e-bbba-4063b06f604e.jpg', image_meta='{"mime":"image/jpeg","size":81025,"width":1200,"height":1800,"sha256":"2d19917ac7327fa04f219cc9c5b3aa44692e6fa122694375c4b03e3833762aaf","sourceUrl":"https://www.pexels.com/photo/8285564/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"soap"}' WHERE code IN ('P0008','P0013','P0014','P0015','P0023','HX_SOAP_100');
UPDATE product SET image_path='30e32958-e18d-5ff4-9e85-61064db5893d.jpg', image_meta='{"mime":"image/jpeg","size":86967,"width":1200,"height":1800,"sha256":"204bfce51688784c2499ad979cf3d922cad72e791cda43ddbdc9ee7db671c831","sourceUrl":"https://www.pexels.com/photo/11860563/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"water"}' WHERE code IN ('P0009','HX_WATER_550');
UPDATE product SET image_path='e46c82e2-40b0-5d4f-96bf-4d579932ea94.jpg', image_meta='{"mime":"image/jpeg","size":45416,"width":1200,"height":800,"sha256":"7ef0adbc1f845cf463f0caed0acbaa5167bbfd27bd085342e02a245865287021","sourceUrl":"https://www.pexels.com/photo/5952015/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"pen"}' WHERE code IN ('P0010','HX_PEN_05');
UPDATE product SET image_path='bdc4ba25-9153-5941-878a-7aacbc209d8e.jpg', image_meta='{"mime":"image/jpeg","size":286470,"width":1200,"height":1800,"sha256":"9f793091880a1c04e832409b235ffe13f597e2f2b1c2bfeddaa4e124deaf137a","sourceUrl":"https://www.pexels.com/photo/6940984/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"noodles"}' WHERE code IN ('P0011','HX_NOODLE_5P');
UPDATE product SET image_path='2ecc72fc-ea47-51c5-b013-55673745e638.jpg', image_meta='{"mime":"image/jpeg","size":312108,"width":1200,"height":1461,"sha256":"ce35205c742819f087275de160c4bcd8963807b0ed54a69e4b4e48c4859f1979","sourceUrl":"https://www.pexels.com/photo/31617910/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"crackers"}' WHERE code IN ('P0012','HX_BISCUIT_100');
UPDATE product SET image_path='5d727db2-e392-5df2-b4a3-1bd43477b7d2.jpg', image_meta='{"mime":"image/jpeg","size":111715,"width":1200,"height":1800,"sha256":"3ea79532f3f4dcc152bdb4bdea7cc40a70673aaa678c0ac1efb5e113993d2049","sourceUrl":"https://www.pexels.com/photo/5594320/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"notebook"}' WHERE code IN ('P0016','P0017','P0021','P0022','HX_NOTE_A5');
UPDATE product SET image_path='2d02cd03-1295-5d5e-bee7-c653e4d1aa80.jpg', image_meta='{"mime":"image/jpeg","size":167152,"width":1200,"height":1800,"sha256":"077400f87c5b1a99181fee709445454d5e0974bd42ccc88082f275a190c67f27","sourceUrl":"https://www.pexels.com/photo/5218021/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"detergent"}' WHERE code IN ('HX_DETERGENT_1KG');
UPDATE sys_user SET avatar_path='482894fd-768d-58c0-af15-cbcf6676dd81.jpg', avatar_meta='{"mime":"image/jpeg","size":53624,"width":800,"height":800,"sha256":"87a8d1457874fe1ccd46e252dac1ae7261c9534de9fe4e15391d5a1cbdea39c4","sourceUrl":"https://www.pexels.com/photo/220453/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-1"}' WHERE username IN ('admin');
UPDATE sys_user SET avatar_path='9987e67f-ed37-538b-9575-9f7830c923d1.jpg', avatar_meta='{"mime":"image/jpeg","size":53191,"width":800,"height":800,"sha256":"44ab90d879f255330b3d44050892a3cdde33a254a4458aa017d1b81d4e4a0456","sourceUrl":"https://www.pexels.com/photo/415829/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-2"}' WHERE username IN ('admin2');
UPDATE sys_user SET avatar_path='5c4d411a-5905-50b5-98b2-736200d32076.jpg', avatar_meta='{"mime":"image/jpeg","size":42812,"width":800,"height":800,"sha256":"eee803e01575dc3eb0bbc0875b262d23d8c1d5091d7388cf5c7ab36d833a476d","sourceUrl":"https://www.pexels.com/photo/614810/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-3"}' WHERE username IN ('buyer01');
UPDATE sys_user SET avatar_path='e3ab0093-bc9f-53ac-8d0c-777d3c390294.jpg', avatar_meta='{"mime":"image/jpeg","size":67052,"width":800,"height":800,"sha256":"72f420ce106799f07dffe1c3eb42515dc593f397b988cad7cf55b0f647c95535","sourceUrl":"https://www.pexels.com/photo/774909/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-4"}' WHERE username IN ('buyer02');
UPDATE sys_user SET avatar_path='23ca5159-b7b8-59cf-a79b-8fb2ab134412.jpg', avatar_meta='{"mime":"image/jpeg","size":91732,"width":800,"height":800,"sha256":"e182ac3ef38ee35a3b3ca407f36584170c8c18da1d2e8452282659a8e61f3307","sourceUrl":"https://www.pexels.com/photo/1239291/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-5"}' WHERE username IN ('cashier01');
UPDATE sys_user SET avatar_path='6c7b50d6-0830-5dd7-b90b-8373bb1ac6f0.jpg', avatar_meta='{"mime":"image/jpeg","size":65759,"width":800,"height":800,"sha256":"c7762781a446090236e654f6466cc5d0a172a4f83444e9bfbef778ad78f5aefb","sourceUrl":"https://www.pexels.com/photo/91227/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-6"}' WHERE username IN ('cashier02');
UPDATE sys_user SET avatar_path='83392ae9-2d11-5d76-88bd-db8522fdb22c.jpg', avatar_meta='{"mime":"image/jpeg","size":76773,"width":800,"height":800,"sha256":"6a9824ec47805451ef85666e2b57c00a00343bc4c94e9b2ca31d3244b4e04153","sourceUrl":"https://www.pexels.com/photo/733872/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-7"}' WHERE username IN ('disabled01');
UPDATE sys_user SET avatar_path='50b137a5-ecd5-50b3-a98b-73575d24a5f2.jpg', avatar_meta='{"mime":"image/jpeg","size":82380,"width":800,"height":800,"sha256":"4523b0fedcac4d0489223a364f75e4a7b726e9467c8345fe6a67529048b6cccb","sourceUrl":"https://www.pexels.com/photo/1222271/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-8"}' WHERE username IN ('unused01');
UPDATE sys_user SET avatar_path='482894fd-768d-58c0-af15-cbcf6676dd81.jpg',avatar_meta='{"mime":"image/jpeg","size":53624,"width":800,"height":800,"sha256":"87a8d1457874fe1ccd46e252dac1ae7261c9534de9fe4e15391d5a1cbdea39c4","sourceUrl":"https://www.pexels.com/photo/220453/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-1"}' WHERE username NOT IN ('admin','admin2','buyer01','buyer02','cashier01','cashier02','disabled01','unused01') AND MOD(CONV(RIGHT(SHA2(username,256),8),16,10),8)=0;
UPDATE sys_user SET avatar_path='9987e67f-ed37-538b-9575-9f7830c923d1.jpg',avatar_meta='{"mime":"image/jpeg","size":53191,"width":800,"height":800,"sha256":"44ab90d879f255330b3d44050892a3cdde33a254a4458aa017d1b81d4e4a0456","sourceUrl":"https://www.pexels.com/photo/415829/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-2"}' WHERE username NOT IN ('admin','admin2','buyer01','buyer02','cashier01','cashier02','disabled01','unused01') AND MOD(CONV(RIGHT(SHA2(username,256),8),16,10),8)=1;
UPDATE sys_user SET avatar_path='5c4d411a-5905-50b5-98b2-736200d32076.jpg',avatar_meta='{"mime":"image/jpeg","size":42812,"width":800,"height":800,"sha256":"eee803e01575dc3eb0bbc0875b262d23d8c1d5091d7388cf5c7ab36d833a476d","sourceUrl":"https://www.pexels.com/photo/614810/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-3"}' WHERE username NOT IN ('admin','admin2','buyer01','buyer02','cashier01','cashier02','disabled01','unused01') AND MOD(CONV(RIGHT(SHA2(username,256),8),16,10),8)=2;
UPDATE sys_user SET avatar_path='e3ab0093-bc9f-53ac-8d0c-777d3c390294.jpg',avatar_meta='{"mime":"image/jpeg","size":67052,"width":800,"height":800,"sha256":"72f420ce106799f07dffe1c3eb42515dc593f397b988cad7cf55b0f647c95535","sourceUrl":"https://www.pexels.com/photo/774909/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-4"}' WHERE username NOT IN ('admin','admin2','buyer01','buyer02','cashier01','cashier02','disabled01','unused01') AND MOD(CONV(RIGHT(SHA2(username,256),8),16,10),8)=3;
UPDATE sys_user SET avatar_path='23ca5159-b7b8-59cf-a79b-8fb2ab134412.jpg',avatar_meta='{"mime":"image/jpeg","size":91732,"width":800,"height":800,"sha256":"e182ac3ef38ee35a3b3ca407f36584170c8c18da1d2e8452282659a8e61f3307","sourceUrl":"https://www.pexels.com/photo/1239291/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-5"}' WHERE username NOT IN ('admin','admin2','buyer01','buyer02','cashier01','cashier02','disabled01','unused01') AND MOD(CONV(RIGHT(SHA2(username,256),8),16,10),8)=4;
UPDATE sys_user SET avatar_path='6c7b50d6-0830-5dd7-b90b-8373bb1ac6f0.jpg',avatar_meta='{"mime":"image/jpeg","size":65759,"width":800,"height":800,"sha256":"c7762781a446090236e654f6466cc5d0a172a4f83444e9bfbef778ad78f5aefb","sourceUrl":"https://www.pexels.com/photo/91227/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-6"}' WHERE username NOT IN ('admin','admin2','buyer01','buyer02','cashier01','cashier02','disabled01','unused01') AND MOD(CONV(RIGHT(SHA2(username,256),8),16,10),8)=5;
UPDATE sys_user SET avatar_path='83392ae9-2d11-5d76-88bd-db8522fdb22c.jpg',avatar_meta='{"mime":"image/jpeg","size":76773,"width":800,"height":800,"sha256":"6a9824ec47805451ef85666e2b57c00a00343bc4c94e9b2ca31d3244b4e04153","sourceUrl":"https://www.pexels.com/photo/733872/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-7"}' WHERE username NOT IN ('admin','admin2','buyer01','buyer02','cashier01','cashier02','disabled01','unused01') AND MOD(CONV(RIGHT(SHA2(username,256),8),16,10),8)=6;
UPDATE sys_user SET avatar_path='50b137a5-ecd5-50b3-a98b-73575d24a5f2.jpg',avatar_meta='{"mime":"image/jpeg","size":82380,"width":800,"height":800,"sha256":"4523b0fedcac4d0489223a364f75e4a7b726e9467c8345fe6a67529048b6cccb","sourceUrl":"https://www.pexels.com/photo/1222271/","licenseUrl":"https://www.pexels.com/license/","seedVersion":1,"seedKey":"avatar-8"}' WHERE username NOT IN ('admin','admin2','buyer01','buyer02','cashier01','cashier02','disabled01','unused01') AND MOD(CONV(RIGHT(SHA2(username,256),8),16,10),8)=7;
-- END BUNDLED MEDIA SEED

SET FOREIGN_KEY_CHECKS = 1;
