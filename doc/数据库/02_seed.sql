-- 历史分离脚本，仅供参考；唯一初始化基准为同目录supermarket_inventory.sql。
-- 全量功能测试初始数据；仅向空项目库导入一次，不覆盖已有数据。
-- 全部测试用户密码为 Test123456，使用实际 BCrypt 摘要。
-- 相对日期按导入时北京时间计算；临期、到期当天、过期数据不会因文件生成日固定失效。
-- 成功AI记录是明确标记is_fixture=1的测试夹具，不能当作真实API调用证据。
SET NAMES utf8mb4;
SET SESSION time_zone = '+08:00';
USE supermarket_inventory;
SET @seed_now = NOW(6);
SET @seed_day = CURRENT_DATE();
SET @month_start = CAST(DATE_FORMAT(@seed_day,'%Y-%m-01') AS DATE);
SET @history_start = DATE_SUB(@month_start,INTERVAL 2 MONTH);
START TRANSACTION;

-- sys_role: 3 条
INSERT INTO sys_role (code,name,permissions,default_permissions,ceiling_permissions,data_scope,created_at,updated_at) VALUES
('ADMIN','超市管理员','["common","common.dashboard","common.profile","common.avatar","common.password","common.logout","system","system.user","system.role","system.config","system.log","system.user.create","system.user.edit","system.user.enable","system.user.reset_password","system.user.delete","system.role.save","system.role.restore","system.config.save","base","base.category","base.category.create","base.category.edit","base.category.enable","base.category.delete","base.product","base.product.create","base.product.edit","base.product.enable","base.product.delete","base.supplier","base.supplier.create","base.supplier.edit","base.supplier.enable","base.supplier.delete","base.product.image","product.purchase_lookup","product.sale_lookup","purchase","purchase.order","purchase.order.create","purchase.order.edit","purchase.order.delete_draft","purchase.order.submit","purchase.order.withdraw","purchase.order.close","purchase.order.reconcile","purchase.order.review","purchase.order.reject","purchase.order.reverse","sale","sale.order","sale.order.complete","sale.order.correct","inventory","inventory.query","inventory.batch","inventory.movement","inventory.warning","inventory.stocktake","inventory.stocktake.start","inventory.stocktake.save","inventory.stocktake.submit","inventory.stocktake.withdraw","inventory.stocktake.close","inventory.stocktake.review","inventory.stocktake.reject","statistics","statistics.sales","statistics.inventory","ai","ai.inventory","ai.sales"]','["common","common.dashboard","common.profile","common.avatar","common.password","common.logout","system","system.user","system.role","system.config","system.log","system.user.create","system.user.edit","system.user.enable","system.user.reset_password","system.user.delete","system.role.save","system.role.restore","system.config.save","base","base.category","base.category.create","base.category.edit","base.category.enable","base.category.delete","base.product","base.product.create","base.product.edit","base.product.enable","base.product.delete","base.supplier","base.supplier.create","base.supplier.edit","base.supplier.enable","base.supplier.delete","base.product.image","product.purchase_lookup","product.sale_lookup","purchase","purchase.order","purchase.order.create","purchase.order.edit","purchase.order.delete_draft","purchase.order.submit","purchase.order.withdraw","purchase.order.close","purchase.order.reconcile","purchase.order.review","purchase.order.reject","purchase.order.reverse","sale","sale.order","sale.order.complete","sale.order.correct","inventory","inventory.query","inventory.batch","inventory.movement","inventory.warning","inventory.stocktake","inventory.stocktake.start","inventory.stocktake.save","inventory.stocktake.submit","inventory.stocktake.withdraw","inventory.stocktake.close","inventory.stocktake.review","inventory.stocktake.reject","statistics","statistics.sales","statistics.inventory","ai","ai.inventory","ai.sales"]','["common","common.dashboard","common.profile","common.avatar","common.password","common.logout","system","system.user","system.role","system.config","system.log","system.user.create","system.user.edit","system.user.enable","system.user.reset_password","system.user.delete","system.role.save","system.role.restore","system.config.save","base","base.category","base.category.create","base.category.edit","base.category.enable","base.category.delete","base.product","base.product.create","base.product.edit","base.product.enable","base.product.delete","base.supplier","base.supplier.create","base.supplier.edit","base.supplier.enable","base.supplier.delete","base.product.image","product.purchase_lookup","product.sale_lookup","purchase","purchase.order","purchase.order.create","purchase.order.edit","purchase.order.delete_draft","purchase.order.submit","purchase.order.withdraw","purchase.order.close","purchase.order.reconcile","purchase.order.review","purchase.order.reject","purchase.order.reverse","sale","sale.order","sale.order.complete","sale.order.correct","inventory","inventory.query","inventory.batch","inventory.movement","inventory.warning","inventory.stocktake","inventory.stocktake.start","inventory.stocktake.save","inventory.stocktake.submit","inventory.stocktake.withdraw","inventory.stocktake.close","inventory.stocktake.review","inventory.stocktake.reject","statistics","statistics.sales","statistics.inventory","ai","ai.inventory","ai.sales"]','ALL',@history_start,@history_start),
('PURCHASER','采购入库操作员','["common","common.dashboard","common.profile","common.avatar","common.password","common.logout","base","base.supplier","base.supplier.create","base.supplier.edit","base.supplier.enable","product.purchase_lookup","purchase","purchase.order","purchase.order.create","purchase.order.edit","purchase.order.delete_draft","purchase.order.submit","purchase.order.withdraw","purchase.order.close","purchase.order.reconcile","inventory","inventory.query","inventory.batch","inventory.movement","inventory.warning","inventory.stocktake","inventory.stocktake.start","inventory.stocktake.save","inventory.stocktake.submit","inventory.stocktake.withdraw","inventory.stocktake.close","statistics","statistics.inventory","ai","ai.inventory"]','["common","common.dashboard","common.profile","common.avatar","common.password","common.logout","base","base.supplier","base.supplier.create","base.supplier.edit","base.supplier.enable","product.purchase_lookup","purchase","purchase.order","purchase.order.create","purchase.order.edit","purchase.order.delete_draft","purchase.order.submit","purchase.order.withdraw","purchase.order.close","purchase.order.reconcile","inventory","inventory.query","inventory.batch","inventory.movement","inventory.warning","inventory.stocktake","inventory.stocktake.start","inventory.stocktake.save","inventory.stocktake.submit","inventory.stocktake.withdraw","inventory.stocktake.close","statistics","statistics.inventory","ai","ai.inventory"]','["common","common.dashboard","common.profile","common.avatar","common.password","common.logout","base","base.supplier","base.supplier.create","base.supplier.edit","base.supplier.enable","product.purchase_lookup","purchase","purchase.order","purchase.order.create","purchase.order.edit","purchase.order.delete_draft","purchase.order.submit","purchase.order.withdraw","purchase.order.close","purchase.order.reconcile","inventory","inventory.query","inventory.batch","inventory.movement","inventory.warning","inventory.stocktake","inventory.stocktake.start","inventory.stocktake.save","inventory.stocktake.submit","inventory.stocktake.withdraw","inventory.stocktake.close","statistics","statistics.inventory","ai","ai.inventory"]','OWN',@history_start,@history_start),
('CASHIER','前台收银操作员','["common","common.dashboard","common.profile","common.avatar","common.password","common.logout","product.sale_lookup","sale","sale.order","sale.order.complete","inventory","inventory.query","statistics","statistics.sales"]','["common","common.dashboard","common.profile","common.avatar","common.password","common.logout","product.sale_lookup","sale","sale.order","sale.order.complete","inventory","inventory.query","statistics","statistics.sales"]','["common","common.dashboard","common.profile","common.avatar","common.password","common.logout","product.sale_lookup","sale","sale.order","sale.order.complete","inventory","inventory.query","statistics","statistics.sales"]','OWN',@history_start,@history_start);

-- sys_user: 8 条
INSERT INTO sys_user (id,username,password_hash,real_name,phone,avatar_path,avatar_meta,role_code,enabled,created_at,updated_at) VALUES
(1,'admin','$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm','系统管理员','13800000001','测试图片.jpg','{"fixture":true,"note":"共用示例相对路径，未伪造文件尺寸"}','ADMIN',1,@history_start,@history_start),
(2,'admin2','$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm','备用管理员','13800000002','测试图片.jpg','{"fixture":true,"note":"共用示例相对路径，未伪造文件尺寸"}','ADMIN',1,@history_start,@history_start),
(3,'buyer01','$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm','采购张三','13800000003','测试图片.jpg','{"fixture":true,"note":"共用示例相对路径，未伪造文件尺寸"}','PURCHASER',1,@history_start,@history_start),
(4,'buyer02','$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm','采购李四','13800000004','测试图片.jpg','{"fixture":true,"note":"共用示例相对路径，未伪造文件尺寸"}','PURCHASER',1,@history_start,@history_start),
(5,'cashier01','$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm','收银王五','13800000005','测试图片.jpg','{"fixture":true,"note":"共用示例相对路径，未伪造文件尺寸"}','CASHIER',1,@history_start,@history_start),
(6,'cashier02','$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm','收银赵六','13800000006','测试图片.jpg','{"fixture":true,"note":"共用示例相对路径，未伪造文件尺寸"}','CASHIER',1,@history_start,@history_start),
(7,'disabled01','$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm','停用收银','13800000007','测试图片.jpg','{"fixture":true,"note":"共用示例相对路径，未伪造文件尺寸"}','CASHIER',0,@history_start,@history_start),
(8,'unused01','$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm','待删除用户','13800000008','测试图片.jpg','{"fixture":true,"note":"共用示例相对路径，未伪造文件尺寸"}','CASHIER',1,@history_start,@history_start);

