-- 历史分离脚本，仅供参考；唯一初始化基准为同目录supermarket_inventory.sql。
-- 超市进销存毕业设计数据库，适用于 MySQL 8.0.16+，已在 8.0.34 验证。
-- 早期配套脚本为02_seed.sql；当前初始化入口请使用supermarket_inventory.sql。
-- 不删除已有库或表；再次执行遇到同名表应停止，不能使用 --force 导入。
-- mysql 是服务器自带系统库；项目数据使用下面的独立库。
SET NAMES utf8mb4;
SET SESSION time_zone = '+08:00';
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION,ONLY_FULL_GROUP_BY';
CREATE DATABASE IF NOT EXISTS supermarket_inventory CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE supermarket_inventory;

-- 01 三种固定角色。小规模固定角色授权使用 JSON 数组，省去权限关联表。
CREATE TABLE sys_role (
  code VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY COMMENT 'ADMIN/PURCHASER/CASHIER，固定三角色',
  name VARCHAR(30) NOT NULL COMMENT '角色名称',
  permissions JSON NOT NULL COMMENT '当前权限代码数组，保存时必须是 ceiling_permissions 子集',
  default_permissions JSON NOT NULL COMMENT '恢复默认使用的权限数组',
  ceiling_permissions JSON NOT NULL COMMENT '岗位允许的最大权限集合，应用不可通过普通授权接口修改',
  data_scope VARCHAR(10) NOT NULL COMMENT 'ALL 全部业务 / OWN 本人业务；共享库存及聚合统计另按权限控制',
  auth_version BIGINT NOT NULL DEFAULT 0 COMMENT '权限变化递增，登录凭证携带并校验角色版本',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT ck_role_code CHECK (code IN ('ADMIN','PURCHASER','CASHIER')),
  CONSTRAINT ck_role_scope CHECK ((code='ADMIN' AND data_scope='ALL') OR (code<>'ADMIN' AND data_scope='OWN')),
  CONSTRAINT ck_role_json CHECK (JSON_TYPE(permissions)='ARRAY' AND JSON_TYPE(default_permissions)='ARRAY' AND JSON_TYPE(ceiling_permissions)='ARRAY'),
  CONSTRAINT ck_role_subset CHECK (JSON_CONTAINS(ceiling_permissions, permissions)=1 AND JSON_CONTAINS(ceiling_permissions, default_permissions)=1),
  CONSTRAINT ck_role_version CHECK (auth_version>=0)
) ENGINE=InnoDB COMMENT='固定角色及菜单按钮授权，权限目录见 sys_config';

-- 02 账号，头像相对路径和上传元数据直接归属用户，不额外建文件表。
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(20) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  password_hash VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'BCrypt $2b$ 或 $2a$ 格式，禁止明文/MD5',
  real_name VARCHAR(20) NOT NULL,
  phone VARCHAR(11) CHARACTER SET ascii DEFAULT NULL,
  avatar_path VARCHAR(255) DEFAULT '测试图片.jpg' COMMENT '相对路径，NULL 表示使用默认头像',
  avatar_meta JSON DEFAULT NULL COMMENT '上传大小、MIME、宽高、上传时间等；初始样例仅标记 fixture',
  role_code VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  credential_version BIGINT NOT NULL DEFAULT 0 COMMENT '密码变更、停用、角色变更、退出递增；无需会话表即可使旧凭证失效',
  last_login_at DATETIME(6) DEFAULT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_user_username (username),
  KEY ix_user_role_status (role_code,enabled),
  CONSTRAINT fk_user_role FOREIGN KEY (role_code) REFERENCES sys_role(code),
  CONSTRAINT ck_user_enabled CHECK (enabled IN (0,1)),
  CONSTRAINT ck_user_version CHECK (credential_version>=0),
  CONSTRAINT ck_user_username CHECK (CHAR_LENGTH(username) BETWEEN 4 AND 20),
  CONSTRAINT ck_user_name CHECK (CHAR_LENGTH(real_name) BETWEEN 2 AND 20),
  CONSTRAINT ck_user_password CHECK (CHAR_LENGTH(password_hash)=60 AND LEFT(password_hash,4) IN ('$2b$','$2a$','$2y$')),
  CONSTRAINT ck_user_phone CHECK (phone IS NULL OR (CHAR_LENGTH(phone)=11 AND LEFT(phone,1)='1'))
) ENGINE=InnoDB COMMENT='用户与本人资料；最后管理员保护及账号状态变更在服务事务内实现';

-- 03 默认业务参数与静态权限树；不存大模型密钥。
CREATE TABLE sys_config (
  config_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  config_value JSON NOT NULL COMMENT 'JSON 数值/数组/对象，具体类型由配置键限定',
  description VARCHAR(255) NOT NULL,
  editable TINYINT NOT NULL DEFAULT 1 COMMENT '权限目录为只读0，普通业务参数为1',
  updated_by BIGINT DEFAULT NULL,
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_config_user FOREIGN KEY (updated_by) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT ck_config_editable CHECK (editable IN (0,1))
) ENGINE=InnoDB COMMENT='默认库存下限、默认临期天数和菜单按钮权限目录';

