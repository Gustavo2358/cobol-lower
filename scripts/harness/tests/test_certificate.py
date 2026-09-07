"""Controlled evidence records are test inputs, never claims of executed project gates."""
import copy
import hashlib
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "scripts/harness"))
from checks import certificate_errors


class Certification(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="lower-certificate-oracle-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        shutil.copytree(ROOT / "docs", self.root / "docs")
        gate_path = self.root / "docs/engineering/gates.json"
        gates = json.loads(gate_path.read_text())
        for gate in gates["gates"]:
            gate.update(implementation_status="AUTOMATED_VERIFIED")
        gate_path.write_text(json.dumps(gates))
        (self.root / "synthetic.log").write_text("controlled test input, not real evidence\n")
        record = json.loads((ROOT / "docs/quality/WORK-LOWER-001/CP0.json").read_text())
        record.update(template=False, reviewed_head="1" * 40, candidate_diff_sha256="2" * 64,
                      not_executed=[], blockers=[], certified_commit=None)
        record["executions"] = [dict(gate=g, required=True, command="controlled test fixture", cwd=".",
            environment="synthetic oracle only", status="PASS", exit_code=0, test_count=1,
            log_reference="synthetic.log") for g in record["frozen_contract"]["required_gates"]]
        record["falsifications"] = [dict(id="synthetic", mutation="controlled forbidden input",
            expected_oracle="negative rejected", failure_reason="the target property fails",
            baseline_exit_code=0, observed_exit_code=1, green_exit_code=0, green_command="controlled",
            restoration_status="RESTORED", baseline_digest="3" * 64, restored_digest="3" * 64)]
        record["regressions"] = [dict(previous_checkpoint=None, guarantees=["all CP0 gates"],
                                     execution_references=list(range(4)))]
        record["review"] = dict(status="PASS", kind="self-review", reviewer_context="synthetic-author",
                                candidate_reference="#/candidate_diff_sha256", evidence=["synthetic.log"])
        record["certification"] = dict(status="PASS", scope="pre_commit", unresolved_findings=[],
                                      all_mutations_restored=True, required_results_pass=True)
        self.record = record

    def test_controlled_complete_record(self):
        self.assertEqual([], certificate_errors(self.root, self.record))

    def test_required_result_not_success(self):
        for status in ("FAIL", "NOT_RUN", "UNKNOWN", "BLOCKED"):
            with self.subTest(status=status):
                record = copy.deepcopy(self.record)
                record["executions"][0].update(status=status, exit_code=1)
                self.assertTrue(certificate_errors(self.root, record))

    def test_gate_absent(self):
        self.record["executions"].pop()
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_gate_set_cannot_shrink(self):
        self.record["frozen_contract"]["required_gates"].pop()
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_specified_gate_is_not_pass(self):
        path = self.root / "docs/engineering/gates.json"
        data = json.loads(path.read_text())
        data["gates"][0]["implementation_status"] = "SPECIFIED_NOT_IMPLEMENTED"
        path.write_text(json.dumps(data))
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_regression_absent(self):
        self.record["regressions"] = []
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_regression_missing_semantic(self):
        self.record["regressions"][0]["execution_references"] = [0, 1, 3]
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_zero_tests(self):
        self.record["executions"][2]["test_count"] = 0
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_unrestored(self):
        self.record["falsifications"][0]["restoration_status"] = "UNRESTORED"
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_restoration_digest(self):
        self.record["falsifications"][0]["restored_digest"] = "4" * 64
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_second_green_missing(self):
        self.record["falsifications"][0].pop("green_exit_code")
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_baseline_not_green(self):
        self.record["falsifications"][0]["baseline_exit_code"] = 1
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_freeze_incomplete(self):
        self.record["frozen_contract"]["references"][0].pop("sha256")
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_freeze_normative_reference_removed(self):
        self.record["frozen_contract"]["references"] = [r for r in self.record["frozen_contract"]["references"]
                                                       if r["path"] != "docs/evals/catalog.md"]
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_required_null_field_missing(self):
        self.record.pop("certified_commit")
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_duplicate_review_identity(self):
        self.record["review"]["candidate_diff_sha256"] = "2" * 64
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_wrong_review_reference(self):
        self.record["review"]["candidate_reference"] = "HEAD"
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_false_independent_review(self):
        self.record["review"]["kind"] = "independent"
        self.assertTrue(certificate_errors(self.root, self.record))
        self.record["review"]["implementer_context"] = self.record["review"]["reviewer_context"]
        self.assertTrue(certificate_errors(self.root, self.record))

    def test_missing_log(self):
        self.record["executions"][0]["log_reference"] = "absent.log"
        self.assertTrue(certificate_errors(self.root, self.record))


if __name__ == "__main__":
    unittest.main(verbosity=2)