-- sys_config: 4 条
INSERT INTO sys_config (config_key,config_value,description,editable,updated_by,updated_at) VALUES
('inventory.default_lower_limit',CAST('5' AS JSON),'新建商品默认库存下限，不覆盖已有商品',1,1,@seed_now),
('inventory.default_near_expiry_days',CAST('7' AS JSON),'新建商品默认临期天数',1,1,@seed_now),
('security.permission_catalog',CAST('[{"code":"common","label":"公共功能","parent":null,"kind":"MODULE","roles":["ADMIN","PURCHASER","CASHIER"]},{"code":"common.dashboard","label":"工作台","parent":"common","kind":"ACTION","roles":["ADMIN","PURCHASER","CASHIER"]},{"code":"common.profile","label":"个人中心","parent":"common","kind":"ACTION","roles":["ADMIN","PURCHASER","CASHIER"]},{"code":"common.avatar","label":"更换或移除本人头像","parent":"common","kind":"ACTION","roles":["ADMIN","PURCHASER","CASHIER"]},{"code":"common.password","label":"修改本人密码","parent":"common","kind":"ACTION","roles":["ADMIN","PURCHASER","CASHIER"]},{"code":"common.logout","label":"退出","parent":"common","kind":"ACTION","roles":["ADMIN","PURCHASER","CASHIER"]},{"code":"system","label":"系统管理","parent":null,"kind":"MODULE","roles":["ADMIN"]},{"code":"system.user","label":"用户管理","parent":"system","kind":"PAGE","roles":["ADMIN"]},{"code":"system.role","label":"角色权限","parent":"system","kind":"PAGE","roles":["ADMIN"]},{"code":"system.config","label":"业务参数","parent":"system","kind":"PAGE","roles":["ADMIN"]},{"code":"system.log","label":"操作记录","parent":"system","kind":"PAGE","roles":["ADMIN"]},{"code":"system.user.create","label":"create","parent":"system.user","kind":"ACTION","roles":["ADMIN"]},{"code":"system.user.edit","label":"edit","parent":"system.user","kind":"ACTION","roles":["ADMIN"]},{"code":"system.user.enable","label":"enable","parent":"system.user","kind":"ACTION","roles":["ADMIN"]},{"code":"system.user.reset_password","label":"reset_password","parent":"system.user","kind":"ACTION","roles":["ADMIN"]},{"code":"system.user.delete","label":"delete","parent":"system.user","kind":"ACTION","roles":["ADMIN"]},{"code":"system.role.save","label":"保存权限","parent":"system.role","kind":"ACTION","roles":["ADMIN"]},{"code":"system.role.restore","label":"恢复默认","parent":"system.role","kind":"ACTION","roles":["ADMIN"]},{"code":"system.config.save","label":"保存参数","parent":"system.config","kind":"ACTION","roles":["ADMIN"]},{"code":"base","label":"基础信息","parent":null,"kind":"MODULE","roles":["ADMIN","PURCHASER"]},{"code":"base.category","label":"商品分类","parent":"base","kind":"PAGE","roles":["ADMIN"]},{"code":"base.category.create","label":"create","parent":"base.category","kind":"ACTION","roles":["ADMIN"]},{"code":"base.category.edit","label":"edit","parent":"base.category","kind":"ACTION","roles":["ADMIN"]},{"code":"base.category.enable","label":"enable","parent":"base.category","kind":"ACTION","roles":["ADMIN"]},{"code":"base.category.delete","label":"delete","parent":"base.category","kind":"ACTION","roles":["ADMIN"]},{"code":"base.product","label":"商品档案","parent":"base","kind":"PAGE","roles":["ADMIN"]},{"code":"base.product.create","label":"create","parent":"base.product","kind":"ACTION","roles":["ADMIN"]},{"code":"base.product.edit","label":"edit","parent":"base.product","kind":"ACTION","roles":["ADMIN"]},{"code":"base.product.enable","label":"enable","parent":"base.product","kind":"ACTION","roles":["ADMIN"]},{"code":"base.product.delete","label":"delete","parent":"base.product","kind":"ACTION","roles":["ADMIN"]},{"code":"base.supplier","label":"供应商","parent":"base","kind":"PAGE","roles":["ADMIN","PURCHASER"]},{"code":"base.supplier.create","label":"create","parent":"base.supplier","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"base.supplier.edit","label":"edit","parent":"base.supplier","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"base.supplier.enable","label":"enable","parent":"base.supplier","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"base.supplier.delete","label":"delete","parent":"base.supplier","kind":"ACTION","roles":["ADMIN"]},{"code":"base.product.image","label":"商品图片上传移除","parent":"base.product","kind":"ACTION","roles":["ADMIN"]},{"code":"product.purchase_lookup","label":"采购选品及参考进价","parent":null,"kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"product.sale_lookup","label":"销售选品","parent":null,"kind":"ACTION","roles":["ADMIN","CASHIER"]},{"code":"purchase","label":"进货管理","parent":null,"kind":"MODULE","roles":["ADMIN","PURCHASER"]},{"code":"purchase.order","label":"采购单","parent":"purchase","kind":"PAGE","roles":["ADMIN","PURCHASER"]},{"code":"purchase.order.create","label":"create","parent":"purchase.order","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"purchase.order.edit","label":"edit","parent":"purchase.order","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"purchase.order.delete_draft","label":"delete_draft","parent":"purchase.order","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"purchase.order.submit","label":"submit","parent":"purchase.order","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"purchase.order.withdraw","label":"withdraw","parent":"purchase.order","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"purchase.order.close","label":"close","parent":"purchase.order","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"purchase.order.reconcile","label":"reconcile","parent":"purchase.order","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"purchase.order.review","label":"review","parent":"purchase.order","kind":"ACTION","roles":["ADMIN"]},{"code":"purchase.order.reject","label":"reject","parent":"purchase.order","kind":"ACTION","roles":["ADMIN"]},{"code":"purchase.order.reverse","label":"reverse","parent":"purchase.order","kind":"ACTION","roles":["ADMIN"]},{"code":"sale","label":"销售管理","parent":null,"kind":"MODULE","roles":["ADMIN","CASHIER"]},{"code":"sale.order","label":"销售开单和记录","parent":"sale","kind":"PAGE","roles":["ADMIN","CASHIER"]},{"code":"sale.order.complete","label":"确认收款销售","parent":"sale.order","kind":"ACTION","roles":["ADMIN","CASHIER"]},{"code":"sale.order.correct","label":"整单纠错","parent":"sale.order","kind":"ACTION","roles":["ADMIN"]},{"code":"inventory","label":"库存管理","parent":null,"kind":"MODULE","roles":["ADMIN","PURCHASER","CASHIER"]},{"code":"inventory.query","label":"库存只读查询","parent":"inventory","kind":"PAGE","roles":["ADMIN","PURCHASER","CASHIER"]},{"code":"inventory.batch","label":"批次库存","parent":"inventory","kind":"PAGE","roles":["ADMIN","PURCHASER"]},{"code":"inventory.movement","label":"库存流水","parent":"inventory","kind":"PAGE","roles":["ADMIN","PURCHASER"]},{"code":"inventory.warning","label":"库存预警","parent":"inventory","kind":"PAGE","roles":["ADMIN","PURCHASER"]},{"code":"inventory.stocktake","label":"库存盘点","parent":"inventory","kind":"PAGE","roles":["ADMIN","PURCHASER"]},{"code":"inventory.stocktake.start","label":"start","parent":"inventory.stocktake","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"inventory.stocktake.save","label":"save","parent":"inventory.stocktake","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"inventory.stocktake.submit","label":"submit","parent":"inventory.stocktake","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"inventory.stocktake.withdraw","label":"withdraw","parent":"inventory.stocktake","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"inventory.stocktake.close","label":"close","parent":"inventory.stocktake","kind":"ACTION","roles":["ADMIN","PURCHASER"]},{"code":"inventory.stocktake.review","label":"review","parent":"inventory.stocktake","kind":"ACTION","roles":["ADMIN"]},{"code":"inventory.stocktake.reject","label":"reject","parent":"inventory.stocktake","kind":"ACTION","roles":["ADMIN"]},{"code":"statistics","label":"统计分析","parent":null,"kind":"MODULE","roles":["ADMIN","PURCHASER","CASHIER"]},{"code":"statistics.sales","label":"销售图表与热销滞销","parent":"statistics","kind":"PAGE","roles":["ADMIN","CASHIER"]},{"code":"statistics.inventory","label":"库存统计","parent":"statistics","kind":"PAGE","roles":["ADMIN","PURCHASER"]},{"code":"ai","label":"智能经营建议","parent":null,"kind":"MODULE","roles":["ADMIN","PURCHASER"]},{"code":"ai.inventory","label":"库存优化建议与历史","parent":"ai","kind":"PAGE","roles":["ADMIN","PURCHASER"]},{"code":"ai.sales","label":"销售经营建议与历史","parent":"ai","kind":"PAGE","roles":["ADMIN"]}]' AS JSON),'菜单按钮权限目录，代码修改时同步维护',0,1,@seed_now),
('demo.seed_info',CAST('{"fixture":true,"password":"见README，不在配置中存口令","image":"测试图片.jpg"}' AS JSON),'测试数据说明',0,1,@seed_now);

