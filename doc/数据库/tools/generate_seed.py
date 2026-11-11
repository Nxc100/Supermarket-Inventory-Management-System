"""历史种子推导预览；唯一数据库基准为doc/数据库/supermarket_inventory.sql。

本脚本只向系统临时目录输出片段，不更新权威SQL。库存流水由事件模型推导。
"""
from __future__ import annotations

import hashlib
import json
import tempfile
from collections import defaultdict
from decimal import Decimal as D
from pathlib import Path


class SQL(str):
    pass


def sql(value):
    if isinstance(value, SQL):
        return str(value)
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, (int, D)):
        return str(value)
    if isinstance(value, (dict, list)):
        value = json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def day(offset=0):
    return SQL(f"DATE_ADD(@seed_day, INTERVAL {offset} DAY)")


def when(offset=0):
    return SQL(f"DATE_ADD(@seed_now, INTERVAL {offset} DAY)")


OLD = SQL("@history_start")
M2 = SQL("DATE_ADD(@history_start,INTERVAL 10 DAY)")
M1 = SQL("DATE_ADD(DATE_SUB(@month_start,INTERVAL 1 MONTH),INTERVAL 5 DAY)")
TODAY = SQL("@seed_now")
HASH = "$2b$10$pi7ZXD0nDaiDtjKkePEkVexGnGEBaDlHDIyM5ML.sy0xaI1UoGCkm"
rows = defaultdict(list)
users = {1: "系统管理员", 2: "备用管理员", 3: "采购张三", 4: "采购李四", 5: "收银王五", 6: "收银赵六", 7: "停用收银", 8: "待删除用户"}


def add(table, **values):
    rows[table].append(values)
    return values


# 权限目录：模块和页面也是权限代码，按钮有parent。固定角色上限不允许被普通授权接口改写。
catalog = []


def permission(code, label, parent=None, kind="ACTION", roles=("ADMIN",)):
    catalog.append(dict(code=code, label=label, parent=parent, kind=kind, roles=list(roles)))


ALL = ("ADMIN", "PURCHASER", "CASHIER")
AP = ("ADMIN", "PURCHASER")
AC = ("ADMIN", "CASHIER")
permission("common", "公共功能", kind="MODULE", roles=ALL)
for c, n in [("dashboard", "工作台"), ("profile", "个人中心"), ("avatar", "更换或移除本人头像"), ("password", "修改本人密码"), ("logout", "退出")]:
    permission("common." + c, n, "common", roles=ALL)
permission("system", "系统管理", kind="MODULE")
for c, n in [("user", "用户管理"), ("role", "角色权限"), ("config", "业务参数"), ("log", "操作记录")]:
    permission("system." + c, n, "system", "PAGE")
for c in ["create", "edit", "enable", "reset_password", "delete"]:
    permission("system.user." + c, c, "system.user")
permission("system.role.save", "保存权限", "system.role")
permission("system.role.restore", "恢复默认", "system.role")
permission("system.config.save", "保存参数", "system.config")
permission("base", "基础信息", kind="MODULE", roles=AP)
for c, n, rs in [("category", "商品分类", ("ADMIN",)), ("product", "商品档案", ("ADMIN",)), ("supplier", "供应商", AP)]:
    permission("base." + c, n, "base", "PAGE", rs)
    for action in ["create", "edit", "enable", "delete"]:
        permission("base." + c + "." + action, action, "base." + c, roles=("ADMIN",) if action == "delete" else rs)
permission("base.product.image", "商品图片上传移除", "base.product")
permission("product.purchase_lookup", "采购选品及参考进价", roles=AP)
permission("product.sale_lookup", "销售选品", roles=AC)
permission("purchase", "进货管理", kind="MODULE", roles=AP)
permission("purchase.order", "采购单", "purchase", "PAGE", AP)
for action in ["create", "edit", "delete_draft", "submit", "withdraw", "close", "reconcile"]:
    permission("purchase.order." + action, action, "purchase.order", roles=AP)
for action in ["review", "reject", "reverse"]:
    permission("purchase.order." + action, action, "purchase.order")
