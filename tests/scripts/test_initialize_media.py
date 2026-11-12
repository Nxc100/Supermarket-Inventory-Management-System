"""Regression checks for file integrity and non-destructive image initialization."""
import copy
import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('initialize_media', ROOT / 'scripts/initialize-media.py')
media = importlib.util.module_from_spec(spec)
spec.loader.exec_module(media)


class MediaInitializationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.assets = media.load_assets()

    def test_bundled_photos_have_real_dimensions_and_complete_seed_coverage(self):
        self.assertEqual(len(self.assets), 22)
        self.assertTrue(all(min(a['width'], a['height']) >= 800 for a in self.assets))
        codes = {code for a in self.assets for code in a.get('product_codes', [])}
        self.assertTrue({f'P{i:04d}' for i in range(1, 25)} <= codes)
        self.assertEqual(sum(len(a.get('usernames', [])) for a in self.assets), 8)

    def test_install_is_repeatable_and_rejects_conflicting_files(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory)
            asset = self.assets[0]
            media.install_assets([asset], target)
            image = target / asset['file']
            stamp = image.stat().st_mtime_ns
            media.install_assets([asset], target)
            self.assertEqual(image.stat().st_mtime_ns, stamp)
            image.write_bytes(b'existing unrelated content')
            with self.assertRaisesRegex(ValueError, 'conflicts'):
                media.install_assets([asset], target)
            self.assertEqual(image.read_bytes(), b'existing unrelated content')
            self.assertEqual(list(target.glob('*.tmp')), [])

    def test_existing_upload_is_preserved_and_broken_binding_can_be_repaired(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory)
            filename = self.assets[0]['file']
            self.assertTrue(media.needs_image(filename, target))
            media.install_assets([self.assets[0]], target)
            self.assertFalse(media.needs_image(filename, target))
            self.assertFalse(media.needs_image('../private.jpg', target))
            self.assertTrue(media.needs_image(None, target))
            self.assertTrue(media.needs_image('测试图片.jpg', target))

    def test_manifest_rejects_corrupt_image_before_database_access(self):
        import json
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory)
            asset = copy.deepcopy(self.assets[0])
            (target / asset['file']).write_bytes(b'not an image')
            (target / 'manifest.json').write_text(json.dumps({'version': 1, 'assets': [asset]}), encoding='utf8')
            with self.assertRaisesRegex(ValueError, 'checksum'):
                media.load_assets(target)

    def test_sql_baseline_contains_current_manifest_bindings(self):
        baseline = (ROOT / 'doc/数据库/supermarket_inventory.sql').read_text(encoding='utf8')
        self.assertIn(media.seed_sql(self.assets), baseline)
        block = media.seed_sql(self.assets)
        self.assertNotIn('stock_qty=', block)
        self.assertNotIn('password_hash=', block)
        self.assertTrue(all(asset['file'] in block for asset in self.assets))

    def test_baseline_views_are_created_after_their_dependencies(self):
        import re
        baseline = (ROOT / 'doc/数据库/supermarket_inventory.sql').read_text(encoding='utf8')
        views = re.findall(r'CREATE ALGORITHM.*?VIEW `(v_\w+)` AS (.*?);', baseline)
        self.assertEqual(len(views), 8)
        created = set()
        for name, definition in views:
            dependencies = set(re.findall(r'`(v_\w+)`', definition)) - {name}
            self.assertTrue(dependencies <= created, (name, dependencies - created))
            created.add(name)


if __name__ == '__main__':
    unittest.main()