-- category: 6 条
INSERT INTO category (id,code,name,sort_order,enabled,created_at,updated_at) VALUES
(1,'C001','饮料乳品',1,1,@history_start,@history_start),
(2,'C002','粮油食品',2,1,@history_start,@history_start),
(3,'C003','日用清洁',3,1,@history_start,@history_start),
(4,'C004','文具用品',4,1,@history_start,@history_start),
(5,'C005','糕点零食',5,1,@history_start,@history_start),
(6,'C006','未用分类',6,1,@history_start,@history_start);

-- supplier: 8 条
INSERT INTO supplier (id,code,name,contact_name,contact_phone,address,enabled,remark,created_by,created_at,updated_at) VALUES
(1,'SUP001','本地综合供应商','联系人1','13900000001','演示地址1号',1,'毕业设计测试档案',3,@history_start,@history_start),
(2,'SUP002','乳品供应商','联系人2','13900000002','演示地址2号',1,'毕业设计测试档案',3,@history_start,@history_start),
(3,'SUP003','粮油供应商','联系人3','13900000003','演示地址3号',1,'毕业设计测试档案',3,@history_start,@history_start),
(4,'SUP004','日用品供应商','联系人4','13900000004','演示地址4号',1,'毕业设计测试档案',3,@history_start,@history_start),
(5,'SUP005','文具供应商','联系人5','13900000005','演示地址5号',1,'毕业设计测试档案',3,@history_start,@history_start),
(6,'SUP006','零食供应商','联系人6','13900000006','演示地址6号',1,'毕业设计测试档案',3,@history_start,@history_start),
(7,'SUP007','停用供应商','联系人7','13900000007','演示地址7号',0,'毕业设计测试档案',3,@history_start,@history_start),
(8,'SUP008','未引用供应商','联系人8','13900000008','演示地址8号',1,'毕业设计测试档案',3,@history_start,@history_start);

-- product: 24 条
INSERT INTO product (id,code,barcode,name,category_id,specification,unit,reference_cost,sale_price,image_path,image_meta,manages_expiry,shelf_life_days,near_expiry_days,stock_lower_limit,can_purchase,can_sell,enabled,stock_qty,frozen_stocktake_id,stock_version,remark,created_by,created_at,updated_at) VALUES
(1,'P0001','6900000000001','演示瓶装饮料',1,'基本单位包装','瓶',3,5,'测试图片.jpg','{"fixture":true}',1,180,7,5,1,1,1,5,NULL,6,'见README测试场景映射',1,@history_start,@seed_now),
(2,'P0002','6900000000002','纯牛奶',1,'基本单位包装','件',4,6,'测试图片.jpg','{"fixture":true}',1,180,7,10,1,1,1,38,NULL,2,'见README测试场景映射',1,@history_start,@seed_now),
(3,'P0003','6900000000003','临期酸奶',1,'基本单位包装','件',5,8,'测试图片.jpg','{"fixture":true}',1,180,7,2,1,1,1,12,NULL,1,'见README测试场景映射',1,@history_start,@seed_now),
(4,'P0004','6900000000004','过期面包',5,'基本单位包装','件',2,5,'测试图片.jpg','{"fixture":true}',1,180,7,5,1,1,1,6,NULL,1,'见README测试场景映射',1,@history_start,@seed_now),
(5,'P0005','6900000000005','大米五公斤',2,'基本单位包装','件',48,68,'测试图片.jpg','{"fixture":true}',0,NULL,7,5,1,1,1,45,NULL,3,'见README测试场景映射',1,@history_start,@seed_now),
(6,'P0006','6900000000006','滞销食用油',2,'基本单位包装','件',40,58,'测试图片.jpg','{"fixture":true}',0,NULL,7,3,1,1,1,30,NULL,1,'见README测试场景映射',1,@history_start,@seed_now),
(7,'P0007','6900000000007','抽纸三包装',3,'基本单位包装','件',8,12,'测试图片.jpg','{"fixture":true}',0,NULL,7,5,1,1,1,40,NULL,7,'见README测试场景映射',1,@history_start,@seed_now),
(8,'P0008','6900000000008','香皂',3,'基本单位包装','件',3.5,6,'测试图片.jpg','{"fixture":true}',0,NULL,7,5,1,1,1,30,NULL,3,'见README测试场景映射',1,@history_start,@seed_now),
(9,'P0009','6900000000009','并发测试矿泉水',1,'基本单位包装','瓶',1,2,'测试图片.jpg','{"fixture":true}',0,NULL,7,5,1,1,1,5,NULL,1,'见README测试场景映射',1,@history_start,@seed_now),
(10,'P0010','6900000000010','盘点中文具笔',4,'基本单位包装','件',1,3,'测试图片.jpg','{"fixture":true}',0,NULL,7,3,1,1,1,20,NULL,2,'见README测试场景映射',1,@history_start,@seed_now),
(11,'P0011','6900000000011','待审核盘点方便面',2,'基本单位包装','件',2,4,'测试图片.jpg','{"fixture":true}',0,NULL,7,3,1,1,1,10,NULL,2,'见README测试场景映射',1,@history_start,@seed_now),
(12,'P0012','6900000000012','驳回盘点饼干',5,'基本单位包装','件',5,9,'测试图片.jpg','{"fixture":true}',1,180,7,3,1,1,1,12,NULL,2,'见README测试场景映射',1,@history_start,@seed_now),
(13,'P0013','6900000000013','已停用历史商品',3,'基本单位包装','件',6,10,'测试图片.jpg','{"fixture":true}',0,NULL,7,2,1,1,0,8,NULL,1,'见README测试场景映射',1,@history_start,@seed_now),
(14,'P0014','6900000000014','禁止采购商品',3,'基本单位包装','件',6,10,'测试图片.jpg','{"fixture":true}',0,NULL,7,2,0,1,1,7,NULL,1,'见README测试场景映射',1,@history_start,@seed_now),
(15,'P0015','6900000000015','禁止销售商品',3,'基本单位包装','件',6,10,'测试图片.jpg','{"fixture":true}',0,NULL,7,2,1,0,1,8,NULL,1,'见README测试场景映射',1,@history_start,@seed_now),
(16,'P0016','6900000000016','零库存商品',4,'基本单位包装','件',2,5,'测试图片.jpg','{"fixture":true}',0,NULL,7,0,1,1,1,0,NULL,0,'见README测试场景映射',1,@history_start,@seed_now),
(17,'P0017','6900000000017','新近入库商品',4,'基本单位包装','件',4,7,'测试图片.jpg','{"fixture":true}',0,NULL,7,3,1,1,1,20,NULL,1,'见README测试场景映射',1,@history_start,@seed_now),
(18,'P0018','6900000000018','到期当天食品',5,'基本单位包装','件',5,8,'测试图片.jpg','{"fixture":true}',1,180,0,2,1,1,1,4,NULL,1,'见README测试场景映射',1,@history_start,@seed_now),
(19,'P0019','6900000000019','明天到期食品',5,'基本单位包装','件',5,8,'测试图片.jpg','{"fixture":true}',1,180,7,2,1,1,1,6,NULL,1,'见README测试场景映射',1,@history_start,@seed_now),
(20,'P0020','6900000000020','过期已清零食品',5,'基本单位包装','件',5,8,'测试图片.jpg','{"fixture":true}',1,180,7,2,1,1,1,0,NULL,2,'见README测试场景映射',1,@history_start,@seed_now),
(21,'P0021','6900000000021','待审核采购商品',4,'基本单位包装','件',2,5,'测试图片.jpg','{"fixture":true}',0,NULL,7,2,1,1,1,0,NULL,0,'见README测试场景映射',1,@history_start,@seed_now),
(22,'P0022',NULL,'可删除未使用商品',4,'基本单位包装','件',3,6,'测试图片.jpg','{"fixture":true}',0,NULL,7,0,1,1,1,0,NULL,0,'见README测试场景映射',1,@history_start,@seed_now),
(23,'P0023','6900000000023','调价快照测试商品',3,'基本单位包装','件',4,9,'测试图片.jpg','{"fixture":true}',0,NULL,7,2,1,1,1,28,NULL,2,'见README测试场景映射',1,@history_start,@seed_now),
(24,'P0024','6900000000024','缺日期驳回食品',5,'基本单位包装','件',5,9,'测试图片.jpg','{"fixture":true}',1,180,7,2,1,1,1,0,NULL,0,'见README测试场景映射',1,@history_start,@seed_now);