permission("sale", "销售管理", kind="MODULE", roles=AC)
permission("sale.order", "销售开单和记录", "sale", "PAGE", AC)
permission("sale.order.complete", "确认收款销售", "sale.order", roles=AC)
permission("sale.order.correct", "整单纠错", "sale.order")
permission("inventory", "库存管理", kind="MODULE", roles=ALL)
permission("inventory.query", "库存只读查询", "inventory", "PAGE", ALL)
for c, n in [("batch", "批次库存"), ("movement", "库存流水"), ("warning", "库存预警"), ("stocktake", "库存盘点")]:
    permission("inventory." + c, n, "inventory", "PAGE", AP)
for c in ["start", "save", "submit", "withdraw", "close"]:
    permission("inventory.stocktake." + c, c, "inventory.stocktake", roles=AP)
for c in ["review", "reject"]:
    permission("inventory.stocktake." + c, c, "inventory.stocktake")
permission("statistics", "统计分析", kind="MODULE", roles=ALL)
permission("statistics.sales", "销售图表与热销滞销", "statistics", "PAGE", AC)
permission("statistics.inventory", "库存统计", "statistics", "PAGE", AP)
permission("ai", "智能经营建议", kind="MODULE", roles=AP)
permission("ai.inventory", "库存优化建议与历史", "ai", "PAGE", AP)
permission("ai.sales", "销售经营建议与历史", "ai", "PAGE")
for role, name in [("ADMIN", "超市管理员"), ("PURCHASER", "采购入库操作员"), ("CASHIER", "前台收银操作员")]:
    codes = [p["code"] for p in catalog if role in p["roles"]]
    add("sys_role", code=role, name=name, permissions=codes, default_permissions=codes, ceiling_permissions=codes,
        data_scope="ALL" if role == "ADMIN" else "OWN", created_at=OLD, updated_at=OLD)
for uid, username, role, enabled in [(1, "admin", "ADMIN", 1), (2, "admin2", "ADMIN", 1), (3, "buyer01", "PURCHASER", 1),
                                     (4, "buyer02", "PURCHASER", 1), (5, "cashier01", "CASHIER", 1), (6, "cashier02", "CASHIER", 1),
                                     (7, "disabled01", "CASHIER", 0), (8, "unused01", "CASHIER", 1)]:
    add("sys_user", id=uid, username=username, password_hash=HASH, real_name=users[uid], phone=f"138000000{uid:02d}",
        avatar_path="测试图片.jpg", avatar_meta={"fixture": True, "note": "共用示例相对路径，未伪造文件尺寸"},
        role_code=role, enabled=enabled, created_at=OLD, updated_at=OLD)
for key, value, desc, edit in [("inventory.default_lower_limit", 5, "新建商品默认库存下限，不覆盖已有商品", 1),
                                ("inventory.default_near_expiry_days", 7, "新建商品默认临期天数", 1),
                                ("security.permission_catalog", catalog, "菜单按钮权限目录，代码修改时同步维护", 0),
                                ("demo.seed_info", {"fixture": True, "password": "见README，不在配置中存口令", "image": "测试图片.jpg"}, "测试数据说明", 0)]:
    json_value = SQL("CAST(" + sql(json.dumps(value,ensure_ascii=False,separators=(",",":"))) + " AS JSON)")
    add("sys_config", config_key=key, config_value=json_value, description=desc, editable=edit, updated_by=1, updated_at=TODAY)
categories = {1: "饮料乳品", 2: "粮油食品", 3: "日用清洁", 4: "文具用品", 5: "糕点零食", 6: "未用分类"}
for cid, name in categories.items():
    add("category", id=cid, code=f"C{cid:03d}", name=name, sort_order=cid, enabled=1, created_at=OLD, updated_at=OLD)
