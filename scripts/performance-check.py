"""Repeatable local capacity check using real HTTP business commands in a dedicated database.

First run prepare-test-db.py --database supermarket_inventory_performance_test.
Requires requests, PyMySQL, JDK 17, a packaged backend and DB_PASSWORD. Never resets
or deletes a database. Re-running resumes only this script's deterministic fixtures;
unknown HTTP write results stop the run and are verified from persisted state next run.
"""
import argparse
import datetime as dt
import json
import math
import os
from pathlib import Path
import platform
import secrets
import socket
import statistics
import subprocess
import time
import uuid

import pymysql
import requests

ROOT = Path(__file__).resolve().parents[1]
DATABASE = 'supermarket_inventory_performance_test'
NAMESPACE = uuid.UUID('2a7f30ed-fd60-4fa1-a339-8dafb0be46c1')
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--java-home', default=os.environ.get('JAVA_HOME'))
parser.add_argument('--port', type=int, default=8081)
parser.add_argument('--measure-only', action='store_true')
args = parser.parse_args()
if not args.java_home or not 1024 <= args.port <= 65535:
    parser.error('Provide a JDK 17 directory and an unprivileged local port')
java = Path(args.java_home) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')
jar = ROOT / 'backend/target/inventory-1.0.0-SNAPSHOT.jar'
if not java.is_file() or not jar.is_file():
    parser.error('JDK executable or packaged backend is missing')
with socket.socket() as probe:
    probe.bind(('127.0.0.1', args.port))  # Refuse to use or stop an existing server.
db = pymysql.connect(host='127.0.0.1', user=os.getenv('DB_USER', 'root'),
    password=os.environ['DB_PASSWORD'], database=DATABASE, charset='utf8mb4',
    autocommit=True, connect_timeout=5, read_timeout=15,
    cursorclass=pymysql.cursors.DictCursor)


def rows(sql, *params):
    with db.cursor() as cursor:
        cursor.execute(sql, params if params else None)
        return cursor.fetchall()


assert rows('SELECT DATABASE() AS name')[0]['name'] == DATABASE
cashier_id = rows("SELECT id FROM sys_user WHERE username='cashier01'")[0]['id']
assert rows("SELECT COUNT(*) AS n FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE'")[0]['n'] == 16
output = ROOT / '.local'
output.mkdir(exist_ok=True)
report = {'database': DATABASE, 'startedAt': dt.datetime.now(dt.timezone.utc).isoformat(),
    'environment': {'os': platform.platform(), 'cpu': platform.processor(),
        'logicalProcessors': os.cpu_count(), 'java': str(java),
        'mysql': rows('SELECT @@version AS version')[0]['version'],
        'javaVersion': subprocess.run([str(java), '-version'], capture_output=True, text=True, check=True).stderr.splitlines()[0]},
    'scope': 'Single local client, HTTP API including serialization and database commit; excludes AI and browser rendering.',
    'targetMs': 2000, 'status': 'RUNNING', 'measurements': {}}
env = os.environ.copy()
env.update({'DB_URL': f'jdbc:mysql://127.0.0.1:3306/{DATABASE}?connectionTimeZone=%2B08:00&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8',
    'SERVER_PORT': str(args.port), 'SERVER_ADDRESS': '127.0.0.1',
    'SPRING_PROFILES_ACTIVE': 'performance', 'JWT_SECRET': secrets.token_urlsafe(48)})
session = requests.Session()
session.trust_env = False
base = f'http://127.0.0.1:{args.port}/api/v1'
tokens = {}
server_log = (output / 'performance-server.log').open('w', encoding='utf8')
process = subprocess.Popen([str(java), '-jar', str(jar)], cwd=ROOT / 'backend', env=env,
    stdout=server_log, stderr=subprocess.STDOUT,
    creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)


def api(method, path, body=None, actor='admin'):
    started = time.perf_counter()
    response = session.request(method, base + path, json=body,
        headers={'Authorization': 'Bearer ' + tokens[actor]} if actor else {}, timeout=(5, 20))
    elapsed = (time.perf_counter() - started) * 1000
    payload = response.json()
    if response.status_code != 200 or payload.get('code') != 'OK':
        raise RuntimeError(f'{method} {path}: HTTP {response.status_code}, {payload.get("message", "Request failed")}')
    return payload['data'], elapsed