-- purchase_order: 10 条
INSERT INTO purchase_order (id,order_no,supplier_id,supplier_name,supplier_contact,supplier_phone,supplier_address,operator_id,operator_name,status,total_qty,total_received_qty,total_amount,arrived_at,first_submitted_at,submitted_at,reviewed_by,reviewer_name,reviewed_at,rejection_reason,stocked_at,closed_by,closed_at,close_reason,reconcile_status,remark,created_at,updated_at,external_total_amount,reconcile_difference,reconcile_resolution,reconciled_by,reconciled_at,reconcile_version,reversed_by,reversed_at,reverse_reason) VALUES
(1,'CG-DEMO-0001',1,'本地综合供应商','联系人1','13900000001','演示地址1号',3,'采购张三','IN_STOCK',332,332,4718.0,@history_start,@history_start,@history_start,1,'系统管理员',@history_start,NULL,@history_start,NULL,NULL,NULL,'MATCHED','多批次、滞销、冻结、调价、边界预警的来源',@history_start,@history_start,4718.0,NULL,'逐行核验一致',3,@seed_now,1,DEFAULT,DEFAULT,DEFAULT),
(2,'CG-DEMO-0002',6,'零食供应商','联系人6','13900000006','演示地址6号',3,'采购张三','IN_STOCK',11,11,37,DATE_ADD(@seed_now, INTERVAL -15 DAY),DATE_ADD(@seed_now, INTERVAL -15 DAY),DATE_ADD(@seed_now, INTERVAL -15 DAY),1,'系统管理员',DATE_ADD(@seed_now, INTERVAL -15 DAY),NULL,DATE_ADD(@seed_now, INTERVAL -15 DAY),NULL,NULL,NULL,'UNRECONCILED',NULL,DATE_ADD(@seed_now, INTERVAL -15 DAY),DATE_ADD(@seed_now, INTERVAL -15 DAY),NULL,NULL,NULL,NULL,NULL,2,DEFAULT,DEFAULT,DEFAULT),
(3,'CG-DEMO-0003',5,'文具供应商','联系人5','13900000005','演示地址5号',4,'采购李四','IN_STOCK',20,20,80,DATE_ADD(@seed_now, INTERVAL -1 DAY),DATE_ADD(@seed_now, INTERVAL -1 DAY),DATE_ADD(@seed_now, INTERVAL -1 DAY),1,'系统管理员',DATE_ADD(@seed_now, INTERVAL -1 DAY),NULL,DATE_ADD(@seed_now, INTERVAL -1 DAY),NULL,NULL,NULL,'DIFFERENCE',NULL,DATE_ADD(@seed_now, INTERVAL -1 DAY),DATE_ADD(@seed_now, INTERVAL -1 DAY),84,'供应商凭据数量多一件，等待核实',NULL,4,@seed_now,1,DEFAULT,DEFAULT,DEFAULT),
(4,'CG-DEMO-0004',1,'本地综合供应商','联系人1','13900000001','演示地址1号',3,'采购张三','REVERSED',4,4,32,DATE_ADD(@seed_now, INTERVAL -3 DAY),DATE_ADD(@seed_now, INTERVAL -3 DAY),DATE_ADD(@seed_now, INTERVAL -3 DAY),1,'系统管理员',DATE_ADD(@seed_now, INTERVAL -3 DAY),NULL,DATE_ADD(@seed_now, INTERVAL -3 DAY),NULL,NULL,NULL,'NA','已撤销入库的历史样例',DATE_ADD(@seed_now, INTERVAL -3 DAY),DATE_ADD(@seed_now, INTERVAL -3 DAY),DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,1,DATE_ADD(@seed_now, INTERVAL -2 DAY),'录入重复采购，尚未使用'),
(5,'CG-DEMO-0005',4,'日用品供应商','联系人4','13900000004','演示地址4号',4,'采购李四','IN_STOCK',3,3,10.5,DATE_ADD(@seed_now, INTERVAL -2 DAY),DATE_ADD(@seed_now, INTERVAL -2 DAY),DATE_ADD(@seed_now, INTERVAL -2 DAY),1,'系统管理员',DATE_ADD(@seed_now, INTERVAL -2 DAY),NULL,DATE_ADD(@seed_now, INTERVAL -2 DAY),NULL,NULL,NULL,'UNRECONCILED','无后续变动且未核对，可测试管理员撤销',DATE_ADD(@seed_now, INTERVAL -2 DAY),DATE_ADD(@seed_now, INTERVAL -2 DAY),DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT),
(6,'CG-DEMO-0006',1,'本地综合供应商','联系人1','13900000001','演示地址1号',3,'采购张三','PENDING',10,10,20,@seed_now,@seed_now,@seed_now,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'UNRECONCILED','待审核，可验证审核与撤回竞争',@seed_now,@seed_now,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT),
(7,'CG-DEMO-0007',1,'本地综合供应商','联系人1','13900000001','演示地址1号',3,'采购张三','REJECTED',8,8,40,@seed_now,@seed_now,@seed_now,1,'系统管理员',@seed_now,'缺少实物生产日期和到期日期，请补充',NULL,NULL,NULL,NULL,'UNRECONCILED',NULL,@seed_now,@seed_now,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT),
(8,'CG-DEMO-0008',1,'本地综合供应商','联系人1','13900000001','演示地址1号',4,'采购李四','DRAFT',2,0,4,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'UNRECONCILED','未提交草稿，可删除',@seed_now,@seed_now,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT),
(9,'CG-DEMO-0009',1,'本地综合供应商','联系人1','13900000001','演示地址1号',3,'采购张三','CLOSED',3,3,6,@seed_now,@seed_now,@seed_now,NULL,NULL,NULL,NULL,NULL,1,@seed_now,'取消本次未生效采购','UNRECONCILED',NULL,@seed_now,@seed_now,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT),
(10,'CG-DEMO-0010',1,'本地综合供应商','联系人1','13900000001','演示地址1号',3,'采购张三','DRAFT',6,0,288,NULL,@seed_now,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'UNRECONCILED','已撤回草稿，不允许硬删',@seed_now,@seed_now,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT,DEFAULT);