suppliers = {}
for sid, name in enumerate(["本地综合供应商", "乳品供应商", "粮油供应商", "日用品供应商", "文具供应商", "零食供应商", "停用供应商", "未引用供应商"], 1):
    suppliers[sid] = add("supplier", id=sid, code=f"SUP{sid:03d}", name=name, contact_name=f"联系人{sid}", contact_phone=f"139000000{sid:02d}",
                         address=f"演示地址{sid}号", enabled=0 if sid == 7 else 1, remark="毕业设计测试档案", created_by=3,
                         created_at=OLD, updated_at=OLD)

# id,name,category,sale,cost,expiry,lower,stock flags
specs = [
 (1,"演示瓶装饮料",1,"5","3",1,5), (2,"纯牛奶",1,"6","4",1,10), (3,"临期酸奶",1,"8","5",1,2),
 (4,"过期面包",5,"5","2",1,5), (5,"大米五公斤",2,"68","48",0,5), (6,"滞销食用油",2,"58","40",0,3),
 (7,"抽纸三包装",3,"12","8",0,5), (8,"香皂",3,"6","3.5",0,5), (9,"并发测试矿泉水",1,"2","1",0,5),
 (10,"盘点中文具笔",4,"3","1",0,3), (11,"待审核盘点方便面",2,"4","2",0,3), (12,"驳回盘点饼干",5,"9","5",1,3),
 (13,"已停用历史商品",3,"10","6",0,2), (14,"禁止采购商品",3,"10","6",0,2), (15,"禁止销售商品",3,"10","6",0,2),
 (16,"零库存商品",4,"5","2",0,0), (17,"新近入库商品",4,"7","4",0,3), (18,"到期当天食品",5,"8","5",1,2),
 (19,"明天到期食品",5,"8","5",1,2), (20,"过期已清零食品",5,"8","5",1,2), (21,"待审核采购商品",4,"5","2",0,2),
 (22,"可删除未使用商品",4,"6","3",0,0), (23,"调价快照测试商品",3,"9","4",0,2), (24,"缺日期驳回食品",5,"9","5",1,2),
]
products = {}
balances = defaultdict(int)
for pid, name, cid, price, cost, expiry, lower in specs:
    products[pid] = add("product", id=pid, code=f"P{pid:04d}", barcode=None if pid == 22 else f"690000000{pid:04d}", name=name,
        category_id=cid, specification="基本单位包装", unit="瓶" if pid in [1, 9] else "件", reference_cost=D(cost), sale_price=D(price),
        image_path="测试图片.jpg", image_meta={"fixture": True}, manages_expiry=expiry, shelf_life_days=180 if expiry else None,
        near_expiry_days=0 if pid == 18 else 7, stock_lower_limit=lower, can_purchase=0 if pid == 14 else 1,
        can_sell=0 if pid == 15 else 1, enabled=0 if pid == 13 else 1, stock_qty=0, frozen_stocktake_id=None,
        stock_version=0, remark="见README测试场景映射", created_by=1, created_at=OLD, updated_at=TODAY)


def snapshot(pid):
    p = products[pid]
    return dict(product_id=pid, product_code=p["code"], product_barcode=p["barcode"], product_name=p["name"],
                category_id=p["category_id"], category_name=categories[p["category_id"]], specification=p["specification"], unit=p["unit"])


def log(action, typ, ident, number, actor=1, at=TODAY, reason=None, before=None, after=None):
    add("operation_log", id=len(rows["operation_log"])+1, actor_id=actor, actor_name=users[actor], action=action, target_type=typ,
        target_id=str(ident), target_no=number, result="SUCCESS", reason=reason, before_data=before, after_data=after, occurred_at=at)


orders = {}
batches = {}
movements = []


def move(kind, batch, delta, no, at, actor=1, allocation=None, stitem=None, reason=None):
    b = batches[batch]
    pid = b["product_id"]
    before = b["stock_qty"] or 0
    pb = balances[pid]
    assert before + delta >= 0, (kind, batch, before, delta)
    b["stock_qty"] = before + delta
    balances[pid] = pb + delta
    products[pid]["stock_version"] += 1
    add("stock_movement", id=len(rows["stock_movement"])+1, event_type=kind, product_id=pid, batch_item_id=batch,
        sale_allocation_id=allocation, stocktake_item_id=stitem, business_no=no, delta_qty=delta,
        batch_before=before, batch_after=before+delta, product_before=pb, product_after=pb+delta,
        actor_id=actor, actor_name=users[actor], reason=reason, occurred_at=at)


