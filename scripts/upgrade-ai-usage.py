"""Explicit, additive AI usage schema upgrade. Never imports or resets a database.

Back up the selected database first. Supply --database explicitly. Test and business
databases are allowed only under the supermarket_inventory naming prefix. The app
never invokes this tool automatically; existing usage remains NULL (unknown).
"""
import argparse
import os
import re
import pymysql

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--database', required=True)
args = parser.parse_args()
if not re.fullmatch(r'supermarket_inventory(?:_[a-z0-9_]+)?', args.database):
    parser.error('Select an explicit supermarket_inventory database')

connection = pymysql.connect(host=os.getenv('DB_HOST', '127.0.0.1'), user=os.getenv('DB_USER', 'root'),
    password=os.environ['DB_PASSWORD'], database=args.database, autocommit=True, connect_timeout=5, read_timeout=30)
try:
    with connection.cursor() as cursor:
        cursor.execute('SELECT DATABASE()')
        assert cursor.fetchone()[0] == args.database
        cursor.execute("SELECT COLUMN_NAME FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ai_suggestion'")
        columns = {r[0] for r in cursor.fetchall()}
        assert {'id', 'is_fixture', 'requested_at'} <= columns, 'Expected baseline table missing'
        for column, definition in [
            ('prompt_tokens', 'BIGINT NULL DEFAULT NULL'), ('completion_tokens', 'BIGINT NULL DEFAULT NULL'),
            ('total_tokens', 'BIGINT NULL DEFAULT NULL'), ('cached_tokens', 'BIGINT NULL DEFAULT NULL'),
            ('provider_request_id', 'VARCHAR(128) NULL DEFAULT NULL')]:
            if column not in columns:
                cursor.execute(f'ALTER TABLE ai_suggestion ADD COLUMN {column} {definition}')
        cursor.execute("SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='ai_suggestion' AND constraint_name='ck_ai_usage'")
        if not cursor.fetchone()[0]:
            cursor.execute('''ALTER TABLE ai_suggestion ADD CONSTRAINT ck_ai_usage CHECK (
                (prompt_tokens IS NULL AND completion_tokens IS NULL AND total_tokens IS NULL AND cached_tokens IS NULL)
                OR (prompt_tokens IS NOT NULL AND completion_tokens IS NOT NULL AND total_tokens IS NOT NULL
                    AND prompt_tokens>=0 AND completion_tokens>=0 AND total_tokens=prompt_tokens+completion_tokens
                    AND (cached_tokens IS NULL OR (cached_tokens>=0 AND cached_tokens<=prompt_tokens))))''')
        cursor.execute("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='ai_suggestion' AND index_name='ix_ai_usage'")
        if not cursor.fetchone()[0]:
            cursor.execute('ALTER TABLE ai_suggestion ADD INDEX ix_ai_usage(is_fixture,requested_at,model_name)')
    print(f'{args.database}: additive AI usage upgrade complete; existing rows preserved, no usage fabricated')
finally:
    connection.close()