-- purchase_item: 28 条
INSERT INTO purchase_item (id,purchase_id,line_no,product_id,product_code,product_barcode,product_name,category_id,category_name,specification,unit,manages_expiry,quantity,received_qty,unit_cost,supplier_batch_no,production_date,expiry_date,batch_no,stock_qty,external_qty,external_unit_cost,reconcile_note,created_at) VALUES
(1,1,1,1,'P0001','6900000000001','演示瓶装饮料',1,'饮料乳品','基本单位包装','瓶',1,10,10,3,'SUPLOT-0001-1',DATE_SUB(@history_start,INTERVAL 10 DAY),DATE_ADD(@seed_day, INTERVAL 3 DAY),'BATCH-00001',0,10,3,'一致',@history_start),
(2,1,2,1,'P0001','6900000000001','演示瓶装饮料',1,'饮料乳品','基本单位包装','瓶',1,10,10,3,'SUPLOT-0001-2',DATE_SUB(@history_start,INTERVAL 10 DAY),DATE_ADD(@seed_day, INTERVAL 90 DAY),'BATCH-00002',5,10,3,'一致',@history_start),
(3,1,3,2,'P0002','6900000000002','纯牛奶',1,'饮料乳品','基本单位包装','件',1,40,40,4,'SUPLOT-0001-3',DATE_SUB(@history_start,INTERVAL 10 DAY),DATE_ADD(@seed_day, INTERVAL 60 DAY),'BATCH-00003',38,40,4,'一致',@history_start),
(4,1,4,3,'P0003','6900000000003','临期酸奶',1,'饮料乳品','基本单位包装','件',1,12,12,5,'SUPLOT-0001-4',DATE_SUB(@history_start,INTERVAL 10 DAY),DATE_ADD(@seed_day, INTERVAL 3 DAY),'BATCH-00004',12,12,5,'一致',@history_start),
(5,1,5,5,'P0005','6900000000005','大米五公斤',2,'粮油食品','基本单位包装','件',0,50,50,48,'SUPLOT-0001-5',NULL,NULL,'BATCH-00005',45,50,48,'一致',@history_start),
(6,1,6,6,'P0006','6900000000006','滞销食用油',2,'粮油食品','基本单位包装','件',0,30,30,40,'SUPLOT-0001-6',NULL,NULL,'BATCH-00006',30,30,40,'一致',@history_start),
(7,1,7,7,'P0007','6900000000007','抽纸三包装',3,'日用清洁','基本单位包装','件',0,40,40,8,'SUPLOT-0001-7',NULL,NULL,'BATCH-00007',40,40,8,'一致',@history_start),
(8,1,8,8,'P0008','6900000000008','香皂',3,'日用清洁','基本单位包装','件',0,30,30,3.5,'SUPLOT-0001-8',NULL,NULL,'BATCH-00008',27,30,3.5,'一致',@history_start),
(9,1,9,9,'P0009','6900000000009','并发测试矿泉水',1,'饮料乳品','基本单位包装','瓶',0,5,5,1,'SUPLOT-0001-9',NULL,NULL,'BATCH-00009',5,5,1,'一致',@history_start),
(10,1,10,10,'P0010','6900000000010','盘点中文具笔',4,'文具用品','基本单位包装','件',0,20,20,1,'SUPLOT-0001-10',NULL,NULL,'BATCH-00010',20,20,1,'一致',@history_start),
(11,1,11,11,'P0011','6900000000011','待审核盘点方便面',2,'粮油食品','基本单位包装','件',0,10,10,2,'SUPLOT-0001-11',NULL,NULL,'BATCH-00011',10,10,2,'一致',@history_start),
(12,1,12,12,'P0012','6900000000012','驳回盘点饼干',5,'糕点零食','基本单位包装','件',1,12,12,5,'SUPLOT-0001-12',DATE_SUB(@history_start,INTERVAL 10 DAY),DATE_ADD(@seed_day, INTERVAL 50 DAY),'BATCH-00012',12,12,5,'一致',@history_start),
(13,1,13,13,'P0013','6900000000013','已停用历史商品',3,'日用清洁','基本单位包装','件',0,8,8,6,'SUPLOT-0001-13',NULL,NULL,'BATCH-00013',8,8,6,'一致',@history_start),
(14,1,14,14,'P0014','6900000000014','禁止采购商品',3,'日用清洁','基本单位包装','件',0,7,7,6,'SUPLOT-0001-14',NULL,NULL,'BATCH-00014',7,7,6,'一致',@history_start),
(15,1,15,15,'P0015','6900000000015','禁止销售商品',3,'日用清洁','基本单位包装','件',0,8,8,6,'SUPLOT-0001-15',NULL,NULL,'BATCH-00015',8,8,6,'一致',@history_start),
(16,1,16,18,'P0018','6900000000018','到期当天食品',5,'糕点零食','基本单位包装','件',1,4,4,5,'SUPLOT-0001-16',DATE_SUB(@history_start,INTERVAL 10 DAY),DATE_ADD(@seed_day, INTERVAL 0 DAY),'BATCH-00016',4,4,5,'一致',@history_start),
(17,1,17,19,'P0019','6900000000019','明天到期食品',5,'糕点零食','基本单位包装','件',1,6,6,5,'SUPLOT-0001-17',DATE_SUB(@history_start,INTERVAL 10 DAY),DATE_ADD(@seed_day, INTERVAL 1 DAY),'BATCH-00017',6,6,5,'一致',@history_start),
(18,1,18,23,'P0023','6900000000023','调价快照测试商品',3,'日用清洁','基本单位包装','件',0,30,30,4,'SUPLOT-0001-18',NULL,NULL,'BATCH-00018',28,30,4,'一致',@history_start),
(19,2,1,4,'P0004','6900000000004','过期面包',5,'糕点零食','基本单位包装','件',1,6,6,2,'SUPLOT-0002-1',DATE_SUB(@history_start,INTERVAL 10 DAY),DATE_ADD(@seed_day, INTERVAL -1 DAY),'BATCH-00019',6,NULL,NULL,NULL,DATE_ADD(@seed_now, INTERVAL -15 DAY)),
(20,2,2,20,'P0020','6900000000020','过期已清零食品',5,'糕点零食','基本单位包装','件',1,5,5,5,'SUPLOT-0002-2',DATE_SUB(@history_start,INTERVAL 10 DAY),DATE_ADD(@seed_day, INTERVAL -2 DAY),'BATCH-00020',0,NULL,NULL,NULL,DATE_ADD(@seed_now, INTERVAL -15 DAY)),
(21,3,1,17,'P0017','6900000000017','新近入库商品',4,'文具用品','基本单位包装','件',0,20,20,4,'SUPLOT-0003-1',NULL,NULL,'BATCH-00021',20,21,4,'凭据数量多一件，待核查',DATE_ADD(@seed_now, INTERVAL -1 DAY)),
(22,4,1,7,'P0007','6900000000007','抽纸三包装',3,'日用清洁','基本单位包装','件',0,4,4,8,'SUPLOT-0004-1',NULL,NULL,'BATCH-00022',0,NULL,NULL,NULL,DATE_ADD(@seed_now, INTERVAL -3 DAY)),
(23,5,1,8,'P0008','6900000000008','香皂',3,'日用清洁','基本单位包装','件',0,3,3,3.5,'SUPLOT-0005-1',NULL,NULL,'BATCH-00023',3,NULL,NULL,NULL,DATE_ADD(@seed_now, INTERVAL -2 DAY)),
(24,6,1,21,'P0021','6900000000021','待审核采购商品',4,'文具用品','基本单位包装','件',0,10,10,2,'SUPLOT-0006-1',NULL,NULL,NULL,NULL,NULL,NULL,NULL,@seed_now),
(25,7,1,24,'P0024','6900000000024','缺日期驳回食品',5,'糕点零食','基本单位包装','件',1,8,8,5,'SUPLOT-0007-1',NULL,NULL,NULL,NULL,NULL,NULL,NULL,@seed_now),
(26,8,1,21,'P0021','6900000000021','待审核采购商品',4,'文具用品','基本单位包装','件',0,2,NULL,2,'SUPLOT-0008-1',NULL,NULL,NULL,NULL,NULL,NULL,NULL,@seed_now),
(27,9,1,21,'P0021','6900000000021','待审核采购商品',4,'文具用品','基本单位包装','件',0,3,3,2,'SUPLOT-0009-1',NULL,NULL,NULL,NULL,NULL,NULL,NULL,@seed_now),
(28,10,1,5,'P0005','6900000000005','大米五公斤',2,'粮油食品','基本单位包装','件',0,6,NULL,48,'SUPLOT-0010-1',NULL,NULL,NULL,NULL,NULL,NULL,NULL,@seed_now);

-- sale_order: 7 条
INSERT INTO sale_order (id,order_no,request_key,request_hash,cashier_id,cashier_name,status,total_qty,total_amount,payment_method,received_amount,completed_at,corrected_by,corrector_name,corrected_at,correction_reason,created_at) VALUES
(1,'XS-DEMO-0001','seed-sale-request-1','23f928fb4160417181a3270e267935fa7f8c010ad64cf1fbe6380bda15ac1e16',5,'收银王五','COMPLETED',5,228,'CASH',228,DATE_ADD(@history_start,INTERVAL 10 DAY),NULL,NULL,NULL,NULL,DATE_ADD(@history_start,INTERVAL 10 DAY)),
(2,'XS-DEMO-0002','seed-sale-request-2','4c8fb025429c03b83ee1c3d4d7372441e5ba5b5b565654c4cf03096db3b3a580',6,'收银赵六','COMPLETED',5,154,'MOCK_EPAY',154,DATE_ADD(DATE_SUB(@month_start,INTERVAL 1 MONTH),INTERVAL 5 DAY),NULL,NULL,NULL,NULL,DATE_ADD(DATE_SUB(@month_start,INTERVAL 1 MONTH),INTERVAL 5 DAY)),
(3,'XS-DEMO-0003','seed-sale-request-3','4e44ea03c2a63a7dce9da411a9854da2b11e857c57ecd4b1434145a525214395',6,'收银赵六','COMPLETED',2,15.00,'CASH',15.00,DATE_ADD(@seed_now, INTERVAL -5 DAY),NULL,NULL,NULL,NULL,DATE_ADD(@seed_now, INTERVAL -5 DAY)),
(4,'XS-DEMO-0004','seed-sale-request-4','26b463ce1813d543d4a1eb9067871972acc5413d08f85391aaa96cea3fb477fc',5,'收银王五','COMPLETED',12,60,'CASH',100,@seed_now,NULL,NULL,NULL,NULL,@seed_now),
(5,'XS-DEMO-0005','seed-sale-request-5','9c1705ad7806a05ce9ffa7282d2bc571c41ab5f6388a9f03cd37bd21728e0f85',6,'收银赵六','COMPLETED',2,12,'MOCK_EPAY',12,@seed_now,NULL,NULL,NULL,NULL,@seed_now),
(6,'XS-DEMO-0006','seed-sale-request-6','b2a16ff79e865a6d3329c50c5468a9a426668810f842c6490a5b4ee4ac364d61',6,'收银赵六','CORRECTED',1,12,'CASH',12,@seed_now,1,'系统管理员',@seed_now,'当天整单误录，货品完整在店',@seed_now),
(7,'XS-DEMO-0007','seed-sale-request-7','608c0e16ec2fb03c93985b29b93d630d0df47580a8cc7709b5c7b6b82e6caf9d',5,'收银王五','COMPLETED',2,10,'CASH',10,@seed_now,NULL,NULL,NULL,NULL,@seed_now);