def purchase(oid, entries, status="IN_STOCK", actor=3, supplier=1, at=OLD, remark=None):
    """entries=(product,quantity,expiry_date)，缺日期可用于未入库草稿。"""
    s = suppliers[supplier]
    total = sum((products[pid]["reference_cost"]*qty for pid, qty, exp in entries), D(0))
    o = add("purchase_order", id=oid, order_no=f"CG-DEMO-{oid:04d}", supplier_id=supplier, supplier_name=s["name"],
        supplier_contact=s["contact_name"], supplier_phone=s["contact_phone"], supplier_address=s["address"],
        operator_id=actor, operator_name=users[actor], status=status, total_qty=sum(e[1] for e in entries),
        total_received_qty=0 if status == "DRAFT" else sum(e[1] for e in entries), total_amount=total,
        arrived_at=None if status == "DRAFT" else at, first_submitted_at=None if status == "DRAFT" else at,
        submitted_at=None if status == "DRAFT" else at, reviewed_by=1 if status in ["IN_STOCK", "REJECTED"] else None,
        reviewer_name=users[1] if status in ["IN_STOCK", "REJECTED"] else None, reviewed_at=at if status in ["IN_STOCK", "REJECTED"] else None,
        rejection_reason="缺少实物生产日期和到期日期，请补充" if status == "REJECTED" else None,
        stocked_at=at if status == "IN_STOCK" else None, closed_by=1 if status == "CLOSED" else None,
        closed_at=at if status == "CLOSED" else None, close_reason="取消本次未生效采购" if status == "CLOSED" else None,
        reconcile_status="UNRECONCILED", remark=remark, created_at=at, updated_at=at)
    orders[oid] = o
    ids = []
    for line, (pid, qty, expiry) in enumerate(entries, 1):
        bid = len(rows["purchase_item"])+1
        b = add("purchase_item", id=bid, purchase_id=oid, line_no=line, **snapshot(pid), manages_expiry=products[pid]["manages_expiry"],
            quantity=qty, received_qty=None if status == "DRAFT" else qty, unit_cost=products[pid]["reference_cost"],
            supplier_batch_no=f"SUPLOT-{oid:04d}-{line}", production_date=SQL("DATE_SUB(@history_start,INTERVAL 10 DAY)") if expiry else None,
            expiry_date=expiry, batch_no=f"BATCH-{bid:05d}" if status == "IN_STOCK" else None,
            stock_qty=0 if status == "IN_STOCK" else None, external_qty=None, external_unit_cost=None, reconcile_note=None, created_at=at)
        batches[bid] = b
        ids.append(bid)
        if status == "IN_STOCK":
            move("PURCHASE_IN", bid, qty, o["order_no"], at)
    if status == "IN_STOCK":
        log("PURCHASE_APPROVE", "PURCHASE", oid, o["order_no"], at=at, after={"status": status, "batchIds": ids})
    elif status == "REJECTED":
        log("PURCHASE_REJECT", "PURCHASE", oid, o["order_no"], at=at, reason=o["rejection_reason"], after={"status": status})
    return ids


# 两个月前先入库，非食品历史销售不会早于来源采购。
main = purchase(1, [(1,10,day(3)), (1,10,day(90)), (2,40,day(60)), (3,12,day(3)),
                    (5,50,None), (6,30,None), (7,40,None), (8,30,None), (9,5,None), (10,20,None),
                    (11,10,None), (12,12,day(50)), (13,8,None), (14,7,None), (15,8,None),
                    (18,4,day(0)), (19,6,day(1)), (23,30,None)], remark="多批次、滞销、冻结、调价、边界预警的来源")
byproduct = defaultdict(list)
for bid in main:
    byproduct[batches[bid]["product_id"]].append(bid)