-- 04 公共操作历史：业务状态转换、角色变更、对账多版本均保留完整前后快照。
CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_id BIGINT DEFAULT NULL,
  actor_name VARCHAR(20) NOT NULL COMMENT '操作者姓名快照',
  action VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  target_type VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'USER/ROLE/PRODUCT/SUPPLIER/PURCHASE/SALE/STOCKTAKE/AI/CONFIG',
  target_id VARCHAR(64) NOT NULL COMMENT '对象ID或角色/参数键；多态历史不作为业务主数据',
  target_no VARCHAR(64) DEFAULT NULL COMMENT '单号或编码快照',
  result VARCHAR(10) NOT NULL DEFAULT 'SUCCESS',
  reason VARCHAR(500) DEFAULT NULL,
  before_data JSON DEFAULT NULL COMMENT '对账撤回时必须保存逐行原始核对信息',
  after_data JSON DEFAULT NULL,
  request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY ix_log_object (target_type,target_id,occurred_at,id),
  KEY ix_log_actor (actor_id,occurred_at),
  KEY ix_log_action_time (action,occurred_at),
  CONSTRAINT fk_log_user FOREIGN KEY (actor_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT ck_log_result CHECK (result IN ('SUCCESS','FAILURE'))
) ENGINE=InnoDB COMMENT='只追加操作历史；JSON 保存不参与库存计算的审计快照';

-- 05 一级商品分类。
CREATE TABLE category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(20) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  name VARCHAR(30) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_category_code (code),
  UNIQUE KEY uk_category_name (name),
  CONSTRAINT ck_category_values CHECK (sort_order>=0 AND enabled IN (0,1) AND CHAR_LENGTH(name) BETWEEN 2 AND 30)
) ENGINE=InnoDB COMMENT='一级商品分类，引用后限制删除';

-- 06 供应商档案。
CREATE TABLE supplier (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  name VARCHAR(100) NOT NULL,
  contact_name VARCHAR(30) NOT NULL,
  contact_phone VARCHAR(20) NOT NULL,
  address VARCHAR(200) DEFAULT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(500) DEFAULT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_supplier_code (code),
  UNIQUE KEY uk_supplier_name (name),
  KEY ix_supplier_enabled (enabled,id),
  CONSTRAINT fk_supplier_user FOREIGN KEY (created_by) REFERENCES sys_user(id),
  CONSTRAINT ck_supplier_enabled CHECK (enabled IN (0,1)),
  CONSTRAINT ck_supplier_names CHECK (CHAR_LENGTH(name) BETWEEN 2 AND 100 AND CHAR_LENGTH(contact_name) BETWEEN 2 AND 30),
  CONSTRAINT ck_supplier_phone CHECK (CHAR_LENGTH(contact_phone) BETWEEN 6 AND 20)
) ENGINE=InnoDB COMMENT='供应商；历史采购使用单头快照';

-- 07 商品 + 库存总余额 + 当前盘点冻结指针，锁商品行作为所有库存写事务的统一入口。
CREATE TABLE product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  barcode VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '无条码用NULL，不用空串',
  name VARCHAR(100) NOT NULL,
  category_id BIGINT NOT NULL,
  specification VARCHAR(100) DEFAULT NULL,
  unit VARCHAR(10) NOT NULL,
  reference_cost DECIMAL(16,2) DEFAULT NULL,
  sale_price DECIMAL(16,2) NOT NULL,
  image_path VARCHAR(255) DEFAULT '测试图片.jpg',
  image_meta JSON DEFAULT NULL,
  manages_expiry TINYINT NOT NULL DEFAULT 0,
  shelf_life_days INT DEFAULT NULL,
  near_expiry_days INT NOT NULL DEFAULT 7,
  stock_lower_limit BIGINT NOT NULL DEFAULT 0,
  can_purchase TINYINT NOT NULL DEFAULT 1,
  can_sell TINYINT NOT NULL DEFAULT 1,
  enabled TINYINT NOT NULL DEFAULT 1,
  stock_qty BIGINT NOT NULL DEFAULT 0 COMMENT '冗余账面余额，必须等于有效批次余额之和，业务表单禁止直接修改',
  frozen_stocktake_id BIGINT DEFAULT NULL COMMENT '活动盘点指针，一个商品最多被一个盘点冻结；后置FK',
  stock_version BIGINT NOT NULL DEFAULT 0 COMMENT '每次库存变化或冻结变化递增',
  remark VARCHAR(500) DEFAULT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_product_code (code),
  UNIQUE KEY uk_product_barcode (barcode),
  KEY ix_product_category (category_id,enabled,id),
  KEY ix_product_status (enabled,can_sell,can_purchase,id),
  KEY ix_product_freeze (frozen_stocktake_id),
  CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id),
  CONSTRAINT fk_product_user FOREIGN KEY (created_by) REFERENCES sys_user(id),
  CONSTRAINT ck_product_flags CHECK (manages_expiry IN (0,1) AND can_purchase IN (0,1) AND can_sell IN (0,1) AND enabled IN (0,1)),
  CONSTRAINT ck_product_price CHECK (sale_price>0 AND (reference_cost IS NULL OR reference_cost>=0)),
  CONSTRAINT ck_product_qty CHECK (stock_qty BETWEEN 0 AND 999999999999 AND stock_lower_limit BETWEEN 0 AND 999999999999 AND stock_version>=0),
  CONSTRAINT ck_product_expiry CHECK (near_expiry_days>=0 AND ((manages_expiry=1 AND shelf_life_days IS NOT NULL AND shelf_life_days>0) OR (manages_expiry=0 AND shelf_life_days IS NULL))),
  CONSTRAINT ck_product_barcode CHECK (barcode IS NULL OR CHAR_LENGTH(barcode) BETWEEN 4 AND 32),
  CONSTRAINT ck_product_name CHECK (CHAR_LENGTH(name) BETWEEN 2 AND 100)
) ENGINE=InnoDB COMMENT='商品档案、库存余额及唯一活动冻结；不单建库存余额和冻结表';

