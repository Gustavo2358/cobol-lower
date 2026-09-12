"""CP0 oracles from CP0-contract.md; fixture mutations never edit the live candidate."""
import copy
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

import yaml

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "scripts/harness"))
from checks import document_errors, remote_errors
from lifecycle_fixture import activate_fixture, attach_git


class Documents(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="lower-doc-oracle-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        shutil.copytree(ROOT / "docs", self.root / "docs")
        for name in ("AGENTS.md", "README.md", "ARCHITECTURE.md", "MANIFEST.sha256"):
            shutil.copy2(ROOT / name, self.root / name)
        # Only sources needed by documentary references; generated build outputs are excluded.
        for name in ("scripts", "core", "adapters", ".github"):
            if (ROOT / name).exists():
                shutil.copytree(ROOT / name, self.root / name,
                                ignore=shutil.ignore_patterns("target", "__pycache__"))
        for name in ("pom.xml", ".gitignore"):
            if (ROOT / name).exists():
                shutil.copy2(ROOT / name, self.root / name)
        activate_fixture(self.root)
        attach_git(self.root)

    def manifest(self, change):
        path = self.root / "docs/work/active/WORK-LOWER-001/work-item.yaml"
        data = yaml.safe_load(path.read_text())
        change(data)
        path.write_text(yaml.safe_dump(data, allow_unicode=True, sort_keys=False))

    def rejects(self, code):
        errors = document_errors(self.root)
        self.assertTrue(any(code in e for e in errors), (code, errors))

    def test_baseline(self):
        self.assertEqual([], document_errors(self.root))

    def test_broken_link(self):
        with (self.root / "README.md").open("a") as out:
            out.write("\n[negative](docs/does-not-exist.md)\n")
        self.rejects("LINK")

    def test_broken_anchor(self):
        with (self.root / "README.md").open("a") as out:
            out.write("\n[negative](AGENTS.md#does-not-exist)\n")
        self.rejects("ANCHOR")

    def test_invariant_missing(self):
        path = self.root / "docs/evals/catalog.json"
        data = json.loads(path.read_text())
        data["evals"][0]["invariants"].append("INV-LWR-999")
        path.write_text(json.dumps(data))
        self.rejects("INVARIANT")

    def test_active_completed(self):
        self.manifest(lambda d: d.update(status="completed"))
        self.rejects("LIFECYCLE")

    def test_required_read_missing(self):
        self.manifest(lambda d: d["must_read"].append("docs/missing.md"))
        self.rejects("MUST_READ")

    def test_default_mode_multiple(self):
        self.manifest(lambda d: d["authorization"].pop("execution_mode"))
        self.rejects("SCHEMA")

    def test_missing_authority(self):
        self.manifest(lambda d: d["authorization"].pop("authority"))
        self.rejects("SCHEMA")

    def test_empty_authorization(self):
        self.manifest(lambda d: d["authorization"].update(authorized_checkpoints=[]))
        self.rejects("SCHEMA")

    def test_nonexistent_checkpoint(self):
        self.manifest(lambda d: d["authorization"].update(authorized_checkpoints=["CP999"], current_checkpoint="CP999"))
        self.rejects("AUTHORIZATION")

    def test_current_not_authorized(self):
        self.manifest(lambda d: d["authorization"].update(current_checkpoint="CP0", authorized_checkpoints=["CP1"]))
        self.rejects("AUTHORIZATION")

    def test_advance_without_dependency(self):
        self.manifest(lambda d: d["authorization"].update(current_checkpoint="CP1"))
        # The negative input must actually lack its dependency even after live CP0 succeeds.
        (self.root / "docs/quality/WORK-LOWER-001/CP0-remote.json").unlink(missing_ok=True)
        self.rejects("DEPENDENCY")

    def test_scope_without_planned(self):
        self.manifest(lambda d: d["source_scope"].append("missing-module/src"))
        self.rejects("SCOPE")

    def test_external_scope(self):
        self.manifest(lambda d: d["source_scope"].append("/tmp/external"))
        self.rejects("SCOPE")

    def test_duplicate_work(self):
        shutil.copytree(self.root / "docs/work/active/WORK-LOWER-001",
                        self.root / "docs/work/proposals/WORK-LOWER-001")
        self.rejects("DUPLICATE")

    def test_source_hash(self):
        path = self.root / "docs/sources/sources.lock.json"
        data = json.loads(path.read_text())
        data["local_sources"][0]["sha256"] = "0" * 64
        path.write_text(json.dumps(data))
        self.rejects("SOURCE_HASH")

    def test_source_revision(self):
        path = self.root / "docs/sources/sources.lock.json"
        data = json.loads(path.read_text())
        data["sources"][0]["commit"] = "main"
        path.write_text(json.dumps(data))
        self.rejects("SOURCE_REVISION")


class Remote(unittest.TestCase):
    required = [{"name": "checkpoint", "workflow": ".github/workflows/checkpoint.yml",
                 "app_slug": "github-actions", "event": "push"}]
    sha = "1" * 40

    def receipt(self):
        return {"pushed_sha": self.sha, "checks": [dict(self.required[0], head_sha=self.sha,
                status="completed", conclusion="success", id=7, run_id=8,
                url="https://github.com/example/repo/actions/runs/8") ]}

    def test_correct_sha_success(self):
        self.assertEqual([], remote_errors(self.required, self.sha, self.receipt()))

    def test_no_push(self):
        value = self.receipt()
        value["pushed_sha"] = None
        self.assertTrue(remote_errors(self.required, self.sha, value))

    def test_missing(self):
        value = self.receipt()
        value["checks"] = []
        self.assertTrue(remote_errors(self.required, self.sha, value))

    def test_old_sha(self):
        value = self.receipt()
        value["checks"][0]["head_sha"] = "2" * 40
        self.assertTrue(remote_errors(self.required, self.sha, value))

    def test_non_success_states(self):
        for status, conclusion in [("in_progress", None), ("queued", None), ("completed", "failure"),
                ("completed", "cancelled"), ("completed", "skipped"), ("completed", "neutral"),
                ("completed", "timed_out"), ("completed", "unknown")]:
            with self.subTest(status=status, conclusion=conclusion):
                value = self.receipt()
                value["checks"][0].update(status=status, conclusion=conclusion)
                self.assertTrue(remote_errors(self.required, self.sha, value))

    def test_wrong_producer(self):
        value = self.receipt()
        value["checks"][0]["app_slug"] = "other"
        self.assertTrue(remote_errors(self.required, self.sha, value))


if __name__ == "__main__":
    unittest.main(verbosity=2)