expired = purchase(2, [(4,6,day(-1)), (20,5,day(-2))], at=when(-15), supplier=6)
fresh = purchase(3, [(17,20,None)], actor=4, supplier=5, at=when(-1))
cancel_batch = purchase(4, [(7,4,None)], at=when(-3), remark="已撤销入库的历史样例")
reversible = purchase(5, [(8,3,None)], actor=4, supplier=4, at=when(-2), remark="无后续变动且未核对，可测试管理员撤销")
purchase(6, [(21,10,None)], status="PENDING", at=TODAY, remark="待审核，可验证审核与撤回竞争")
purchase(7, [(24,8,None)], status="REJECTED", at=TODAY)
purchase(8, [(21,2,None)], status="DRAFT", actor=4, at=TODAY, remark="未提交草稿，可删除")
purchase(9, [(21,3,None)], status="CLOSED", at=TODAY)
purchase(10, [(5,6,None)], status="DRAFT", at=TODAY, remark="已撤回草稿，不允许硬删")
orders[10]["first_submitted_at"] = TODAY
log("PURCHASE_WITHDRAW", "PURCHASE", 10, orders[10]["order_no"], actor=3, before={"status":"PENDING"}, after={"status":"DRAFT"})

# 演示采购撤销：有原始正向和反向流水，源批次保留为0。
o = orders[4]
move("PURCHASE_REVERSE", cancel_batch[0], -4, o["order_no"], when(-2), reason="录入重复采购，尚未使用")
o.update(status="REVERSED", reversed_by=1, reversed_at=when(-2), reverse_reason="录入重复采购，尚未使用", reconcile_status="NA")
log("PURCHASE_REVERSE", "PURCHASE", 4, o["order_no"], at=when(-2), reason=o["reverse_reason"], before={"status":"IN_STOCK"}, after={"status":"REVERSED"})


def sale(sid, lines, at, cashier=5, method="CASH", received=None, correct=False):
    """lines=(product,quantity,price,[批次分配数])；价格可显式给历史快照。"""
    total = sum((D(price)*qty for pid, qty, price, alloc in lines), D(0))
    s = add("sale_order", id=sid, order_no=f"XS-DEMO-{sid:04d}", request_key=f"seed-sale-request-{sid}",
        request_hash=hashlib.sha256(f"seed-sale-{sid}-{lines}".encode()).hexdigest(), cashier_id=cashier, cashier_name=users[cashier],
        status="CORRECTED" if correct else "COMPLETED", total_qty=sum(x[1] for x in lines), total_amount=total,
        payment_method=method, received_amount=total if method == "MOCK_EPAY" or received is None else D(received), completed_at=at,
        corrected_by=1 if correct else None, corrector_name=users[1] if correct else None,
        corrected_at=at if correct else None, correction_reason="当天整单误录，货品完整在店" if correct else None, created_at=at)
    allocations = []
    for n, (pid, qty, price, alloc) in enumerate(lines, 1):
        iid = len(rows["sale_item"])+1
        add("sale_item", id=iid, sale_id=sid, line_no=n, **snapshot(pid), unit_price=D(price), quantity=qty)
        assert sum(q for bid,q in alloc) == qty
        for bid, q in alloc:
            aid = len(rows["sale_allocation"])+1
            add("sale_allocation", id=aid, sale_item_id=iid, product_id=pid, batch_item_id=bid, quantity=q)
            move("SALE_OUT", bid, -q, s["order_no"], at, actor=cashier, allocation=aid)
            allocations.append((aid,bid,q))
    if correct:
        for aid,bid,q in allocations:
            move("SALE_CORRECTION", bid, q, s["order_no"], at, actor=1, allocation=aid, reason=s["correction_reason"])
        log("SALE_CORRECT", "SALE", sid, s["order_no"], at=at, reason=s["correction_reason"], before={"status":"COMPLETED"}, after={"status":"CORRECTED"})
    return s