-- 08 采购单与当前对账摘要。
CREATE TABLE purchase_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  supplier_id BIGINT NOT NULL,
  supplier_name VARCHAR(100) NOT NULL,
  supplier_contact VARCHAR(30) NOT NULL,
  supplier_phone VARCHAR(20) NOT NULL,
  supplier_address VARCHAR(200) DEFAULT NULL,
  operator_id BIGINT NOT NULL,
  operator_name VARCHAR(20) NOT NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/REJECTED/IN_STOCK/CLOSED/REVERSED',
  total_qty BIGINT NOT NULL DEFAULT 0,
  total_received_qty BIGINT NOT NULL DEFAULT 0,
  total_amount DECIMAL(16,2) NOT NULL DEFAULT 0,
  expected_arrival_date DATE DEFAULT NULL,
  arrived_at DATETIME(6) DEFAULT NULL,
  first_submitted_at DATETIME(6) DEFAULT NULL COMMENT '一旦非空即禁止硬删，撤回不清空',
  submitted_at DATETIME(6) DEFAULT NULL,
  reviewed_by BIGINT DEFAULT NULL,
  reviewer_name VARCHAR(20) DEFAULT NULL,
  reviewed_at DATETIME(6) DEFAULT NULL,
  rejection_reason VARCHAR(500) DEFAULT NULL,
  stocked_at DATETIME(6) DEFAULT NULL,
  closed_by BIGINT DEFAULT NULL,
  closed_at DATETIME(6) DEFAULT NULL,
  close_reason VARCHAR(500) DEFAULT NULL,
  reversed_by BIGINT DEFAULT NULL,
  reversed_at DATETIME(6) DEFAULT NULL,
  reverse_reason VARCHAR(500) DEFAULT NULL,
  reconcile_status VARCHAR(12) NOT NULL DEFAULT 'UNRECONCILED' COMMENT 'UNRECONCILED/DIFFERENCE/MATCHED/NA',
  external_total_amount DECIMAL(16,2) DEFAULT NULL,
  reconcile_reference VARCHAR(100) DEFAULT NULL,
  reconcile_difference VARCHAR(500) DEFAULT NULL,
  reconcile_resolution VARCHAR(500) DEFAULT NULL,
  reconciled_by BIGINT DEFAULT NULL,
  reconciled_at DATETIME(6) DEFAULT NULL,
  reconcile_version INT NOT NULL DEFAULT 0 COMMENT '每次核对/撤回递增，历史落operation_log完整JSON',
  remark VARCHAR(500) DEFAULT NULL,
  row_version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_purchase_no (order_no),
  KEY ix_purchase_owner (operator_id,status,created_at,id),
  KEY ix_purchase_status (status,submitted_at,id),
  KEY ix_purchase_supplier (supplier_id,stocked_at),
  KEY ix_purchase_reconcile (reconcile_status,stocked_at),
  CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id),
  CONSTRAINT fk_purchase_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id),
  CONSTRAINT fk_purchase_reviewer FOREIGN KEY (reviewed_by) REFERENCES sys_user(id),
  CONSTRAINT fk_purchase_closer FOREIGN KEY (closed_by) REFERENCES sys_user(id),
  CONSTRAINT fk_purchase_reverser FOREIGN KEY (reversed_by) REFERENCES sys_user(id),
  CONSTRAINT fk_purchase_reconciler FOREIGN KEY (reconciled_by) REFERENCES sys_user(id),
  CONSTRAINT ck_purchase_status CHECK (status IN ('DRAFT','PENDING','REJECTED','IN_STOCK','CLOSED','REVERSED')),
  CONSTRAINT ck_purchase_totals CHECK (total_qty>0 AND total_received_qty>=0 AND total_amount>0 AND row_version>=0 AND reconcile_version>=0),
  CONSTRAINT ck_purchase_reconcile CHECK (reconcile_status IN ('UNRECONCILED','DIFFERENCE','MATCHED','NA') AND (external_total_amount IS NULL OR external_total_amount>=0)),
  CONSTRAINT ck_purchase_instock CHECK (status NOT IN ('IN_STOCK','REVERSED') OR (stocked_at IS NOT NULL AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL AND arrived_at IS NOT NULL AND total_qty=total_received_qty)),
  CONSTRAINT ck_purchase_pending CHECK (status<>'PENDING' OR (submitted_at IS NOT NULL AND first_submitted_at IS NOT NULL AND arrived_at IS NOT NULL AND total_qty=total_received_qty)),
  CONSTRAINT ck_purchase_reversed CHECK ((status='REVERSED' AND reversed_at IS NOT NULL AND reversed_by IS NOT NULL AND reverse_reason IS NOT NULL AND CHAR_LENGTH(reverse_reason)>=2 AND reconcile_status='NA') OR (status<>'REVERSED' AND reversed_at IS NULL AND reconcile_status<>'NA')),
  CONSTRAINT ck_purchase_matched CHECK (reconcile_status<>'MATCHED' OR (status='IN_STOCK' AND external_total_amount IS NOT NULL AND external_total_amount=total_amount AND reconciled_by IS NOT NULL AND reconciled_at IS NOT NULL)),
  CONSTRAINT ck_purchase_difference CHECK (reconcile_status<>'DIFFERENCE' OR (status='IN_STOCK' AND external_total_amount IS NOT NULL AND reconcile_difference IS NOT NULL AND CHAR_LENGTH(reconcile_difference)>=2)),
  CONSTRAINT ck_purchase_closed CHECK (status<>'CLOSED' OR (closed_at IS NOT NULL AND closed_by IS NOT NULL AND close_reason IS NOT NULL)),
  CONSTRAINT ck_purchase_rejected CHECK (status<>'REJECTED' OR (reviewed_at IS NOT NULL AND rejection_reason IS NOT NULL AND CHAR_LENGTH(rejection_reason)>=2))
) ENGINE=InnoDB COMMENT='采购单与当前对账摘要；核对撤回历史不覆盖，通过operation_log保留';