-- sale_item: 9 条
INSERT INTO sale_item (id,sale_id,line_no,product_id,product_code,product_barcode,product_name,category_id,category_name,specification,unit,unit_price,quantity) VALUES
(1,1,1,5,'P0005','6900000000005','大米五公斤',2,'粮油食品','基本单位包装','件',68,3),
(2,1,2,7,'P0007','6900000000007','抽纸三包装',3,'日用清洁','基本单位包装','件',12,2),
(3,2,1,5,'P0005','6900000000005','大米五公斤',2,'粮油食品','基本单位包装','件',68,2),
(4,2,2,8,'P0008','6900000000008','香皂',3,'日用清洁','基本单位包装','件',6,3),
(5,3,1,23,'P0023','6900000000023','调价快照测试商品',3,'日用清洁','基本单位包装','件',7.50,2),
(6,4,1,1,'P0001','6900000000001','演示瓶装饮料',1,'饮料乳品','基本单位包装','瓶',5,12),
(7,5,1,2,'P0002','6900000000002','纯牛奶',1,'饮料乳品','基本单位包装','件',6,2),
(8,6,1,7,'P0007','6900000000007','抽纸三包装',3,'日用清洁','基本单位包装','件',12,1),
(9,7,1,1,'P0001','6900000000001','演示瓶装饮料',1,'饮料乳品','基本单位包装','瓶',5,2);

-- sale_allocation: 10 条
INSERT INTO sale_allocation (id,sale_item_id,product_id,batch_item_id,quantity) VALUES
(1,1,5,5,3),
(2,2,7,7,2),
(3,3,5,5,2),
(4,4,8,8,3),
(5,5,23,18,2),
(6,6,1,1,10),
(7,6,1,2,2),
(8,7,2,3,2),
(9,8,7,7,1),
(10,9,1,2,2);

-- stocktake_order: 5 条
INSERT INTO stocktake_order (id,order_no,operator_id,operator_name,status,started_at,submitted_at,reviewed_by,reviewer_name,reviewed_at,rejection_reason,completed_at,closed_by,closed_at,close_reason,remark,created_at,updated_at) VALUES
(1,'PD-DEMO-0001',3,'采购张三','COMPLETED',@seed_now,@seed_now,1,'系统管理员',@seed_now,NULL,@seed_now,NULL,NULL,NULL,'测试冻结、空值、差异、审核和关闭',@seed_now,@seed_now),
(2,'PD-DEMO-0002',3,'采购张三','COUNTING',@seed_now,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'测试冻结、空值、差异、审核和关闭',@seed_now,@seed_now),
(3,'PD-DEMO-0003',4,'采购李四','PENDING',@seed_now,@seed_now,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'测试冻结、空值、差异、审核和关闭',@seed_now,@seed_now),
(4,'PD-DEMO-0004',3,'采购张三','REJECTED',@seed_now,@seed_now,1,'系统管理员',@seed_now,'实盘说明待补充核实',NULL,NULL,NULL,NULL,'测试冻结、空值、差异、审核和关闭',@seed_now,@seed_now),
(5,'PD-DEMO-0005',4,'采购李四','CLOSED',@seed_now,NULL,NULL,NULL,NULL,NULL,NULL,1,@seed_now,'取消本次盘点并释放冻结','测试冻结、空值、差异、审核和关闭',@seed_now,@seed_now);

-- stocktake_item: 8 条
INSERT INTO stocktake_item (id,stocktake_id,batch_item_id,product_id,book_qty,actual_qty,reason_type,reason_note) VALUES
(1,1,2,1,8,7,'LOSS','破损损耗一瓶'),
(2,1,7,7,38,40,'COUNT_ERROR','复核原先漏记两件'),
(3,1,5,5,45,45,NULL,NULL),
(4,1,20,20,5,0,'EXPIRY','过期实物已完整清理'),
(5,2,10,10,20,NULL,NULL,NULL),
(6,3,11,11,10,10,NULL,NULL),
(7,4,12,12,12,11,'LOSS','盘亏待复核'),
(8,5,8,8,27,27,NULL,NULL);

-- stock_movement: 38 条
INSERT INTO stock_movement (id,event_type,product_id,batch_item_id,sale_allocation_id,stocktake_item_id,business_no,delta_qty,batch_before,batch_after,product_before,product_after,actor_id,actor_name,reason,occurred_at) VALUES
(1,'PURCHASE_IN',1,1,NULL,NULL,'CG-DEMO-0001',10,0,10,0,10,1,'系统管理员',NULL,@history_start),
(2,'PURCHASE_IN',1,2,NULL,NULL,'CG-DEMO-0001',10,0,10,10,20,1,'系统管理员',NULL,@history_start),
(3,'PURCHASE_IN',2,3,NULL,NULL,'CG-DEMO-0001',40,0,40,0,40,1,'系统管理员',NULL,@history_start),
(4,'PURCHASE_IN',3,4,NULL,NULL,'CG-DEMO-0001',12,0,12,0,12,1,'系统管理员',NULL,@history_start),
(5,'PURCHASE_IN',5,5,NULL,NULL,'CG-DEMO-0001',50,0,50,0,50,1,'系统管理员',NULL,@history_start),
(6,'PURCHASE_IN',6,6,NULL,NULL,'CG-DEMO-0001',30,0,30,0,30,1,'系统管理员',NULL,@history_start),
(7,'PURCHASE_IN',7,7,NULL,NULL,'CG-DEMO-0001',40,0,40,0,40,1,'系统管理员',NULL,@history_start),
(8,'PURCHASE_IN',8,8,NULL,NULL,'CG-DEMO-0001',30,0,30,0,30,1,'系统管理员',NULL,@history_start),
(9,'PURCHASE_IN',9,9,NULL,NULL,'CG-DEMO-0001',5,0,5,0,5,1,'系统管理员',NULL,@history_start),
(10,'PURCHASE_IN',10,10,NULL,NULL,'CG-DEMO-0001',20,0,20,0,20,1,'系统管理员',NULL,@history_start),
(11,'PURCHASE_IN',11,11,NULL,NULL,'CG-DEMO-0001',10,0,10,0,10,1,'系统管理员',NULL,@history_start),
(12,'PURCHASE_IN',12,12,NULL,NULL,'CG-DEMO-0001',12,0,12,0,12,1,'系统管理员',NULL,@history_start),
(13,'PURCHASE_IN',13,13,NULL,NULL,'CG-DEMO-0001',8,0,8,0,8,1,'系统管理员',NULL,@history_start),
(14,'PURCHASE_IN',14,14,NULL,NULL,'CG-DEMO-0001',7,0,7,0,7,1,'系统管理员',NULL,@history_start),
(15,'PURCHASE_IN',15,15,NULL,NULL,'CG-DEMO-0001',8,0,8,0,8,1,'系统管理员',NULL,@history_start),
(16,'PURCHASE_IN',18,16,NULL,NULL,'CG-DEMO-0001',4,0,4,0,4,1,'系统管理员',NULL,@history_start),
(17,'PURCHASE_IN',19,17,NULL,NULL,'CG-DEMO-0001',6,0,6,0,6,1,'系统管理员',NULL,@history_start),
(18,'PURCHASE_IN',23,18,NULL,NULL,'CG-DEMO-0001',30,0,30,0,30,1,'系统管理员',NULL,@history_start),
(19,'SALE_OUT',5,5,1,NULL,'XS-DEMO-0001',-3,50,47,50,47,5,'收银王五',NULL,DATE_ADD(@history_start,INTERVAL 10 DAY)),
(20,'SALE_OUT',7,7,2,NULL,'XS-DEMO-0001',-2,40,38,40,38,5,'收银王五',NULL,DATE_ADD(@history_start,INTERVAL 10 DAY)),
(21,'SALE_OUT',5,5,3,NULL,'XS-DEMO-0002',-2,47,45,47,45,6,'收银赵六',NULL,DATE_ADD(DATE_SUB(@month_start,INTERVAL 1 MONTH),INTERVAL 5 DAY)),
(22,'SALE_OUT',8,8,4,NULL,'XS-DEMO-0002',-3,30,27,30,27,6,'收银赵六',NULL,DATE_ADD(DATE_SUB(@month_start,INTERVAL 1 MONTH),INTERVAL 5 DAY)),
(23,'PURCHASE_IN',4,19,NULL,NULL,'CG-DEMO-0002',6,0,6,0,6,1,'系统管理员',NULL,DATE_ADD(@seed_now, INTERVAL -15 DAY)),
(24,'PURCHASE_IN',20,20,NULL,NULL,'CG-DEMO-0002',5,0,5,0,5,1,'系统管理员',NULL,DATE_ADD(@seed_now, INTERVAL -15 DAY)),
(25,'SALE_OUT',23,18,5,NULL,'XS-DEMO-0003',-2,30,28,30,28,6,'收银赵六',NULL,DATE_ADD(@seed_now, INTERVAL -5 DAY)),
(26,'PURCHASE_IN',7,22,NULL,NULL,'CG-DEMO-0004',4,0,4,38,42,1,'系统管理员',NULL,DATE_ADD(@seed_now, INTERVAL -3 DAY)),
(27,'PURCHASE_IN',8,23,NULL,NULL,'CG-DEMO-0005',3,0,3,27,30,1,'系统管理员',NULL,DATE_ADD(@seed_now, INTERVAL -2 DAY)),
(28,'PURCHASE_REVERSE',7,22,NULL,NULL,'CG-DEMO-0004',-4,4,0,42,38,1,'系统管理员','录入重复采购，尚未使用',DATE_ADD(@seed_now, INTERVAL -2 DAY)),
(29,'PURCHASE_IN',17,21,NULL,NULL,'CG-DEMO-0003',20,0,20,0,20,1,'系统管理员',NULL,DATE_ADD(@seed_now, INTERVAL -1 DAY)),
(30,'SALE_OUT',1,1,6,NULL,'XS-DEMO-0004',-10,10,0,20,10,5,'收银王五',NULL,@seed_now),
(31,'SALE_OUT',1,2,7,NULL,'XS-DEMO-0004',-2,10,8,10,8,5,'收银王五',NULL,@seed_now),
(32,'SALE_OUT',2,3,8,NULL,'XS-DEMO-0005',-2,40,38,40,38,6,'收银赵六',NULL,@seed_now),
(33,'SALE_OUT',7,7,9,NULL,'XS-DEMO-0006',-1,38,37,38,37,6,'收银赵六',NULL,@seed_now),
(34,'SALE_CORRECTION',7,7,9,NULL,'XS-DEMO-0006',1,37,38,37,38,1,'系统管理员','当天整单误录，货品完整在店',@seed_now),
(35,'STOCKTAKE_LOSS',1,2,NULL,1,'PD-DEMO-0001',-1,8,7,8,7,1,'系统管理员','破损损耗一瓶',@seed_now),
(36,'STOCKTAKE_GAIN',7,7,NULL,2,'PD-DEMO-0001',2,38,40,38,40,1,'系统管理员','复核原先漏记两件',@seed_now),
(37,'STOCKTAKE_LOSS',20,20,NULL,4,'PD-DEMO-0001',-5,5,0,5,0,1,'系统管理员','过期实物已完整清理',@seed_now),
(38,'SALE_OUT',1,2,10,NULL,'XS-DEMO-0007',-2,7,5,7,5,5,'收银王五',NULL,@seed_now);