sale(1, [(5,3,"68",[(byproduct[5][0],3)]),(7,2,"12",[(byproduct[7][0],2)])], M2)
sale(2, [(5,2,"68",[(byproduct[5][0],2)]),(8,3,"6",[(byproduct[8][0],3)])], M1, cashier=6, method="MOCK_EPAY")
sale(3, [(23,2,"7.50",[(byproduct[23][0],2)])], when(-5), cashier=6)
sale(4, [(1,12,"5",[(byproduct[1][0],10),(byproduct[1][1],2)])], TODAY, received="100")
sale(5, [(2,2,"6",[(byproduct[2][0],2)])], TODAY, cashier=6, method="MOCK_EPAY")
sale(6, [(7,1,"12",[(byproduct[7][0],1)])], TODAY, cashier=6, correct=True)


def stocktake(oid, entries, status="COMPLETED", actor=3, at=TODAY):
    """entries=(batch,actual,reasonType,note)，book由当前事件模型产生。"""
    o = add("stocktake_order", id=oid, order_no=f"PD-DEMO-{oid:04d}", operator_id=actor, operator_name=users[actor], status=status,
        started_at=at, submitted_at=at if status in ["PENDING","REJECTED","COMPLETED"] else None,
        reviewed_by=1 if status in ["REJECTED","COMPLETED"] else None, reviewer_name=users[1] if status in ["REJECTED","COMPLETED"] else None,
        reviewed_at=at if status in ["REJECTED","COMPLETED"] else None, rejection_reason="实盘说明待补充核实" if status == "REJECTED" else None,
        completed_at=at if status == "COMPLETED" else None, closed_by=1 if status == "CLOSED" else None,
        closed_at=at if status == "CLOSED" else None, close_reason="取消本次盘点并释放冻结" if status == "CLOSED" else None,
        remark="测试冻结、空值、差异、审核和关闭", created_at=at, updated_at=at)
    pids = set()
    for bid, actual, reason, note in entries:
        b = batches[bid]
        pid = b["product_id"]
        pids.add(pid)
        iid = len(rows["stocktake_item"])+1
        book = b["stock_qty"]
        add("stocktake_item", id=iid, stocktake_id=oid, batch_item_id=bid, product_id=pid, book_qty=book, actual_qty=actual,
            reason_type=reason, reason_note=note)
        if status == "COMPLETED" and actual != book:
            move("STOCKTAKE_GAIN" if actual > book else "STOCKTAKE_LOSS", bid, actual-book, o["order_no"], at, stitem=iid, reason=note)
    if status in ["COUNTING","PENDING","REJECTED"]:
        for pid in pids:
            assert products[pid]["frozen_stocktake_id"] is None
            products[pid]["frozen_stocktake_id"] = oid
            products[pid]["stock_version"] += 1
    log("STOCKTAKE_" + ("APPROVE" if status == "COMPLETED" else status), "STOCKTAKE", oid, o["order_no"],
        actor=1 if status in ["COMPLETED","REJECTED","CLOSED"] else actor, at=at, after={"status":status,"productIds":sorted(pids)})
    return o


stocktake(1, [(byproduct[1][1],7,"LOSS","破损损耗一瓶"), (byproduct[7][0],40,"COUNT_ERROR","复核原先漏记两件"),
              (byproduct[5][0],45,None,None), (expired[1],0,"EXPIRY","过期实物已完整清理")])
sale(7, [(1,2,"5",[(byproduct[1][1],2)])], TODAY)
stocktake(2, [(byproduct[10][0],None,None,None)], status="COUNTING")
stocktake(3, [(byproduct[11][0],10,None,None)], status="PENDING", actor=4)
stocktake(4, [(byproduct[12][0],11,"LOSS","盘亏待复核")], status="REJECTED")
stocktake(5, [(byproduct[8][0],27,None,None)], status="CLOSED", actor=4)


