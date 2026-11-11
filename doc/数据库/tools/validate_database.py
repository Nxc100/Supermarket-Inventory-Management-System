"""验证数据库约束及事务协议，不需要SpringBoot/Vue。

只允许在名称以_dbcheck结尾的验证库中执行；写操作一律回滚。
依赖PyMySQL；BCrypt摘要验证还需bcrypt。密码从MYSQL_PWD读取或交互输入。
"""
import argparse
import datetime
import getpass
import json
import os
from pathlib import Path

import bcrypt
import pymysql

parser = argparse.ArgumentParser()
parser.add_argument("--host", default="127.0.0.1")
parser.add_argument("--port", type=int, default=3306)
parser.add_argument("--user", default="root")
parser.add_argument("--database", default="supermarket_inventory_dbcheck")
args = parser.parse_args()
if not args.database.endswith("_dbcheck"):
    raise SystemExit("只允许在独立的 *_dbcheck 验证库执行")
password = os.environ.get("MYSQL_PWD") or getpass.getpass("MySQL password: ")


def connect():
    c = pymysql.connect(host=args.host, port=args.port, user=args.user, password=password,
                        database=args.database, charset="utf8mb4", autocommit=False)
    with c.cursor() as q:
        q.execute("SET time_zone='+08:00'")
    return c


c = connect()
results = []
with c.cursor() as q:
    q.execute("SELECT VERSION()")
    mysql_version = q.fetchone()[0]


def record(name, ok, detail):
    results.append(dict(test=name, result="PASS" if ok else "FAIL", detail=detail))


def reject(name, statement, error_code, parameters=None):
    c.rollback()
    try:
        with c.cursor() as q:
            q.execute(statement, parameters)
        record(name, False, "数据库意外接受非法数据")
    except pymysql.MySQLError as e:
        record(name, e.args[0] == error_code, "expected=%s,actual=%s" % (error_code,e.args[0]))
    finally:
        c.rollback()


def scalar(query, parameters=None):
    with c.cursor() as q:
        q.execute(query, parameters)
        return q.fetchone()[0]


