"""Import the sole SQL baseline into an explicitly isolated MySQL test database.

Requires PyMySQL. DB_PASSWORD is read from the environment. Development databases
are rejected; --reset authorizes replacement of this test database only.
"""
import argparse
import os
import re
from pathlib import Path
import pymysql
from pymysql.constants import CLIENT

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--database', default='supermarket_inventory_test')
parser.add_argument('--reset', action='store_true')
args = parser.parse_args()
if not re.fullmatch(r'supermarket_inventory_[a-z0-9_]*(test|dbcheck)', args.database):
    parser.error('Only isolated supermarket_inventory_*test / *dbcheck databases are allowed')
conn = pymysql.connect(host=os.getenv('DB_HOST', '127.0.0.1'),
    user=os.getenv('DB_USER', 'root'), password=os.environ['DB_PASSWORD'],
    charset='utf8mb4', client_flag=CLIENT.MULTI_STATEMENTS)
with conn.cursor() as cursor:
    cursor.execute('SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=%s', (args.database,))
    if cursor.fetchone()[0] and not args.reset:
        parser.error('Test database is not empty. Pass --reset only when its replacement is intended.')
    if args.reset:
        cursor.execute(f'DROP DATABASE IF EXISTS `{args.database}`')
    cursor.execute(f'CREATE DATABASE IF NOT EXISTS `{args.database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci')
conn.select_db(args.database)
baseline = next((Path(__file__).resolve().parents[1] / 'doc').rglob('supermarket_inventory.sql'))
try:
    with conn.cursor() as cursor:
        cursor.execute(baseline.read_text(encoding='utf-8'))
        while cursor.nextset():
            pass
    conn.commit()
    with conn.cursor() as cursor:
        cursor.execute('SELECT table_type,COUNT(*) FROM information_schema.tables WHERE table_schema=%s GROUP BY table_type', (args.database,))
        counts = dict(cursor.fetchall())
        assert counts == {'BASE TABLE': 16, 'VIEW': 8}, counts
        cursor.execute('SELECT @@foreign_key_checks')
        assert cursor.fetchone()[0] == 1
        for view in ['v_batch_inventory', 'v_product_inventory', 'v_cashier_inventory', 'v_category_inventory', 'v_low_stock', 'v_sale_event', 'v_purchase_event', 'v_reconcile_history']:
            cursor.execute(f'SELECT COUNT(*) FROM `{view}`')
            cursor.fetchone()
        print(f'{args.database}: 16 tables, 8 readable views, FK checks enabled')
finally:
    conn.close()