-- operation_log: 28 条
INSERT INTO operation_log (id,actor_id,actor_name,action,target_type,target_id,target_no,result,reason,before_data,after_data,occurred_at) VALUES
(1,1,'系统管理员','PURCHASE_APPROVE','PURCHASE','1','CG-DEMO-0001','SUCCESS',NULL,NULL,'{"status":"IN_STOCK","batchIds":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18]}',@history_start),
(2,1,'系统管理员','PURCHASE_APPROVE','PURCHASE','2','CG-DEMO-0002','SUCCESS',NULL,NULL,'{"status":"IN_STOCK","batchIds":[19,20]}',DATE_ADD(@seed_now, INTERVAL -15 DAY)),
(3,1,'系统管理员','PURCHASE_APPROVE','PURCHASE','3','CG-DEMO-0003','SUCCESS',NULL,NULL,'{"status":"IN_STOCK","batchIds":[21]}',DATE_ADD(@seed_now, INTERVAL -1 DAY)),
(4,1,'系统管理员','PURCHASE_APPROVE','PURCHASE','4','CG-DEMO-0004','SUCCESS',NULL,NULL,'{"status":"IN_STOCK","batchIds":[22]}',DATE_ADD(@seed_now, INTERVAL -3 DAY)),
(5,1,'系统管理员','PURCHASE_APPROVE','PURCHASE','5','CG-DEMO-0005','SUCCESS',NULL,NULL,'{"status":"IN_STOCK","batchIds":[23]}',DATE_ADD(@seed_now, INTERVAL -2 DAY)),
(6,1,'系统管理员','PURCHASE_REJECT','PURCHASE','7','CG-DEMO-0007','SUCCESS','缺少实物生产日期和到期日期，请补充',NULL,'{"status":"REJECTED"}',@seed_now),
(7,3,'采购张三','PURCHASE_WITHDRAW','PURCHASE','10','CG-DEMO-0010','SUCCESS',NULL,'{"status":"PENDING"}','{"status":"DRAFT"}',@seed_now),
(8,1,'系统管理员','PURCHASE_REVERSE','PURCHASE','4','CG-DEMO-0004','SUCCESS','录入重复采购，尚未使用','{"status":"IN_STOCK"}','{"status":"REVERSED"}',DATE_ADD(@seed_now, INTERVAL -2 DAY)),
(9,1,'系统管理员','SALE_CORRECT','SALE','6','XS-DEMO-0006','SUCCESS','当天整单误录，货品完整在店','{"status":"COMPLETED"}','{"status":"CORRECTED"}',@seed_now),
(10,1,'系统管理员','STOCKTAKE_APPROVE','STOCKTAKE','1','PD-DEMO-0001','SUCCESS',NULL,NULL,'{"status":"COMPLETED","productIds":[1,5,7,20]}',@seed_now),
(11,3,'采购张三','STOCKTAKE_COUNTING','STOCKTAKE','2','PD-DEMO-0002','SUCCESS',NULL,NULL,'{"status":"COUNTING","productIds":[10]}',@seed_now),
(12,4,'采购李四','STOCKTAKE_PENDING','STOCKTAKE','3','PD-DEMO-0003','SUCCESS',NULL,NULL,'{"status":"PENDING","productIds":[11]}',@seed_now),
(13,1,'系统管理员','STOCKTAKE_REJECTED','STOCKTAKE','4','PD-DEMO-0004','SUCCESS',NULL,NULL,'{"status":"REJECTED","productIds":[12]}',@seed_now),
(14,1,'系统管理员','STOCKTAKE_CLOSED','STOCKTAKE','5','PD-DEMO-0005','SUCCESS',NULL,NULL,'{"status":"CLOSED","productIds":[8]}',@seed_now),
(15,3,'采购张三','RECONCILE_MATCH','PURCHASE','1','CG-DEMO-0001','SUCCESS',NULL,'{"status":"UNRECONCILED"}','{"status":"MATCHED","externalTotal":"4718.0","lines":[{"itemId":1,"externalQty":10,"externalUnitCost":"3"},{"itemId":2,"externalQty":10,"externalUnitCost":"3"},{"itemId":3,"externalQty":40,"externalUnitCost":"4"},{"itemId":4,"externalQty":12,"externalUnitCost":"5"},{"itemId":5,"externalQty":50,"externalUnitCost":"48"},{"itemId":6,"externalQty":30,"externalUnitCost":"40"},{"itemId":7,"externalQty":40,"externalUnitCost":"8"},{"itemId":8,"externalQty":30,"externalUnitCost":"3.5"},{"itemId":9,"externalQty":5,"externalUnitCost":"1"},{"itemId":10,"externalQty":20,"externalUnitCost":"1"},{"itemId":11,"externalQty":10,"externalUnitCost":"2"},{"itemId":12,"externalQty":12,"externalUnitCost":"5"},{"itemId":13,"externalQty":8,"externalUnitCost":"6"},{"itemId":14,"externalQty":7,"externalUnitCost":"6"},{"itemId":15,"externalQty":8,"externalUnitCost":"6"},{"itemId":16,"externalQty":4,"externalUnitCost":"5"},{"itemId":17,"externalQty":6,"externalUnitCost":"5"},{"itemId":18,"externalQty":30,"externalUnitCost":"4"}]}',@seed_now),
(16,4,'采购李四','RECONCILE_DIFFERENCE','PURCHASE','3','CG-DEMO-0003','SUCCESS',NULL,'{"status":"UNRECONCILED"}','{"status":"DIFFERENCE","externalTotal":"84","lines":[{"itemId":21,"externalQty":21,"externalUnitCost":"4"}]}',@seed_now),
(17,3,'采购张三','RECONCILE_MATCH','PURCHASE','2','CG-DEMO-0002','SUCCESS',NULL,'{"status":"UNRECONCILED"}','{"status":"MATCHED","externalTotal":"37","lines":[{"itemId":19,"externalQty":6,"externalUnitCost":"2"},{"itemId":20,"externalQty":5,"externalUnitCost":"5"}]}',@seed_now),
(18,3,'采购张三','RECONCILE_WITHDRAW','PURCHASE','2','CG-DEMO-0002','SUCCESS','重新检查供应商纸质凭据','{"status":"MATCHED","externalTotal":"37","lines":[{"itemId":19,"externalQty":6,"externalUnitCost":"2"},{"itemId":20,"externalQty":5,"externalUnitCost":"5"}]}','{"status":"UNRECONCILED"}',@seed_now),
(19,1,'系统管理员','PRODUCT_PRICE_CHANGE','PRODUCT','23','P0023','SUCCESS','验证历史售价不随新售价变化','{"salePrice":"7.50"}','{"salePrice":"9.00"}',@seed_now),
(20,1,'系统管理员','PRODUCT_STATUS_CHANGE','PRODUCT','13','P0013','SUCCESS',NULL,'{"enabled":1,"canPurchase":1,"canSell":1}','{"enabled":0,"canPurchase":1,"canSell":1}',@seed_now),
(21,1,'系统管理员','PRODUCT_STATUS_CHANGE','PRODUCT','14','P0014','SUCCESS',NULL,'{"enabled":1,"canPurchase":1,"canSell":1}','{"enabled":1,"canPurchase":0,"canSell":1}',@seed_now),
(22,1,'系统管理员','PRODUCT_STATUS_CHANGE','PRODUCT','15','P0015','SUCCESS',NULL,'{"enabled":1,"canPurchase":1,"canSell":1}','{"enabled":1,"canPurchase":1,"canSell":0}',@seed_now),
(23,3,'采购张三','AI_SUCCESS','AI','1','seed-ai-1','SUCCESS',NULL,NULL,'{"isFixture":true,"status":"SUCCESS","type":"INVENTORY"}',@seed_now),
(24,1,'系统管理员','AI_SUCCESS','AI','2','seed-ai-2','SUCCESS',NULL,NULL,'{"isFixture":true,"status":"SUCCESS","type":"SALES"}',@seed_now),
(25,3,'采购张三','AI_FAILURE','AI','3','seed-ai-3','SUCCESS',NULL,NULL,'{"isFixture":true,"status":"FAILURE","type":"INVENTORY"}',@seed_now),
(26,1,'系统管理员','AI_FAILURE','AI','4','seed-ai-4','SUCCESS',NULL,NULL,'{"isFixture":true,"status":"FAILURE","type":"SALES"}',@seed_now),
(27,4,'采购李四','AI_FAILURE','AI','5','seed-ai-5','SUCCESS',NULL,NULL,'{"isFixture":true,"status":"FAILURE","type":"INVENTORY"}',@seed_now),
(28,3,'采购张三','AI_FAILURE','AI','6','seed-ai-6','SUCCESS',NULL,NULL,'{"isFixture":true,"status":"FAILURE","type":"INVENTORY"}',@seed_now);

