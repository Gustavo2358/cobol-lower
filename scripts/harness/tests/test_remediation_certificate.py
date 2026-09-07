"""Controlled certificate records, never claims of actual execution or new authorization."""
import copy
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "scripts/harness"))
from checks import certificate_errors, read_data


class RemediationCertificate(unittest.TestCase):
    def setUp(self):
        self.record = read_data(ROOT / "docs/quality/WORK-LOWER-001/R1.json")
        previous = read_data(ROOT / "docs/quality/WORK-LOWER-001/CP5.json")
        # Reuse complete controlled values only to test predicates; no log is rewritten.
        for key in ("executions", "regressions", "falsifications", "review", "certification"):
            self.record[key] = copy.deepcopy(previous[key])
        self.record.update(not_executed=[], candidate_diff_sha256="a" * 64)

    def errors(self):
        return certificate_errors(ROOT, self.record)

    def test_complete_controlled_remediation(self):
        self.assertEqual([], self.errors())

    def test_not_cp6_and_no_gate_shrink(self):
        self.record["checkpoint"] = "CP6"
        self.assertIn("CERTIFICATE remediation is not a checkpoint", self.errors())
        self.record["checkpoint"] = None
        self.record["frozen_contract"]["required_gates"].remove("full")
        self.assertIn("CERTIFICATE remediation frozen gates/id", self.errors())

    def test_recovery_sha_and_path_cannot_come_from_receipt(self):
        self.record["previous_certified_checkpoint"]["commit"] = "b" * 40
        self.record["previous_certified_checkpoint"]["remote_receipt"] = "docs/quality/WORK-LOWER-001/CP4-remote.json"
        self.assertIn("CERTIFICATE remediation recovery commit", self.errors())
        self.assertIn("CERTIFICATE remediation recovery paths", self.errors())

    def test_normative_contract_and_grant_cannot_disappear(self):
        for path, error in (("docs/engineering/falsification.md", "FREEZE normative references incomplete"),
                            ("docs/quality/WORK-LOWER-001/R1-contract.md", "remediation immutable contract"),
                            ("docs/quality/WORK-LOWER-001/R1-authorization.json", "remediation immutable grant")):
            original = self.record["frozen_contract"]["references"]
            self.record["frozen_contract"]["references"] = [r for r in original if r["path"] != path]
            self.assertIn("CERTIFICATE " + error, self.errors())
            self.record["frozen_contract"]["references"] = original

    def test_source_lock_cannot_be_reference_only(self):
        lock = next(r for r in self.record["frozen_contract"]["references"] if r["path"] == "docs/sources/sources.lock.json")
        lock["usage"] = "reference_only"
        self.assertIn("CERTIFICATE FREEZE source lock immutable", self.errors())


if __name__ == "__main__":
    unittest.main(verbosity=2)
