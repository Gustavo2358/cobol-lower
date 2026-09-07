import copy
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "scripts/harness"))
from git_checks import candidate_digest, candidate_errors, DIFF_OPTIONS, exclusions, evidence_path


class GitIdentity(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="lower-git-oracle-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git("init", "-q", "-b", "work")
        (self.root / "contract.md").write_text("frozen original contract\n")
        (self.root / "code.txt").write_text("baseline\n")
        self.git("add", ".")
        self.git("-c", "user.name=Controlled Oracle", "-c", "user.email=oracle@example.invalid", "commit", "-qm", "synthetic base")
        self.base = self.git("rev-parse", "HEAD").strip()
        (self.root / "code.txt").write_text("candidate\n")
        self.git("add", ".")
        self.record = dict(work_item="WORK-LOWER-001", checkpoint="CP0", base_commit=self.base,
            reviewed_head=self.base, candidate_diff_sha256=None, diff_options=DIFF_OPTIONS,
            frozen_contract=dict(references=[dict(path="contract.md", git_revision=self.base,
                sha256=hashlib.sha256(b"frozen original contract\n").hexdigest())]))
        self.record["candidate_exclusions"] = exclusions(self.record)
        self.record["candidate_diff_sha256"] = candidate_digest(self.root, self.record)
        evidence = self.root / exclusions(self.record)[0]
        evidence.parent.mkdir(parents=True)
        evidence.write_text(json.dumps(self.record))
        self.git("add", ".")

    def git(self, *args):
        return subprocess.check_output(["git", *args], cwd=self.root, stderr=subprocess.STDOUT, text=True)

    def test_complete_candidate(self):
        self.assertEqual([], candidate_errors(self.root, self.record))

    def test_wrong_digest(self):
        self.record["candidate_diff_sha256"] = "0" * 64
        self.assertIn("CANDIDATE_DIGEST", candidate_errors(self.root, self.record))

    def test_frozen_bytes(self):
        self.record["frozen_contract"]["references"][0]["sha256"] = "0" * 64
        self.assertTrue(any(e.startswith("FREEZE_HASH") for e in candidate_errors(self.root, self.record)))

    def test_missing_revision(self):
        self.record["frozen_contract"]["references"][0]["git_revision"] = "f" * 40
        self.assertTrue(any(e.startswith("FREEZE_UNAVAILABLE") for e in candidate_errors(self.root, self.record)))

    def test_unstaged(self):
        (self.root / "code.txt").write_text("unstaged change\n")
        self.assertIn("INDEX_WORKTREE_MISMATCH", candidate_errors(self.root, self.record))

    def test_untracked(self):
        (self.root / "unexpected.txt").write_text("not included\n")
        self.assertIn("INDEX_WORKTREE_MISMATCH", candidate_errors(self.root, self.record))

    def test_extra_exclusion(self):
        self.record["candidate_exclusions"].append("code.txt")
        self.assertIn("CANDIDATE_EXCLUSIONS", candidate_errors(self.root, self.record))

    def test_published_commit(self):
        self.git("-c", "user.name=Controlled Oracle", "-c", "user.email=oracle@example.invalid", "commit", "-qm",
                 "synthetic checkpoint\n\nCheckpoint-Evidence: " + exclusions(self.record)[0])
        commit = self.git("rev-parse", "HEAD").strip()
        self.assertEqual([], candidate_errors(self.root, self.record, commit))

    def test_missing_trailer(self):
        self.git("-c", "user.name=Controlled Oracle", "-c", "user.email=oracle@example.invalid", "commit", "-qm", "uncertified")
        commit = self.git("rev-parse", "HEAD").strip()
        self.assertIn("CHECKPOINT_TRAILER", candidate_errors(self.root, self.record, commit))

    def test_select_current_checkpoint_trailer(self):
        path = "docs/quality/WORK-LOWER-001/CP1.json"
        self.git("-c", "user.name=Controlled Oracle", "-c", "user.email=oracle@example.invalid", "commit", "-qm",
                 "synthetic checkpoint\n\nCheckpoint-Evidence: " + path)
        self.assertEqual(path, evidence_path(self.root, "HEAD"))

    def test_select_rejects_missing_trailer(self):
        with self.assertRaises(ValueError):
            evidence_path(self.root, self.base)

    def test_select_rejects_unsafe_and_duplicate_trailers(self):
        for trailer in ["../outside.json", "docs/quality/WORK-LOWER-001/CP1.json\nCheckpoint-Evidence: docs/quality/WORK-LOWER-001/CP0.json"]:
            self.git("-c", "user.name=Controlled Oracle", "-c", "user.email=oracle@example.invalid", "commit", "--allow-empty", "-qm",
                     "synthetic bad trailer\n\nCheckpoint-Evidence: " + trailer)
            with self.assertRaises(ValueError):
                evidence_path(self.root, "HEAD")

    def test_commit_missing_evidence(self):
        (self.root / exclusions(self.record)[0]).unlink()
        self.git("add", "-u")
        self.git("-c", "user.name=Controlled Oracle", "-c", "user.email=oracle@example.invalid", "commit", "-qm",
                 "synthetic checkpoint\n\nCheckpoint-Evidence: " + exclusions(self.record)[0])
        commit = self.git("rev-parse", "HEAD").strip()
        self.assertIn("COMMITTED_EVIDENCE", candidate_errors(self.root, self.record, commit))

    def test_evidence_not_the_committed_record(self):
        self.git("-c", "user.name=Controlled Oracle", "-c", "user.email=oracle@example.invalid", "commit", "-qm",
                 "synthetic checkpoint\n\nCheckpoint-Evidence: " + exclusions(self.record)[0])
        commit = self.git("rev-parse", "HEAD").strip()
        self.record["invented_certification"] = "PASS"
        self.assertIn("COMMITTED_EVIDENCE", candidate_errors(self.root, self.record, commit))


if __name__ == "__main__":
    unittest.main(verbosity=2)