-- ai_suggestion: 6 条
INSERT INTO ai_suggestion (id,request_key,suggestion_type,operator_id,operator_name,period_start,period_end,filter_data,input_summary,snapshot_at,provider,model_name,is_fixture,status,content,error_code,error_message,duration_ms,requested_at,finished_at) VALUES
(1,'seed-ai-1','INVENTORY',3,'采购张三',DATE_ADD(@seed_day, INTERVAL -29 DAY),DATE_ADD(@seed_day, INTERVAL 0 DAY),'{"productIds":[1]}',JSON_OBJECT('fixture',TRUE,'productId',1,'bookQty',5, 'saleableQty',5, 'lowerLimit',5,'periodNetQty',14,'periodNetAmount',70.00,'capturedAt',DATE_FORMAT(@seed_now,'%Y-%m-%d %H:%i:%s.%f')),@seed_now,'FIXTURE','fixture-not-api',1,'SUCCESS','【初始测试数据，非真实模型调用】库存5瓶、下限5瓶，近期净销量14瓶；请人工核实补货数量。',NULL,NULL,0,@seed_now,@seed_now),
(2,'seed-ai-2','SALES',1,'系统管理员',DATE_ADD(@seed_day, INTERVAL -29 DAY),DATE_ADD(@seed_day, INTERVAL 0 DAY),'{}',JSON_OBJECT('fixture',TRUE,'netQty',(SELECT COALESCE(SUM(signed_qty),0) FROM v_sale_event WHERE event_at>=DATE_SUB(@seed_day,INTERVAL 29 DAY)),'netAmount',(SELECT COALESCE(SUM(signed_amount),0) FROM v_sale_event WHERE event_at>=DATE_SUB(@seed_day,INTERVAL 29 DAY))),@seed_now,'FIXTURE','fixture-not-api',1,'SUCCESS','【初始测试数据，非真实模型调用】请结合输入摘要查看销售与商品排行，并人工核查滞销原因。',NULL,NULL,0,@seed_now,@seed_now),
(3,'seed-ai-3','INVENTORY',3,'采购张三',DATE_ADD(@seed_day, INTERVAL -29 DAY),DATE_ADD(@seed_day, INTERVAL 0 DAY),'{"productIds":[1]}',JSON_OBJECT('fixture',TRUE,'productId',1,'bookQty',5, 'saleableQty',5, 'lowerLimit',5,'periodNetQty',14,'periodNetAmount',70.00,'capturedAt',DATE_FORMAT(@seed_now,'%Y-%m-%d %H:%i:%s.%f')),@seed_now,'FIXTURE','fixture-not-api',1,'FAILURE',NULL,'TIMEOUT','测试降级记录：TIMEOUT',15000,@seed_now,@seed_now),
(4,'seed-ai-4','SALES',1,'系统管理员',DATE_ADD(@seed_day, INTERVAL -29 DAY),DATE_ADD(@seed_day, INTERVAL 0 DAY),'{}',JSON_OBJECT('fixture',TRUE,'netQty',(SELECT COALESCE(SUM(signed_qty),0) FROM v_sale_event WHERE event_at>=DATE_SUB(@seed_day,INTERVAL 29 DAY)),'netAmount',(SELECT COALESCE(SUM(signed_amount),0) FROM v_sale_event WHERE event_at>=DATE_SUB(@seed_day,INTERVAL 29 DAY))),@seed_now,'FIXTURE','fixture-not-api',1,'FAILURE',NULL,'NOT_CONFIGURED','测试降级记录：NOT_CONFIGURED',0,@seed_now,@seed_now),
(5,'seed-ai-5','INVENTORY',4,'采购李四',DATE_ADD(@seed_day, INTERVAL -29 DAY),DATE_ADD(@seed_day, INTERVAL 0 DAY),'{"productIds":[1]}',JSON_OBJECT('fixture',TRUE,'productId',1,'bookQty',5, 'saleableQty',5, 'lowerLimit',5,'periodNetQty',14,'periodNetAmount',70.00,'capturedAt',DATE_FORMAT(@seed_now,'%Y-%m-%d %H:%i:%s.%f')),@seed_now,'FIXTURE','fixture-not-api',1,'FAILURE',NULL,'RATE_LIMITED','测试降级记录：RATE_LIMITED',0,@seed_now,@seed_now),
(6,'seed-ai-6','INVENTORY',3,'采购张三',DATE_ADD(@seed_day, INTERVAL -29 DAY),DATE_ADD(@seed_day, INTERVAL 0 DAY),'{"productIds":[1]}',JSON_OBJECT('fixture',TRUE,'productId',1,'bookQty',5, 'saleableQty',5, 'lowerLimit',5,'periodNetQty',14,'periodNetAmount',70.00,'capturedAt',DATE_FORMAT(@seed_now,'%Y-%m-%d %H:%i:%s.%f')),@seed_now,'FIXTURE','fixture-not-api',1,'FAILURE',NULL,'EMPTY_RESULT','测试降级记录：EMPTY_RESULT',0,@seed_now,@seed_now);

-- 盘点单已存在后设置活动冻结。
UPDATE product SET frozen_stocktake_id=2 WHERE id=10;
UPDATE product SET frozen_stocktake_id=3 WHERE id=11;
UPDATE product SET frozen_stocktake_id=4 WHERE id=12;
COMMIT;
SELECT 'seed_ready' result,@seed_day seed_date,(SELECT COUNT(*) FROM product) products,(SELECT COUNT(*) FROM stock_movement) movements;