-- 09 一条采购明细只产生一个库存批次，二者合并；未入库时 batch_no/stock_qty 为NULL。
CREATE TABLE purchase_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  purchase_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  product_id BIGINT NOT NULL,
  product_code VARCHAR(32) NOT NULL,
  product_barcode VARCHAR(32) DEFAULT NULL,
  product_name VARCHAR(100) NOT NULL,
  category_id BIGINT NOT NULL,
  category_name VARCHAR(30) NOT NULL,
  specification VARCHAR(100) DEFAULT NULL,
  unit VARCHAR(10) NOT NULL,
  manages_expiry TINYINT NOT NULL,
  quantity INT NOT NULL,
  received_qty INT DEFAULT NULL COMMENT '未登记到货为NULL，入库前必须等于quantity',
  unit_cost DECIMAL(16,2) NOT NULL,
  line_amount DECIMAL(16,2) GENERATED ALWAYS AS (quantity*unit_cost) STORED,
  supplier_batch_no VARCHAR(64) DEFAULT NULL,
  production_date DATE DEFAULT NULL,
  expiry_date DATE DEFAULT NULL,
  batch_no VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '系统审核入库时生成，唯一',
  stock_qty BIGINT DEFAULT NULL COMMENT '批次当前在库量，未入库NULL，已入库及撤销批次为非负数',
  external_qty INT DEFAULT NULL COMMENT '最近一次外部核对数量，历史保存在对账日志',
  external_unit_cost DECIMAL(16,2) DEFAULT NULL,
  external_line_amount DECIMAL(16,2) GENERATED ALWAYS AS (external_qty*external_unit_cost) STORED,
  reconcile_note VARCHAR(500) DEFAULT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_purchase_line (purchase_id,line_no),
  UNIQUE KEY uk_batch_no (batch_no),
  UNIQUE KEY uk_batch_product (id,product_id),
  KEY ix_batch_fefo (product_id,expiry_date,id),
  KEY ix_batch_expiry (expiry_date,product_id),
  CONSTRAINT fk_pitem_order FOREIGN KEY (purchase_id) REFERENCES purchase_order(id),
  CONSTRAINT fk_pitem_product FOREIGN KEY (product_id) REFERENCES product(id),
  CONSTRAINT fk_pitem_category FOREIGN KEY (category_id) REFERENCES category(id),
  CONSTRAINT ck_pitem_qty CHECK (quantity BETWEEN 1 AND 999999 AND (received_qty IS NULL OR received_qty BETWEEN 0 AND 999999)),
  CONSTRAINT ck_pitem_price CHECK (unit_cost>0 AND line_no>0),
  CONSTRAINT ck_pitem_batch CHECK ((batch_no IS NULL AND stock_qty IS NULL) OR (batch_no IS NOT NULL AND stock_qty IS NOT NULL AND stock_qty BETWEEN 0 AND 999999999999 AND received_qty IS NOT NULL AND received_qty=quantity)),
  CONSTRAINT ck_pitem_expiry CHECK (manages_expiry IN (0,1) AND ((manages_expiry=0 AND production_date IS NULL AND expiry_date IS NULL) OR (manages_expiry=1 AND (production_date IS NULL OR expiry_date IS NULL OR expiry_date>=production_date)))),
  CONSTRAINT ck_pitem_stock_dates CHECK (batch_no IS NULL OR manages_expiry=0 OR (production_date IS NOT NULL AND expiry_date IS NOT NULL)),
  CONSTRAINT ck_pitem_external CHECK ((external_qty IS NULL AND external_unit_cost IS NULL) OR (external_qty IS NOT NULL AND external_unit_cost IS NOT NULL AND external_qty BETWEEN 0 AND 999999 AND external_unit_cost>=0))
) ENGINE=InnoDB COMMENT='采购明细与其唯一库存批次合并，业务追溯仍使用真实外键';

