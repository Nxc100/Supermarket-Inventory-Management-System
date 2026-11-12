"""Install bundled demo photos and fill missing image bindings without resetting business data.

Requires PyMySQL and Pillow. Default execution only previews changes. --apply
backs up image columns and updates them in one transaction; --install-only
copies assets without connecting to MySQL. No network access is needed.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import tempfile
from datetime import datetime, timezone

import pymysql
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
MEDIA = ROOT / 'doc/数据库/media'
GENERATED = re.compile(r'[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}\.(jpg|png)')
LEGACY_PATH = '测试图片.jpg'
SQL_START = '-- BEGIN BUNDLED MEDIA SEED'
SQL_END = '-- END BUNDLED MEDIA SEED'


def load_assets(directory: Path = MEDIA) -> list[dict]:
    manifest = json.loads((directory / 'manifest.json').read_text(encoding='utf8'))
    if manifest['version'] != 1:
        raise ValueError('Unsupported media manifest version')
    assets = manifest['assets']
    names, bindings = set(), set()
    for asset in assets:
        name = asset['file']
        if not GENERATED.fullmatch(name) or name in names:
            raise ValueError(f'Invalid or duplicate image filename: {name}')
        names.add(name)
        file = directory / name
        if file.is_symlink() or not file.is_file():
            raise ValueError(f'Missing regular image file: {name}')
        if hashlib.sha256(file.read_bytes()).hexdigest() != asset['sha256']:
            raise ValueError(f'Image checksum mismatch: {name}')
        with Image.open(file) as image:
            image.load()
            mime = {'JPEG': 'image/jpeg', 'PNG': 'image/png'}.get(image.format)
            if (mime != asset['mime'] or image.size != (asset['width'], asset['height'])
                    or mime != ('image/jpeg' if name.endswith('.jpg') else 'image/png')
                    or min(image.size) < 64 or max(image.size) > 4096
                    or file.stat().st_size != asset['size'] or not 0 < asset['size'] <= 2 * 1024 * 1024):
                raise ValueError(f'Invalid image format, dimensions or size: {name}')
        if asset['kind'] not in ('product', 'avatar'):
            raise ValueError('Unsupported image kind')
        for key in asset.get('product_codes', []) + asset.get('usernames', []):
            binding = (asset['kind'], key)
            if binding in bindings:
                raise ValueError(f'Duplicate image binding: {key}')
            bindings.add(binding)
    return assets


def metadata(asset: dict) -> dict:
    return {**{key: asset[key] for key in ('mime', 'size', 'width', 'height', 'sha256')},
            'sourceUrl': asset['source_url'], 'licenseUrl': asset['license_url'],
            'seedVersion': 1, 'seedKey': asset['key']}


def install_assets(assets: list[dict], target: Path, source: Path = MEDIA) -> None:
    target.mkdir(parents=True, exist_ok=True)
    for asset in assets:
        destination = target / asset['file']
        if destination.is_symlink():
            raise ValueError(f'Image destination is a symlink: {asset["file"]}')
        if destination.exists():
            if hashlib.sha256(destination.read_bytes()).hexdigest() != asset['sha256']:
                raise ValueError(f'Image destination conflicts with bundled asset: {asset["file"]}')
            continue
        with tempfile.NamedTemporaryFile(dir=target, prefix='media-', suffix='.tmp', delete=False) as stream:
            temporary = Path(stream.name)
            try:
                with (source / asset['file']).open('rb') as original:
                    shutil.copyfileobj(original, stream)
            except BaseException:
                stream.close()
                temporary.unlink(missing_ok=True)
                raise
        try:
            # A hard link publishes the complete file atomically and cannot replace
            # a filename created concurrently by another initializer or uploader.
            os.link(temporary, destination)
        except FileExistsError:
            if hashlib.sha256(destination.read_bytes()).hexdigest() != asset['sha256']:
                raise ValueError(f'Concurrent image destination conflict: {asset["file"]}')
        finally:
            temporary.unlink(missing_ok=True)


def needs_image(path: str | None, directory: Path) -> bool:
    if not path or path == LEGACY_PATH:
        return True
    return bool(GENERATED.fullmatch(path)) and not (directory / path).is_file()


def seed_sql(assets: list[dict]) -> str:
    def literal(value: str) -> str:
        return "'" + value.replace('\\', '\\\\').replace("'", "''") + "'"
    lines = [SQL_START, '-- Fresh database seed only; install files with scripts/initialize-media.py --install-only.',
             '-- Existing databases must use the incremental --apply command, never re-import this SQL.']
    for asset in assets:
        product = asset['kind'] == 'product'
        table, key, column = ('product', 'code', 'image') if product else ('sys_user', 'username', 'avatar')
        keys = asset.get('product_codes' if product else 'usernames', [])
        if not keys:
            continue
        values = ','.join(literal(value) for value in keys)
        info = json.dumps(metadata(asset), ensure_ascii=False, separators=(',', ':'))
        lines.append(f"UPDATE {table} SET {column}_path={literal(asset['file'])}, {column}_meta={literal(info)} "
                     f"WHERE {key} IN ({values});")
    avatars = [asset for asset in assets if asset['kind'] == 'avatar']
    known_users = ','.join(literal(name) for asset in avatars for name in asset.get('usernames', []))
    for index, asset in enumerate(avatars):
        info = json.dumps(metadata(asset), ensure_ascii=False, separators=(',', ':'))
        # Use the same bounded SHA-256 value in MySQL and Python so fresh and
        # incremental initialization choose the same avatar for other accounts.
        lines.append(f"UPDATE sys_user SET avatar_path={literal(asset['file'])},avatar_meta={literal(info)} "
                     f"WHERE username NOT IN ({known_users}) AND MOD(CONV(RIGHT(SHA2(username,256),8),16,10),{len(avatars)})={index};")
    return '\n'.join([*lines, SQL_END])


def sync_baseline(assets: list[dict]) -> None:
    baseline = ROOT / 'doc/数据库/supermarket_inventory.sql'
    original = baseline.read_text(encoding='utf8')
    block = seed_sql(assets)
    if SQL_START in original:
        updated = re.sub(re.escape(SQL_START) + r'.*?' + re.escape(SQL_END), lambda _: block, original, flags=re.S)
    else:
        marker = 'SET FOREIGN_KEY_CHECKS = 1;'
        if original.count(marker) != 1:
            raise ValueError('Cannot locate the baseline media insertion point')
        updated = original.replace(marker, block + '\n\n' + marker)
    baseline.write_text(updated, encoding='utf8', newline='\n')


def initialize(args: argparse.Namespace, assets: list[dict]) -> dict:
    if not re.fullmatch(r'supermarket_inventory(?:_[a-z0-9_]+)?', args.database):
        raise ValueError('Select a supermarket_inventory project database; system databases are not allowed')
    connection = pymysql.connect(host=os.getenv('DB_HOST', '127.0.0.1'),
                                 port=int(os.getenv('DB_PORT', '3306')),
                                 user=os.getenv('DB_USER', 'root'), password=os.environ['DB_PASSWORD'],
                                 database=args.database, charset='utf8mb4', connect_timeout=5,
                                 read_timeout=15, write_timeout=15, autocommit=False,
                                 cursorclass=pymysql.cursors.DictCursor)
    plan, preserved, unmapped = [], 0, 0
    try:
        with connection.cursor() as cursor:
            cursor.execute('SET SESSION innodb_lock_wait_timeout=5')
            for table, key, prefix, kind in [('product', 'code', 'image', 'product'),
                                             ('sys_user', 'username', 'avatar', 'avatar')]:
                choices = [asset for asset in assets if asset['kind'] == kind]
                mapping = {value: asset for asset in choices for value in asset.get('product_codes' if kind == 'product' else 'usernames', [])}
                cursor.execute(f'SELECT id,{key},{prefix}_path,{prefix}_meta FROM {table} ORDER BY id')
                for row in cursor.fetchall():
                    if not needs_image(row[f'{prefix}_path'], args.upload_dir):
                        preserved += 1
                        continue
                    asset = mapping.get(row[key])
                    if asset is None and kind == 'avatar' and choices:
                        # Newly created demo accounts receive a stable choice without
                        # requiring their usernames in the seed file or changing roles.
                        index = int(hashlib.sha256(row[key].encode()).hexdigest()[-8:], 16) % len(choices)
                        asset = choices[index]
                    if asset is None:
                        unmapped += 1
                        continue
                    plan.append({'table': table, 'prefix': prefix, 'id': row['id'], 'key': row[key],
                                 'old_path': row[f'{prefix}_path'], 'old_meta': row[f'{prefix}_meta'],
                                 'path': asset['file'], 'meta': json.dumps(metadata(asset), ensure_ascii=False)})
            backup = None
            if args.apply and plan:
                install_assets(assets, args.upload_dir)
                args.backup_dir.mkdir(parents=True, exist_ok=True)
                stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
                backup = args.backup_dir / f'{args.database}-{stamp}.json'
                backup.write_text(json.dumps({'database': args.database, 'changes': plan}, ensure_ascii=False, indent=2), encoding='utf8')
                changed = 0
                for row in plan:
                    prefix = row['prefix']
                    cursor.execute(f"UPDATE {row['table']} SET {prefix}_path=%s,{prefix}_meta=%s "
                                   f"WHERE id=%s AND {prefix}_path <=> %s AND {prefix}_meta <=> CAST(%s AS JSON)",
                                   (row['path'], row['meta'], row['id'], row['old_path'], row['old_meta']))
                    changed += cursor.rowcount
                connection.commit()
            else:
                changed = 0
                connection.rollback()
            return {'database': args.database, 'mode': 'apply' if args.apply else 'preview',
                    'planned': len(plan), 'updated': changed, 'preserved': preserved, 'unmapped': unmapped,
                    'skipped_concurrent': len(plan) - changed if args.apply else 0,
                    'backup': str(backup) if backup else None,
                    'bindings': [{'table': row['table'], 'key': row['key'], 'path': row['path']} for row in plan]}
    except BaseException:
        connection.rollback()
        raise
    finally:
        connection.close()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--database', default='supermarket_inventory')
    parser.add_argument('--upload-dir', type=Path, default=Path(os.getenv('UPLOAD_DIR', str(ROOT / 'backend/.local/uploads'))))
    parser.add_argument('--backup-dir', type=Path, default=ROOT / '.local/media-initialization')
    action = parser.add_mutually_exclusive_group()
    action.add_argument('--apply', action='store_true')
    action.add_argument('--install-only', action='store_true')
    action.add_argument('--sync-baseline', action='store_true', help='Regenerate image bindings in the sole SQL baseline; no database connection')
    args = parser.parse_args()
    args.upload_dir = args.upload_dir.resolve()
    assets = load_assets()
    if args.sync_baseline:
        sync_baseline(assets)
        result = {'mode': 'sync-baseline', 'assets': len(assets)}
    elif args.install_only:
        install_assets(assets, args.upload_dir)
        result = {'mode': 'install-only', 'assets': len(assets), 'directory': str(args.upload_dir)}
    else:
        if not os.getenv('DB_PASSWORD'):
            parser.error('Set DB_PASSWORD in the environment before accessing MySQL')
        result = initialize(args, assets)
    print(json.dumps(result, ensure_ascii=True, indent=2))


if __name__ == '__main__':
    main()