try:
    # 直接执行交付的只读一致性SQL，而非复制一份不同规则。
    verification = (Path(__file__).resolve().parent.parent / "03_verify.sql").read_text(encoding="utf-8")
    check_sql = verification[verification.index("WITH checks AS"):verification.index("-- 场景清单")].strip().rstrip(";")
    with c.cursor() as q:
        q.execute(check_sql)
        for name, violations, status in q.fetchall():
            record(name, int(violations)==0, "violations="+str(violations))
    hashes = []
    with c.cursor() as q:
        q.execute("SELECT password_hash FROM sys_user")
        hashes = [r[0] for r in q.fetchall()]
    record("N01_bcrypt_passwords", all(bcrypt.checkpw(b"Test123456",h.encode()) for h in hashes), "8个测试账号摘要逐个验证")
    reject("N02_unique_username", "UPDATE sys_user SET username='admin' WHERE id=8",1062)
    reject("N03_unique_barcode", "UPDATE product SET barcode=(SELECT barcode FROM (SELECT barcode FROM product WHERE id=1) x) WHERE id=22",1062)
    reject("N04_nonnegative_product", "UPDATE product SET stock_qty=-1 WHERE id=9",3819)
    reject("N05_nonnegative_batch", "UPDATE purchase_item SET stock_qty=-1 WHERE id=1",3819)
    reject("N06_movement_arithmetic", "UPDATE stock_movement SET batch_after=batch_after+1 WHERE id=1",3819)
    reject("N07_cross_product_allocation", "INSERT INTO sale_allocation (sale_item_id,product_id,batch_item_id,quantity) SELECT sale_item_id,product_id,1,quantity FROM sale_allocation WHERE id=1",1452)
    reject("N08_partial_external_values", "UPDATE purchase_item SET external_qty=1,external_unit_cost=NULL WHERE id=1",3819)
    reject("N09_invalid_expiry", "UPDATE purchase_item SET expiry_date=DATE_SUB(production_date,INTERVAL 1 DAY) WHERE id=1",3819)
    reject("N10_negative_actual", "UPDATE stocktake_item SET actual_qty=-1 WHERE id=5",3819)
    reject("N11_missing_difference_reason", "UPDATE stocktake_item SET actual_qty=book_qty+1,reason_type=NULL,reason_note=NULL WHERE id=5",3819)
    reject("N12_empty_ai_success", "UPDATE ai_suggestion SET content='' WHERE id=1",3819)
    reject("N13_duplicate_idempotency", "UPDATE sale_order SET request_key='seed-sale-request-1' WHERE id=2",1062)
    reject("N14_cross_day_correction", "UPDATE sale_order SET status='CORRECTED',corrected_by=1,corrected_at=DATE_ADD(completed_at,INTERVAL 1 DAY),correction_reason='跨日非法纠错' WHERE id=1",3819)
    reject("N15_cashier_exceeds_ceiling", "UPDATE sys_role SET permissions=JSON_ARRAY_APPEND(permissions,'$','purchase.order.review') WHERE code='CASHIER'",3819)
    reject("N16_delete_referenced_product", "DELETE FROM product WHERE id=1",1451)
    reject("N17_delete_referenced_category", "DELETE FROM category WHERE id=1",1451)
    # 防重账本：复制一条合法流水，但不复制自增ID，唯一来源键应拒绝。
    movement_columns="event_type,product_id,batch_item_id,sale_allocation_id,stocktake_item_id,business_no,delta_qty,batch_before,batch_after,product_before,product_after,actor_id,actor_name,reason,occurred_at"
    reject("N18_duplicate_stock_event",f"INSERT INTO stock_movement ({movement_columns}) SELECT {movement_columns} FROM stock_movement WHERE id=1",1062)
    reject("N19_missing_reverse_reason", "UPDATE purchase_order SET reverse_reason=NULL WHERE id=4",3819)
    reject("N20_stocked_batch_missing_received", "UPDATE purchase_item SET received_qty=NULL WHERE id=1",3819)
    # NULL与0区分：未填实盘变成有效的0，正确产生负差异，但回滚不修改夹具。
    c.rollback()
    is_null=scalar("SELECT actual_qty IS NULL FROM stocktake_item WHERE id=5")
    with c.cursor() as q:
        q.execute("UPDATE stocktake_item SET actual_qty=0,reason_type='LOSS',reason_note='测试有效零数量' WHERE id=5")
    diff=scalar("SELECT difference_qty FROM stocktake_item WHERE id=5")
    record("N21_null_vs_zero",is_null==1 and diff==-20,"NULL代表未填；0产生-20差异")
    c.rollback()
    record("N22_freeze_preserves_saleable",scalar("SELECT is_frozen=1 AND saleable_qty=20 FROM v_product_inventory WHERE id=10")==1,"冻结只禁交易，不制造零可售库存")
    # 固定在夹具真实到期日与次日，避免隔天重跑把自然老化误报成回归。
    # 只改变当前连接的时钟，不修改数据、系统时间或MySQL全局设置。
    expiry_day=scalar("SELECT UNIX_TIMESTAMP(CONCAT(MIN(expiry_date),' 12:00:00')) FROM purchase_item WHERE product_id=18 AND stock_qty>0")
    try:
        with c.cursor() as q:
            q.execute("SET timestamp=%s",(expiry_day,))
        before=scalar("SELECT saleable_qty FROM v_product_inventory WHERE id=18")
        with c.cursor() as q:
            q.execute("SET timestamp=%s",(expiry_day+86400,))
        after=scalar("SELECT saleable_qty FROM v_product_inventory WHERE id=18")
        book=scalar("SELECT book_qty FROM v_product_inventory WHERE id=18")
        record("N23_dynamic_expiry_without_job",before==4 and after==0 and book==4,"次日禁售但账面仍4，无自动清零")
    finally:
        with c.cursor() as q:
            q.execute("SET timestamp=DEFAULT")
    c.rollback()
    # 事务异常回滚：余额和批次先减，再制造错误；整组回滚后均恢复。
    initial_product=scalar("SELECT stock_qty FROM product WHERE id=9")
    bid=scalar("SELECT id FROM purchase_item WHERE product_id=9 AND batch_no IS NOT NULL LIMIT 1")
    try:
        with c.cursor() as q:
            q.execute("UPDATE product SET stock_qty=stock_qty-4 WHERE id=9")
            q.execute("UPDATE purchase_item SET stock_qty=stock_qty-4 WHERE id=%s",(bid,))
            q.execute("UPDATE product SET stock_qty=-1 WHERE id=9")
        raise AssertionError("expected constraint error")
    except pymysql.MySQLError:
        c.rollback()
    record("N24_bundle_rollback",scalar("SELECT stock_qty FROM product WHERE id=9")==initial_product and scalar("SELECT stock_qty FROM purchase_item WHERE id=%s",(bid,))==initial_product,"余额和批次均恢复到5")
    c.rollback()
    # 两连接真实行锁竞争：第二连接限时等待；不提交任何库存变更。
    c2=connect()
    try:
        with c.cursor() as q:
            q.execute("SELECT id FROM product WHERE id=9 FOR UPDATE")
        with c2.cursor() as q:
            q.execute("SET innodb_lock_wait_timeout=1")
            try:
                q.execute("SELECT id FROM product WHERE id=9 FOR UPDATE")
                record("N25_product_lock_competition",False,"第二连接意外取得同一排他行锁")
            except pymysql.MySQLError as e:
                record("N25_product_lock_competition",e.args[0]==1205,"第二连接等待超时1205，事务协议可串行化库存写")
    finally:
        c2.rollback();c2.close();c.rollback()
    # 未引用样例确实可删除，回滚后保留可供前端测试。
    with c.cursor() as q:
        q.execute("DELETE FROM product WHERE id=22")
        q.execute("DELETE FROM supplier WHERE id=8")
        q.execute("DELETE FROM sys_user WHERE id=8")
        q.execute("DELETE FROM category WHERE id=6")
    record("N26_delete_unreferenced",True,"未引用商品、供应商、用户、分类均可物理删除")
    c.rollback()
    record("N27_split_sale_no_double_amount",scalar("SELECT SUM(signed_amount) FROM v_sale_event WHERE sale_id=4")==60,"12瓶跨两批次，销售额仍为60元")
    record("N28_correction_net_zero",scalar("SELECT SUM(signed_amount) FROM v_sale_event WHERE sale_id=6")==0,"原事件和反向事件并存，净额0")
    record("N29_price_snapshot",scalar("SELECT unit_price FROM sale_item WHERE product_id=23")==7.5 and scalar("SELECT sale_price FROM product WHERE id=23")==9,"历史7.50，当前9.00")
    record("N30_fixture_flag",scalar("SELECT COUNT(*) FROM ai_suggestion WHERE is_fixture=1 AND provider='FIXTURE'")==6,"所有AI种子明确标记为夹具")
finally:
    c.rollback()
    c.close()

report={"database":args.database,"mysql_version":mysql_version,
        "validated_at":datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat(),
        "passed":sum(r["result"]=="PASS" for r in results),"failed":sum(r["result"]=="FAIL" for r in results),"checks":results}
report_path=Path(__file__).resolve().parent.parent/"验证结果.json"
report_path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print(json.dumps({k:v for k,v in report.items() if k!="checks"},ensure_ascii=False))
for item in results:
    if item["result"]!="PASS":
        print(item)
raise SystemExit(1 if report["failed"] else 0)