-- 10 销售只保存已完成单；未结算购物内容不落销售表。
CREATE TABLE sale_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  request_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '确认销售唯一幂等标识，重试返回原单',
  request_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '请求规范化摘要，同key不同内容必须拒绝',
  cashier_id BIGINT NOT NULL,
  cashier_name VARCHAR(20) NOT NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'COMPLETED',
  total_qty BIGINT NOT NULL,
  total_amount DECIMAL(16,2) NOT NULL,
  payment_method VARCHAR(12) NOT NULL COMMENT 'CASH/MOCK_EPAY',
  received_amount DECIMAL(16,2) NOT NULL,
  change_amount DECIMAL(16,2) GENERATED ALWAYS AS (received_amount-total_amount) STORED,
  completed_at DATETIME(6) NOT NULL,
  corrected_by BIGINT DEFAULT NULL,
  corrector_name VARCHAR(20) DEFAULT NULL,
  corrected_at DATETIME(6) DEFAULT NULL,
  correction_reason VARCHAR(500) DEFAULT NULL,
  row_version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_sale_no (order_no),
  UNIQUE KEY uk_sale_request (request_key),
  KEY ix_sale_cashier (cashier_id,completed_at,id),
  KEY ix_sale_time (completed_at,id),
  KEY ix_sale_correction (corrected_at,id),
  CONSTRAINT fk_sale_cashier FOREIGN KEY (cashier_id) REFERENCES sys_user(id),
  CONSTRAINT fk_sale_corrector FOREIGN KEY (corrected_by) REFERENCES sys_user(id),
  CONSTRAINT ck_sale_status CHECK (status IN ('COMPLETED','CORRECTED')),
  CONSTRAINT ck_sale_amount CHECK (total_qty>0 AND total_amount>0 AND received_amount>=total_amount AND row_version>=0),
  CONSTRAINT ck_sale_payment CHECK (payment_method IN ('CASH','MOCK_EPAY') AND (payment_method<>'MOCK_EPAY' OR received_amount=total_amount)),
  CONSTRAINT ck_sale_corrected CHECK ((status='COMPLETED' AND corrected_at IS NULL AND corrected_by IS NULL) OR (status='CORRECTED' AND corrected_at IS NOT NULL AND corrected_by IS NOT NULL AND correction_reason IS NOT NULL AND CHAR_LENGTH(correction_reason)>=2 AND DATE(corrected_at)=DATE(completed_at) AND corrected_at>=completed_at))
) ENGINE=InnoDB COMMENT='已完成销售及最多一次当天整单纠错，保留原金额和时间';

-- 11 销售商品行；快照避免档案或售价修改污染历史统计。
CREATE TABLE sale_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sale_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  product_id BIGINT NOT NULL,
  product_code VARCHAR(32) NOT NULL,
  product_barcode VARCHAR(32) DEFAULT NULL,
  product_name VARCHAR(100) NOT NULL,
  category_id BIGINT NOT NULL,
  category_name VARCHAR(30) NOT NULL,
  specification VARCHAR(100) DEFAULT NULL,
  unit VARCHAR(10) NOT NULL,
  unit_price DECIMAL(16,2) NOT NULL,
  quantity INT NOT NULL,
  line_amount DECIMAL(16,2) GENERATED ALWAYS AS (unit_price*quantity) STORED,
  UNIQUE KEY uk_sale_line (sale_id,line_no),
  UNIQUE KEY uk_sale_product (sale_id,product_id),
  UNIQUE KEY uk_sitem_product (id,product_id),
  KEY ix_sitem_product (product_id,sale_id),
  CONSTRAINT fk_sitem_sale FOREIGN KEY (sale_id) REFERENCES sale_order(id),
  CONSTRAINT fk_sitem_product FOREIGN KEY (product_id) REFERENCES product(id),
  CONSTRAINT fk_sitem_category FOREIGN KEY (category_id) REFERENCES category(id),
  CONSTRAINT ck_sitem_values CHECK (line_no>0 AND unit_price>0 AND quantity BETWEEN 1 AND 999999)
) ENGINE=InnoDB COMMENT='销售商品明细，不包含收银角色无权查看的采购成本';

-- 12 商品行跨批次分配，复合外键确保销售商品与来源批次商品相同。
CREATE TABLE sale_allocation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sale_item_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  batch_item_id BIGINT NOT NULL COMMENT 'purchase_item.id，即系统库存批次ID',
  quantity INT NOT NULL,
  UNIQUE KEY uk_allocation_line_batch (sale_item_id,batch_item_id),
  UNIQUE KEY uk_allocation_source (id,batch_item_id,product_id),
  CONSTRAINT fk_alloc_sitem FOREIGN KEY (sale_item_id,product_id) REFERENCES sale_item(id,product_id),
  CONSTRAINT fk_alloc_batch FOREIGN KEY (batch_item_id,product_id) REFERENCES purchase_item(id,product_id),
  CONSTRAINT ck_allocation_qty CHECK (quantity BETWEEN 1 AND 999999)
) ENGINE=InnoDB COMMENT='按FEFO/FIFO扣库明细，也是销售纠错恢复批次的依据';

-- 13 盘点单，冻结指针在product上，历史范围和冻结时间由本单及明细保留。
CREATE TABLE stocktake_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  operator_id BIGINT NOT NULL,
  operator_name VARCHAR(20) NOT NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'COUNTING' COMMENT 'COUNTING/PENDING/REJECTED/COMPLETED/CLOSED',
  started_at DATETIME(6) NOT NULL,
  submitted_at DATETIME(6) DEFAULT NULL,
  reviewed_by BIGINT DEFAULT NULL,
  reviewer_name VARCHAR(20) DEFAULT NULL,
  reviewed_at DATETIME(6) DEFAULT NULL,
  rejection_reason VARCHAR(500) DEFAULT NULL,
  completed_at DATETIME(6) DEFAULT NULL,
  closed_by BIGINT DEFAULT NULL,
  closed_at DATETIME(6) DEFAULT NULL,
  close_reason VARCHAR(500) DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  row_version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_stocktake_no (order_no),
  KEY ix_stocktake_owner (operator_id,status,started_at),
  KEY ix_stocktake_status (status,submitted_at),
  CONSTRAINT fk_stocktake_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id),
  CONSTRAINT fk_stocktake_reviewer FOREIGN KEY (reviewed_by) REFERENCES sys_user(id),
  CONSTRAINT fk_stocktake_closer FOREIGN KEY (closed_by) REFERENCES sys_user(id),
  CONSTRAINT ck_stocktake_status CHECK (status IN ('COUNTING','PENDING','REJECTED','COMPLETED','CLOSED')),
  CONSTRAINT ck_stocktake_pending CHECK (status NOT IN ('PENDING','REJECTED','COMPLETED') OR submitted_at IS NOT NULL),
  CONSTRAINT ck_stocktake_done CHECK (status<>'COMPLETED' OR (completed_at IS NOT NULL AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL)),
  CONSTRAINT ck_stocktake_reject CHECK (status<>'REJECTED' OR (rejection_reason IS NOT NULL AND CHAR_LENGTH(rejection_reason)>=2 AND reviewed_at IS NOT NULL)),
  CONSTRAINT ck_stocktake_closed CHECK (status<>'CLOSED' OR (closed_at IS NOT NULL AND closed_by IS NOT NULL AND close_reason IS NOT NULL)),
  CONSTRAINT ck_stocktake_version CHECK (row_version>=0)
) ENGINE=InnoDB COMMENT='盘点任务、审核及冻结生命周期';