def command(document, action, actor):
    return api('POST', f'/purchases/{document["order"]["id"]}/{action}',
        {'expectedVersion': document['order']['rowVersion']}, actor)[0]


def sale(index, products):
    key = str(uuid.uuid5(NAMESPACE, f'sale-{index}'))
    existing = rows('SELECT id FROM sale_order WHERE request_key=%s AND cashier_id=%s', key, cashier_id)
    if existing:
        return None
    body = {'requestKey': key, 'paymentMethod': 'CASH', 'receivedAmount': '10.00',
        'items': [{'productId': products[index % 300], 'quantity': 1, 'expectedPrice': '5.00'}]}
    quote_ms = api('POST', '/sales/quote', {'items': body['items']}, 'cashier01')[1]
    return quote_ms + api('POST', '/sales/complete', body, 'cashier01')[1]


try:
    deadline = time.monotonic() + 30
    while True:
        if process.poll() is not None:
            raise RuntimeError('Performance server exited; inspect .local/performance-server.log')
        try:
            if session.get(base + '/auth/me', timeout=(1, 1)).status_code == 401:
                break
        except requests.RequestException:
            pass
        if time.monotonic() >= deadline:
            raise RuntimeError('Performance server did not become ready within 30 seconds')
        time.sleep(.2)
    for actor in ['admin', 'buyer01', 'cashier01']:
        tokens[actor] = api('POST', '/auth/login', {'username': actor, 'password': 'Test123456'}, None)[0]['token']
    # No direct INSERT/UPDATE of stock, batches, business documents or ledger rows.
    if not args.measure_only:
        for index in range(20):
            name = f'PERF_SUPPLIER_{index:03}'
            if not rows('SELECT id FROM supplier WHERE name=%s', name):
                api('POST', '/suppliers', {'name': name, 'contactName': '容量验证',
                    'contactPhone': '0710-1234567', 'remark': 'Dedicated performance fixture'}, 'buyer01')
        for index in range(300):
            code = f'PERF_PRODUCT_{index:03}'
            if not rows('SELECT id FROM product WHERE code=%s', code):
                api('POST', '/products', {'code': code, 'name': f'容量验证商品{index:03}',
                    'categoryId': '1', 'unit': '瓶', 'referenceCost': '3.00', 'salePrice': '5.00',
                    'managesExpiry': False, 'nearExpiryDays': 7, 'stockLowerLimit': 5,
                    'canPurchase': True, 'canSell': True})
    products = [str(r['id']) for r in rows("SELECT id FROM product WHERE code LIKE 'PERF\\_PRODUCT\\_%' ORDER BY code")]
    suppliers = [str(r['id']) for r in rows("SELECT id FROM supplier WHERE name LIKE 'PERF\\_SUPPLIER\\_%' ORDER BY name")]
    assert len(products) == 300 and len(suppliers) == 20, 'Complete the capacity fixtures before measuring'
    if not args.measure_only:
        for index in range(1000):
            remark = f'PERF_PURCHASE_{index:04}'
            existing = rows('SELECT id FROM purchase_order WHERE remark=%s', remark)
            assert len(existing) <= 1, 'Duplicate fixture marker requires review'
            if existing:
                document = api('GET', f'/purchases/{existing[0]["id"]}', actor='buyer01')[0]
            else:
                arrived = (dt.datetime.now(dt.timezone(dt.timedelta(hours=8))) - dt.timedelta(seconds=1)).isoformat()
                document = api('POST', '/purchases', {'supplierId': suppliers[index % 20],
                    'arrivedAt': arrived, 'remark': remark, 'items': [{'productId': products[index % 300],
                    'quantity': 100, 'receivedQty': 100, 'unitCost': '3.00'}]}, 'buyer01')[0]
            if document['order']['status'] == 'DRAFT':
                document = command(document, 'submit', 'buyer01')
            if document['order']['status'] == 'PENDING':
                document = command(document, 'approve', 'admin')
            assert document['order']['status'] == 'IN_STOCK', 'Unexpected fixture state requires review'
            if index % 100 == 99:
                print(f'Approved capacity purchases: {index + 1}/1000', flush=True)
        for index in range(5000):
            sale(index, products)
            if index % 500 == 499:
                print(f'Completed capacity sales: {index + 1}/5000', flush=True)
    assert rows("SELECT COUNT(*) AS n FROM purchase_order WHERE remark LIKE 'PERF\\_PURCHASE\\_%' AND status='IN_STOCK'")[0]['n'] == 1000
    assert rows('SELECT COUNT(*) AS n FROM sale_order so JOIN sale_item si ON si.sale_id=so.id JOIN product p ON p.id=si.product_id WHERE p.code LIKE %s', 'PERF\\_PRODUCT\\_%')[0]['n'] >= 5000
    for label, path in [('products', '/products?pageNum=1&pageSize=20&keyword=PERF'),
        ('purchases', '/purchases?pageNum=2&pageSize=20'), ('sales', '/sales?pageNum=2&pageSize=20'),
        ('inventory', '/inventory/products?pageNum=1&pageSize=20'), ('dashboard', '/dashboard'),
        ('salesStatistics', '/statistics/sales'), ('inventoryStatistics', '/statistics/inventory')]:
        for _ in range(3):
            api('GET', path)
        values = [api('GET', path)[1] for _ in range(30)]
        ordered = sorted(values)
        report['measurements'][label] = {'method': 'GET', 'path': path, 'samples': len(values), 'p50Ms': round(statistics.median(values), 2),
            'p95Ms': round(ordered[math.ceil(len(values) * .95) - 1], 2),
            'maxMs': round(max(values), 2), 'overTarget': sum(v > 2000 for v in values)}
    first_measurement = rows("SELECT COUNT(*) AS n FROM sale_order so JOIN sale_item si ON si.sale_id=so.id JOIN product p ON p.id=si.product_id WHERE p.code LIKE 'PERF\\_PRODUCT\\_%'")[0]['n']
    settlement = [sale(index, products) for index in range(first_measurement, first_measurement + 30)]
    assert all(v is not None for v in settlement)
    settlement.sort()
    report['measurements']['settlement'] = {'method': 'POST quote + POST complete', 'samples': 30, 'p50Ms': round(statistics.median(settlement), 2),
        'p95Ms': round(settlement[28], 2), 'maxMs': round(max(settlement), 2),
        'overTarget': sum(v > 2000 for v in settlement)}
    inconsistencies = rows("""SELECT p.id FROM product p WHERE p.code LIKE 'PERF\\_PRODUCT\\_%' AND (
        p.stock_qty<>(SELECT COALESCE(SUM(i.stock_qty),0) FROM purchase_item i WHERE i.product_id=p.id)
        OR p.stock_qty<>(SELECT COALESCE(SUM(m.delta_qty),0) FROM stock_movement m WHERE m.product_id=p.id)
        OR p.stock_qty<0)""")
    assert not inconsistencies, 'Capacity fixture balances do not match batches and ledger'
    report['consistency'] = {'productsChecked': 300, 'mismatches': 0,
        'netStock': int(rows("SELECT SUM(stock_qty) AS n FROM product WHERE code LIKE 'PERF\\_PRODUCT\\_%'")[0]['n'])}
    report['databaseCounts'] = {table: rows(f'SELECT COUNT(*) AS n FROM {table}')[0]['n']
        for table in ['product', 'supplier', 'purchase_order', 'sale_order', 'stock_movement']}
    report['status'] = 'PASSED' if all(v['overTarget'] == 0 for v in report['measurements'].values()) else 'TARGET_EXCEEDED'
    print(json.dumps(report, ensure_ascii=False, indent=2), flush=True)
except Exception as exc:
    report.update(status='FAILED', error=f'{type(exc).__name__}: {exc}')
    raise
finally:
    report['finishedAt'] = dt.datetime.now(dt.timezone.utc).isoformat()
    (output / 'performance-report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf8')
    session.close()
    db.close()
    process.terminate()  # Only the process started above, never an existing database or app server.
    try:
        process.wait(timeout=15)
    except subprocess.TimeoutExpired:
        process.kill()
        process.wait(timeout=5)
    server_log.close()
