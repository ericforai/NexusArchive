# Input: demo data generator
# Output: unittest results for demo data + attachments + sql markers
# Pos: demo data generator tests
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from generate_demo_data import (
    build_archives,
    build_file_contents,
    build_relations,
    load_spec,
    render_sql_blocks,
    write_outputs,
)


class DemoDataSpecTest(unittest.TestCase):
    def test_load_spec_has_expected_fonds(self):
        spec = load_spec(Path("scripts/demo_data_spec.json"))
        fonds_codes = [f["fonds_code"] for f in spec["fonds"]]
        self.assertEqual(
            fonds_codes,
            ["BR-GROUP", "BR-SALES", "BR-TRADE", "BR-MFG", "COMP001", "BRJT", "DEMO"],
        )


class DemoArchiveTest(unittest.TestCase):
    def test_archive_counts_and_relations(self):
        spec = load_spec(Path("scripts/demo_data_spec.json"))
        archives = build_archives(spec)
        relations = build_relations(spec, archives)
        self.assertEqual(len(archives), 7 * 6)
        archive_ids = {a["id"] for a in archives}
        for rel in relations:
            self.assertIn(rel["source_id"], archive_ids)
            self.assertIn(rel["target_id"], archive_ids)

    def test_sequences_use_high_range(self):
        spec = load_spec(Path("scripts/demo_data_spec.json"))
        archives = build_archives(spec)
        for archive in archives:
            code_seq = int(archive["archive_code"].split("-")[-1])
            biz_seq = int(archive["unique_biz_id"].split("-")[-1])
            self.assertGreaterEqual(code_seq, 900)
            self.assertGreaterEqual(biz_seq, 900)


class DemoAttachmentTest(unittest.TestCase):
    def test_files_and_hashes(self):
        spec = load_spec(Path("scripts/demo_data_spec.json"))
        archives = build_archives(spec)
        files = build_file_contents(spec, archives)
        self.assertTrue(all(f["file_size"] > 0 for f in files))
        for file_info in files:
            file_name = file_info["file_name"]
            if file_name.endswith("_bank.pdf"):
                self.assertEqual(file_info["attachment_type"], "bank_slip")
            if file_name.endswith("_report.pdf"):
                self.assertEqual(file_info["attachment_type"], "other")


class DemoSqlRenderTest(unittest.TestCase):
    def test_sql_contains_markers(self):
        sql = render_sql_blocks(load_spec(Path("scripts/demo_data_spec.json")))
        self.assertIn("-- DEMO DATA START", sql)
        self.assertIn("INSERT INTO acc_archive", sql)

    def test_sql_sets_search_path(self):
        sql = render_sql_blocks(load_spec(Path("scripts/demo_data_spec.json")))
        self.assertIn("SET search_path TO public;", sql)

    def test_write_outputs_injects_seed_block(self):
        spec_path = Path("scripts/demo_data_spec.json")
        with tempfile.TemporaryDirectory() as tmpdir:
            tmp_root = Path(tmpdir)
            seed_path = tmp_root / "seed-data.sql"
            migration_path = tmp_root / "V20260119__seed_demo_full_data.sql"
            docs_dir = tmp_root / "demo-data-docs"
            seed_path.write_text(
                "-- header\n-- PostgreSQL database dump complete\n",
                encoding="utf-8",
            )
            write_outputs(
                spec_path=spec_path,
                seed_path=seed_path,
                migration_path=migration_path,
                docs_dir=docs_dir,
            )
            seed_contents = seed_path.read_text(encoding="utf-8")
            self.assertIn("-- DEMO DATA START", seed_contents)
            self.assertTrue(migration_path.exists())


if __name__ == "__main__":
    unittest.main()