ALTER TABLE product ADD CONSTRAINT fk_product_freeze FOREIGN KEY (frozen_stocktake_id) REFERENCES stocktake_order(id);

-- 14 盘点批次明细，NULL实盘代表未填，0代表实际清点为零。
CREATE TABLE stocktake_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stocktake_id BIGINT NOT NULL,
  batch_item_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  book_qty BIGINT NOT NULL,
  actual_qty BIGINT DEFAULT NULL,
  difference_qty BIGINT GENERATED ALWAYS AS (actual_qty-book_qty) STORED,
  reason_type VARCHAR(12) DEFAULT NULL COMMENT 'COUNT_ERROR/LOSS/EXPIRY/OTHER',
  reason_note VARCHAR(500) DEFAULT NULL,
  UNIQUE KEY uk_stocktake_batch (stocktake_id,batch_item_id),
  UNIQUE KEY uk_stitem_source (id,batch_item_id,product_id),
  KEY ix_stocktake_product (product_id,stocktake_id),
  CONSTRAINT fk_stitem_order FOREIGN KEY (stocktake_id) REFERENCES stocktake_order(id),
  CONSTRAINT fk_stitem_batch FOREIGN KEY (batch_item_id,product_id) REFERENCES purchase_item(id,product_id),
  CONSTRAINT ck_stitem_qty CHECK (book_qty BETWEEN 0 AND 999999999999 AND (actual_qty IS NULL OR actual_qty BETWEEN 0 AND 999999999999)),
  CONSTRAINT ck_stitem_reason_type CHECK (reason_type IS NULL OR reason_type IN ('COUNT_ERROR','LOSS','EXPIRY','OTHER')),
  CONSTRAINT ck_stitem_reason CHECK (actual_qty IS NULL OR actual_qty=book_qty OR (reason_type IS NOT NULL AND reason_note IS NOT NULL AND CHAR_LENGTH(reason_note)>=2))
) ENGINE=InnoDB COMMENT='批次账面快照与实盘差异；不允许未知来源盘盈';

-- 15 统一库存账本，同时承担真实正反向业务事件，不另建冲销事件表。
CREATE TABLE stock_movement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_type VARCHAR(20) NOT NULL COMMENT 'PURCHASE_IN/PURCHASE_REVERSE/SALE_OUT/SALE_CORRECTION/STOCKTAKE_GAIN/STOCKTAKE_LOSS',
  product_id BIGINT NOT NULL,
  batch_item_id BIGINT NOT NULL,
  sale_allocation_id BIGINT DEFAULT NULL,
  stocktake_item_id BIGINT DEFAULT NULL,
  source_key BIGINT GENERATED ALWAYS AS (COALESCE(sale_allocation_id,stocktake_item_id,batch_item_id)) STORED,
  business_no VARCHAR(40) NOT NULL,
  delta_qty BIGINT NOT NULL,
  batch_before BIGINT NOT NULL,
  batch_after BIGINT NOT NULL,
  product_before BIGINT NOT NULL,
  product_after BIGINT NOT NULL,
  actor_id BIGINT NOT NULL,
  actor_name VARCHAR(20) NOT NULL,
  reason VARCHAR(500) DEFAULT NULL,
  occurred_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_movement_once (event_type,source_key) COMMENT '同类来源只能入账一次，正反向类型不同',
  KEY ix_movement_product (product_id,occurred_at,id),
  KEY ix_movement_batch (batch_item_id,id),
  KEY ix_movement_business (business_no,event_type),
  CONSTRAINT fk_movement_batch FOREIGN KEY (batch_item_id,product_id) REFERENCES purchase_item(id,product_id),
  CONSTRAINT fk_movement_allocation FOREIGN KEY (sale_allocation_id,batch_item_id,product_id) REFERENCES sale_allocation(id,batch_item_id,product_id),
  CONSTRAINT fk_movement_stocktake FOREIGN KEY (stocktake_item_id,batch_item_id,product_id) REFERENCES stocktake_item(id,batch_item_id,product_id),
  CONSTRAINT fk_movement_actor FOREIGN KEY (actor_id) REFERENCES sys_user(id),
  CONSTRAINT ck_movement_type CHECK (event_type IN ('PURCHASE_IN','PURCHASE_REVERSE','SALE_OUT','SALE_CORRECTION','STOCKTAKE_GAIN','STOCKTAKE_LOSS')),
  CONSTRAINT ck_movement_source CHECK ((event_type IN ('PURCHASE_IN','PURCHASE_REVERSE') AND sale_allocation_id IS NULL AND stocktake_item_id IS NULL) OR (event_type IN ('SALE_OUT','SALE_CORRECTION') AND sale_allocation_id IS NOT NULL AND stocktake_item_id IS NULL) OR (event_type IN ('STOCKTAKE_GAIN','STOCKTAKE_LOSS') AND stocktake_item_id IS NOT NULL AND sale_allocation_id IS NULL)),
  CONSTRAINT ck_movement_math CHECK (delta_qty<>0 AND batch_before>=0 AND batch_after>=0 AND product_before>=0 AND product_after>=0 AND batch_before+delta_qty=batch_after AND product_before+delta_qty=product_after),
  CONSTRAINT ck_movement_sign CHECK ((event_type IN ('PURCHASE_IN','SALE_CORRECTION','STOCKTAKE_GAIN') AND delta_qty>0) OR (event_type IN ('PURCHASE_REVERSE','SALE_OUT','STOCKTAKE_LOSS') AND delta_qty<0))
) ENGINE=InnoDB COMMENT='不可任意编辑删除的库存流水，正反事件构建统计视图，防止冲销覆盖历史';

