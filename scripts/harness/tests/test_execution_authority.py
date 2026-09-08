"""Historical validity and explicit post-review authority are separate predicates."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "scripts/harness"))
from checks import execution_authority, read_data
from lifecycle_fixture import copy_checkout


class ExecutionAuthority(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory(prefix="lower-authority-")
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        copy_checkout(self.root)
        # Controlled unmerged premise; independent of the live historical merge registry.
        path = self.root / "docs/work/registry.json"
        registry = read_data(path)
        registry["history"][0].update(merge_status="open_not_merged", review_status="changes_requested")
        path.write_text(json.dumps(registry))
        self.record = read_data(self.root / "docs/quality/WORK-LOWER-001/R1.json")

    def test_explicit_v2_unmerged_remediation_positive(self):
        self.assertEqual("R1", execution_authority(self.root, self.record)["id"])

    def test_merged_v2_remediation_is_not_execution_authority(self):
        path = self.root / "docs/work/registry.json"
        registry = read_data(path)
        registry["history"][0].update(merge_status="merged", review_status="reviewed")
        path.write_text(json.dumps(registry))
        with self.assertRaisesRegex(ValueError, "AUTHORIZATION remediation requires unmerged"):
            execution_authority(self.root, self.record)

    def test_history_without_explicit_remediation_does_not_authorize(self):
        path = self.root / "docs/work/registry.json"
        registry = read_data(path)
        registry["history"][0].pop("remediations")
        path.write_text(json.dumps(registry))
        with self.assertRaisesRegex(ValueError, "AUTHORIZATION remediation mismatch"):
            execution_authority(self.root, self.record)


if __name__ == "__main__":
    unittest.main(verbosity=2)