def reconciliation(oid, status, difference=False):
    o = orders[oid]
    lines = [b for b in batches.values() if b["purchase_id"] == oid]
    ext = D(0)
    snapshots = []
    for n,b in enumerate(lines):
        q = b["quantity"] + (1 if difference and n == 0 else 0)
        b.update(external_qty=q, external_unit_cost=b["unit_cost"], reconcile_note="凭据数量多一件，待核查" if difference and n == 0 else "一致")
        ext += q*b["unit_cost"]
        snapshots.append(dict(itemId=b["id"], externalQty=q, externalUnitCost=str(b["unit_cost"])))
    o.update(reconcile_status=status, external_total_amount=ext, reconcile_difference="供应商凭据数量多一件，等待核实" if difference else None,
             reconcile_resolution=None if difference else "逐行核验一致", reconciled_by=o["operator_id"], reconciled_at=TODAY, reconcile_version=1)
    log("RECONCILE_DIFFERENCE" if difference else "RECONCILE_MATCH", "PURCHASE", oid, o["order_no"], actor=o["operator_id"],
        before={"status":"UNRECONCILED"}, after={"status":status,"externalTotal":str(ext),"lines":snapshots})


reconciliation(1,"MATCHED")
reconciliation(3,"DIFFERENCE",True)
# 保留一次实际撤回历史，当前恢复未核对，原核对行数据全在JSON历史中。
reconciliation(2,"MATCHED")
old_o = orders[2]
old_lines = [b for b in batches.values() if b["purchase_id"] == 2]
log("RECONCILE_WITHDRAW", "PURCHASE", 2, old_o["order_no"], actor=3, reason="重新检查供应商纸质凭据", before={
    "status":"MATCHED","externalTotal":str(old_o["external_total_amount"]),
    "lines":[dict(itemId=b["id"],externalQty=b["external_qty"],externalUnitCost=str(b["external_unit_cost"])) for b in old_lines]},
    after={"status":"UNRECONCILED"})
old_o.update(reconcile_status="UNRECONCILED", external_total_amount=None, reconcile_resolution=None, reconciled_by=None, reconciled_at=None, reconcile_version=2)
for b in old_lines:
    b.update(external_qty=None,external_unit_cost=None,reconcile_note=None)

log("PRODUCT_PRICE_CHANGE", "PRODUCT", 23, products[23]["code"], reason="验证历史售价不随新售价变化", before={"salePrice":"7.50"}, after={"salePrice":"9.00"})
for pid in [13,14,15]:
    log("PRODUCT_STATUS_CHANGE", "PRODUCT", pid, products[pid]["code"], before={"enabled":1,"canPurchase":1,"canSell":1},
        after={"enabled":products[pid]["enabled"],"canPurchase":products[pid]["can_purchase"],"canSell":products[pid]["can_sell"]})

# 按真实事件时间回放，得到时间连续且每一步可核对的前后余额。
# SQL日期保持相对导入日，rank只用于生成时排序；同一时点按原始事件顺序。
time_rank = {str(OLD):-1000,str(M2):-900,str(M1):-800,str(TODAY):0}
for offset in [-15,-5,-3,-2,-1]:
    time_rank[str(when(offset))] = offset
rows["stock_movement"].sort(key=lambda m:(time_rank[str(m["occurred_at"])],m["id"]))
replay_batch = defaultdict(int)
replay_product = defaultdict(int)
for mid,m in enumerate(rows["stock_movement"],1):
    bid,pid,d = m["batch_item_id"],m["product_id"],m["delta_qty"]
    m.update(id=mid,batch_before=replay_batch[bid],batch_after=replay_batch[bid]+d,
             product_before=replay_product[pid],product_after=replay_product[pid]+d)
    assert m["batch_after"] >= 0 and m["product_after"] >= 0
    replay_batch[bid] += d
    replay_product[pid] += d
assert dict(replay_product) == dict(balances)
for p in products.values():
    p["stock_qty"] = balances[p["id"]]