-- 16 智能建议与降级记录。
CREATE TABLE ai_suggestion (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  suggestion_type VARCHAR(12) NOT NULL COMMENT 'INVENTORY/SALES',
  operator_id BIGINT NOT NULL,
  operator_name VARCHAR(20) NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  filter_data JSON NOT NULL COMMENT '分类/商品ID筛选，不含联系人、账号密码等',
  input_summary JSON NOT NULL COMMENT '实际发送的最小聚合业务快照',
  snapshot_at DATETIME(6) NOT NULL,
  provider VARCHAR(40) NOT NULL,
  model_name VARCHAR(100) NOT NULL,
  is_fixture TINYINT NOT NULL DEFAULT 0 COMMENT '1仅初始测试夹具，界面必须标注，不能当真实API成功证据',
  status VARCHAR(10) NOT NULL COMMENT 'PENDING/SUCCESS/FAILURE',
  content MEDIUMTEXT DEFAULT NULL,
  error_code VARCHAR(40) DEFAULT NULL,
  error_message VARCHAR(500) DEFAULT NULL,
  duration_ms INT DEFAULT NULL,
  requested_at DATETIME(6) NOT NULL,
  finished_at DATETIME(6) DEFAULT NULL,
  prompt_tokens BIGINT NULL DEFAULT NULL COMMENT '服务商输入Token，NULL为未报告',
  completion_tokens BIGINT NULL DEFAULT NULL,
  total_tokens BIGINT NULL DEFAULT NULL,
  cached_tokens BIGINT NULL DEFAULT NULL,
  provider_request_id VARCHAR(128) NULL DEFAULT NULL,
  KEY ix_ai_usage (is_fixture,requested_at,model_name),
  CONSTRAINT ck_ai_usage CHECK ((prompt_tokens IS NULL AND completion_tokens IS NULL AND total_tokens IS NULL AND cached_tokens IS NULL) OR (prompt_tokens IS NOT NULL AND completion_tokens IS NOT NULL AND total_tokens IS NOT NULL AND prompt_tokens>=0 AND completion_tokens>=0 AND total_tokens=prompt_tokens+completion_tokens AND (cached_tokens IS NULL OR (cached_tokens>=0 AND cached_tokens<=prompt_tokens)))),
  UNIQUE KEY uk_ai_request (request_key),
  KEY ix_ai_type_status (suggestion_type,status,requested_at),
  KEY ix_ai_owner (operator_id,requested_at),
  CONSTRAINT fk_ai_user FOREIGN KEY (operator_id) REFERENCES sys_user(id),
  CONSTRAINT ck_ai_type CHECK (suggestion_type IN ('INVENTORY','SALES')),
  CONSTRAINT ck_ai_status CHECK (status IN ('PENDING','SUCCESS','FAILURE')),
  CONSTRAINT ck_ai_time CHECK (period_end>=period_start AND (duration_ms IS NULL OR duration_ms>=0)),
  CONSTRAINT ck_ai_fixture CHECK (is_fixture IN (0,1) AND (is_fixture=0 OR provider='FIXTURE')),
  CONSTRAINT ck_ai_result CHECK ((status='PENDING' AND content IS NULL AND finished_at IS NULL) OR (status='SUCCESS' AND content IS NOT NULL AND CHAR_LENGTH(TRIM(content))>0 AND finished_at IS NOT NULL AND error_code IS NULL) OR (status='FAILURE' AND content IS NULL AND error_code IS NOT NULL AND error_message IS NOT NULL AND finished_at IS NOT NULL))
) ENGINE=InnoDB COMMENT='模型输出与失败记录；不含密钥，测试数据不伪装真实模型调用';

-- 下列视图不增加基础表数量；它们统一库存、日期预警、事件统计口径。
CREATE SQL SECURITY INVOKER VIEW v_batch_inventory AS
SELECT i.id batch_id,i.batch_no,i.product_id,i.purchase_id,o.order_no purchase_no,o.supplier_id,
       o.operator_id purchase_operator_id,o.stocked_at,i.supplier_batch_no,i.production_date,i.expiry_date,
       i.unit_cost,i.received_qty,i.stock_qty,
       CASE WHEN i.manages_expiry=1 THEN DATEDIFF(i.expiry_date,CURRENT_DATE()) ELSE NULL END remaining_days,
       CASE WHEN i.manages_expiry=1 AND i.expiry_date<CURRENT_DATE() THEN 1 ELSE 0 END is_expired,
       CASE WHEN i.manages_expiry=1 AND i.stock_qty>0 AND i.expiry_date BETWEEN CURRENT_DATE() AND DATE_ADD(CURRENT_DATE(),INTERVAL p.near_expiry_days DAY) THEN 1 ELSE 0 END is_near_expiry
