# Input: demo data generator
# Output: unittest results
# Pos: demo data generator tests
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from generate_demo_data import load_spec


class DemoDataSpecTest(unittest.TestCase):
    def test_load_spec_has_expected_fonds(self):
        spec = load_spec(Path("scripts/demo_data_spec.json"))
        fonds_codes = [f["fonds_code"] for f in spec["fonds"]]
        self.assertEqual(
            fonds_codes,
            ["BR-GROUP", "BR-SALES", "BR-TRADE", "BR-MFG", "COMP001", "BRJT", "DEMO"],
        )


if __name__ == "__main__":
    unittest.main()