for aid, typ, actor, status, error in [(1,"INVENTORY",3,"SUCCESS",None),(2,"SALES",1,"SUCCESS",None),
    (3,"INVENTORY",3,"FAILURE","TIMEOUT"),(4,"SALES",1,"FAILURE","NOT_CONFIGURED"),
    (5,"INVENTORY",4,"FAILURE","RATE_LIMITED"),(6,"INVENTORY",3,"FAILURE","EMPTY_RESULT")]:
    summary = SQL("JSON_OBJECT('fixture',TRUE,'productId',1,'bookQty'," + str(balances[1]) + ", 'saleableQty'," + str(balances[1]) +
                  ", 'lowerLimit',5,'periodNetQty',14,'periodNetAmount',70.00,'capturedAt',DATE_FORMAT(@seed_now,'%Y-%m-%d %H:%i:%s.%f'))") if typ == "INVENTORY" else SQL(
                  "JSON_OBJECT('fixture',TRUE,'netQty',(SELECT COALESCE(SUM(signed_qty),0) FROM v_sale_event WHERE event_at>=DATE_SUB(@seed_day,INTERVAL 29 DAY)),"
                  "'netAmount',(SELECT COALESCE(SUM(signed_amount),0) FROM v_sale_event WHERE event_at>=DATE_SUB(@seed_day,INTERVAL 29 DAY)))")
    add("ai_suggestion", id=aid, request_key=f"seed-ai-{aid}", suggestion_type=typ, operator_id=actor, operator_name=users[actor],
        period_start=day(-29), period_end=day(), filter_data={"productIds":[1]} if typ == "INVENTORY" else {}, input_summary=summary,
        snapshot_at=TODAY, provider="FIXTURE", model_name="fixture-not-api", is_fixture=1, status=status,
        content="【初始测试数据，非真实模型调用】库存5瓶、下限5瓶，近期净销量14瓶；请人工核实补货数量。" if status == "SUCCESS" and typ == "INVENTORY" else
                "【初始测试数据，非真实模型调用】请结合输入摘要查看销售与商品排行，并人工核查滞销原因。" if status == "SUCCESS" else None,
        error_code=error, error_message="测试降级记录："+error if error else None, duration_ms=15000 if error == "TIMEOUT" else 0,
        requested_at=TODAY, finished_at=TODAY)
    log("AI_"+status, "AI", aid, f"seed-ai-{aid}", actor=actor, after={"isFixture":True,"status":status,"type":typ})

# SQL按外键依赖顺序输出；冻结指针待盘点表记录插入后更新，保持外键开启。
table_order = ["sys_role","sys_user","sys_config","category","supplier","product","purchase_order","purchase_item",
               "sale_order","sale_item","sale_allocation","stocktake_order","stocktake_item","stock_movement","operation_log","ai_suggestion"]
header = """-- 全量功能测试初始数据；仅向空项目库导入一次，不覆盖已有数据。
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
"""
chunks = [header]
freeze_updates = []
for p in products.values():
    if p["frozen_stocktake_id"]:
        freeze_updates.append(f"UPDATE product SET frozen_stocktake_id={p['frozen_stocktake_id']} WHERE id={p['id']};")
        p["frozen_stocktake_id"] = None
for table in table_order:
    records = rows[table]
    columns = list(dict.fromkeys(k for r in records for k in r))
    # 不同字典省略的字段必须使用DEFAULT而不是NULL，保证版本等默认值。
    values = ["(" + ",".join(sql(r[k]) if k in r else "DEFAULT" for k in columns) + ")" for r in records]
    chunks.append(f"\n-- {table}: {len(records)} 条\nINSERT INTO {table} ({','.join(columns)}) VALUES\n" + ",\n".join(values) + ";\n")
chunks.extend(["\n-- 盘点单已存在后设置活动冻结。\n", "\n".join(freeze_updates), "\nCOMMIT;\n",
               "SELECT 'seed_ready' result,@seed_day seed_date,(SELECT COUNT(*) FROM product) products,(SELECT COUNT(*) FROM stock_movement) movements;\n"])
destination = Path(tempfile.gettempdir()) / "supermarket_seed_preview.sql"
destination.write_text("".join(chunks), encoding="utf-8", newline="\n")
print("Historical seed preview only; not the authoritative initialization file:", destination)
for t in table_order:
    print(f"{t}: {len(rows[t])}")