FROM purchase_item i JOIN purchase_order o ON o.id=i.purchase_id JOIN product p ON p.id=i.product_id
WHERE o.status='IN_STOCK' AND i.batch_no IS NOT NULL;

CREATE SQL SECURITY INVOKER VIEW v_product_inventory AS
SELECT p.id,p.code,p.barcode,p.name,p.category_id,c.name category_name,p.specification,p.unit,p.sale_price,p.image_path,
       p.enabled,p.can_sell,p.can_purchase,p.stock_qty book_qty,
       COALESCE(b.batch_qty,0) batch_qty,
       CASE WHEN p.enabled=1 AND p.can_sell=1 THEN COALESCE(b.unexpired_qty,0) ELSE 0 END saleable_qty,
       COALESCE(b.expired_qty,0) expired_qty,COALESCE(b.near_batch_count,0) near_batch_count,
       COALESCE(b.expired_batch_count,0) expired_batch_count,p.stock_lower_limit,p.frozen_stocktake_id,
       CASE WHEN p.frozen_stocktake_id IS NULL THEN 0 ELSE 1 END is_frozen,
       b.last_stocked_at,b.first_stocked_at
FROM product p JOIN category c ON c.id=p.category_id LEFT JOIN (
 SELECT product_id,SUM(stock_qty) batch_qty,
        SUM(CASE WHEN is_expired=0 THEN stock_qty ELSE 0 END) unexpired_qty,
        SUM(CASE WHEN is_expired=1 THEN stock_qty ELSE 0 END) expired_qty,
        SUM(is_near_expiry) near_batch_count,
        SUM(CASE WHEN is_expired=1 AND stock_qty>0 THEN 1 ELSE 0 END) expired_batch_count,
        MAX(stocked_at) last_stocked_at,MIN(stocked_at) first_stocked_at
 FROM v_batch_inventory GROUP BY product_id
) b ON b.product_id=p.id;

CREATE SQL SECURITY INVOKER VIEW v_low_stock AS
SELECT * FROM v_product_inventory WHERE enabled=1 AND can_sell=1 AND saleable_qty<=stock_lower_limit;

-- 只含收银角色允许的库存字段；仍需服务端授权，视图本身不是登录鉴权。
CREATE SQL SECURITY INVOKER VIEW v_cashier_inventory AS
SELECT id,code,barcode,name,category_id,category_name,specification,unit,sale_price,image_path,saleable_qty,is_frozen
FROM v_product_inventory;

-- 销售原事件及冲销事件，不按最终状态过滤原事件，不按批次行重复计销售额。
CREATE SQL SECURITY INVOKER VIEW v_sale_event AS
SELECT s.id sale_id,s.order_no,s.cashier_id,s.cashier_name,i.id sale_item_id,i.product_id,i.category_id,i.category_name,
       i.product_code,i.product_name,'SALE' event_type,s.completed_at event_at,i.quantity signed_qty,i.line_amount signed_amount
FROM sale_order s JOIN sale_item i ON i.sale_id=s.id
UNION ALL
SELECT s.id,s.order_no,s.cashier_id,s.cashier_name,i.id,i.product_id,i.category_id,i.category_name,
       i.product_code,i.product_name,'CORRECTION',s.corrected_at,-i.quantity,-i.line_amount
FROM sale_order s JOIN sale_item i ON i.sale_id=s.id WHERE s.status='CORRECTED';

CREATE SQL SECURITY INVOKER VIEW v_purchase_event AS
SELECT o.id purchase_id,o.order_no,o.supplier_id,o.supplier_name,o.operator_id,i.id purchase_item_id,i.product_id,
       'PURCHASE' event_type,o.stocked_at event_at,i.received_qty signed_qty,i.line_amount signed_amount
FROM purchase_order o JOIN purchase_item i ON i.purchase_id=o.id WHERE o.status IN ('IN_STOCK','REVERSED')
UNION ALL
SELECT o.id,o.order_no,o.supplier_id,o.supplier_name,o.operator_id,i.id,i.product_id,
       'REVERSAL',o.reversed_at,-i.received_qty,-i.line_amount
FROM purchase_order o JOIN purchase_item i ON i.purchase_id=o.id WHERE o.status='REVERSED';

CREATE SQL SECURITY INVOKER VIEW v_category_inventory AS
SELECT c.id category_id,c.name category_name,COALESCE(SUM(p.stock_qty),0) book_qty
FROM category c LEFT JOIN product p ON p.category_id=c.id GROUP BY c.id,c.name;

-- 历史对账入口，日志中 before_data/after_data 含逐行核对信息。
CREATE SQL SECURITY INVOKER VIEW v_reconcile_history AS
SELECT id,actor_id,actor_name,target_id purchase_id,target_no order_no,action,reason,before_data,after_data,occurred_at
FROM operation_log WHERE target_type='PURCHASE' AND action IN ('RECONCILE_DIFFERENCE','RECONCILE_MATCH','RECONCILE_WITHDRAW');

SELECT 'schema_ready' result,DATABASE() database_name,COUNT(*) base_tables
FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE';
