"""Required CI split countercases; synthetic orchestration edits, no heavy execution."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ci_policy import classify, classify_delta, git, orchestration_errors, qualification_relation
from ci_fast import profile_steps
from local_only import require_local

ROOT = Path(__file__).resolve().parents[3]


class OrchestrationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        lock = json.loads((ROOT / "scripts/harness/remote-fast.lock.json").read_text())
        for path in [*lock["files"], ".github/workflows/checkpoint.yml", "scripts/harness/remote-fast.lock.json"]:
            dst = self.root / path
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / path, dst)
        self.workflow = self.root / ".github/workflows/checkpoint.yml"
        self.assertEqual([], orchestration_errors(self.root))

    def test_qualification_alias(self):
        self.workflow.write_text(self.workflow.read_text().replace("ci-fast", "qualification-local"))
        self.assertTrue(orchestration_errors(self.root))

    def test_direct_heavy(self):
        original = self.workflow.read_text()
        for command in ("python3 scripts/harness/run.py full", "python3 scripts/harness/challenge.py",
                        "python3 scripts/harness/run.py performance", "python3 scripts/harness/run.py challenge",
                        "python3 scripts/harness/w2b_e2e.py", "python3 scripts/renamed-safe-alias.py"):
            with self.subTest(command=command):
                self.workflow.write_text(original + "      - run: " + command + "\n")
                self.assertTrue(orchestration_errors(self.root))

    def test_manual_full(self):
        (self.workflow.parent / "manual.yml").write_text('"on": workflow_dispatch\njobs:\n  full:\n    steps:\n      - run: python3 scripts/harness/run.py full\n')
        self.assertTrue(orchestration_errors(self.root))

    def test_hidden_entrypoint_alias(self):
        p = self.root / "scripts/harness/ci_fast.py"
        p.write_text(p.read_text() + "\n# renamed task\nimport subprocess\nsubprocess.run(['python3','scripts/harness/challenge.py'])\n")
        self.assertTrue(orchestration_errors(self.root))

    def test_reusable_job(self):
        self.workflow.write_text(self.workflow.read_text().replace("    runs-on: ubuntu-latest", "    uses: ./.github/workflows/hidden.yml"))
        self.assertTrue(orchestration_errors(self.root))

    def test_local_only_runtime(self):
        with patch.dict(os.environ, {"GITHUB_ACTIONS": "true"}):
            with self.assertRaisesRegex(RuntimeError, "REMOTE_QUALIFICATION_PROHIBITED"):
                require_local()

    def test_src_never_docs(self):
        self.assertEqual("CODE_OR_HARNESS_CHANGE", classify(["src/main/java/a/X.java"]))
        self.assertEqual("CODE_OR_HARNESS_CHANGE", classify(["core/src/main/java/a/X.java", "MANIFEST.sha256"]))

    def test_harness_never_docs(self):
        for path in ("scripts/harness/run.py", "core/pom.xml", "pom.xml", ".github/workflows/checkpoint.yml",
                     "adapters/src/test/resources/sp/example.json", "docs/sources/sources.lock.json", "docs/templates/work-item.schema.json"):
            self.assertEqual("CODE_OR_HARNESS_CHANGE", classify(["docs/readme.md", path]))

    def test_docs_fast_no_bootstrap(self):
        classification = classify(["docs/readme.md", "docs/work/history/WORK-LOWER-011.md", "MANIFEST.sha256"])
        self.assertEqual("DOCS_ONLY", classification)
        self.assertEqual(("checkout", "orchestration", "docs", "integrity", "git-scope", "harness-focal"), profile_steps(classification))

    def test_merge_main_fast_only(self):
        # Event/branch do not choose product suites: the same fixed plan serves push/PR/merge.
        for classification in ("DOCS_ONLY", "CODE_OR_HARNESS_CHANGE"):
            steps = profile_steps(classification)
            self.assertFalse(set(steps) & {"full", "performance", "challenge", "qualification-local", "producer-e2e"})
        text = self.workflow.read_text()
        self.assertIn("pull_request:", text)
        self.assertNotIn("workflow_dispatch", text)


class DocumentaryQualificationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        git(self.root, "init", "-b", "main")
        git(self.root, "config", "user.name", "CI test")
        git(self.root, "config", "user.email", "ci-test@example.invalid")
        (self.root / "src").mkdir()
        (self.root / "docs").mkdir()
        (self.root / "src/product.java").write_text("product")
        (self.root / "docs/readme.md").write_text("initial")
        self.before = self.commit()

    def commit(self):
        git(self.root, "add", ".")
        git(self.root, "commit", "-m", "test")
        return git(self.root, "rev-parse", "HEAD").decode().strip()

    def test_docs_successor_does_not_inherit_full(self):
        (self.root / "docs/readme.md").write_text("closeout receipt")
        after = self.commit()
        self.assertEqual("DOCS_ONLY", classify_delta(self.root, self.before, after)[0])
        relation = qualification_relation(self.root, self.before, after)
        self.assertEqual("DOCUMENTATION_ONLY_SUCCESSOR", relation["relation"])
        self.assertEqual(self.before, relation["qualified_head"])
        self.assertFalse(relation["full_executed_on_current_head"])

    def test_product_and_manifest_invalidates(self):
        (self.root / "src/product.java").write_text("changed")
        (self.root / "MANIFEST.sha256").write_text("updated")
        after = self.commit()
        with self.assertRaisesRegex(RuntimeError, "INVALIDATED"):
            qualification_relation(self.root, self.before, after)

    def test_same_tree_merge_preserves_qualification_without_claiming_execution(self):
        tree = git(self.root, "rev-parse", "HEAD^{tree}").decode().strip()
        merged = git(self.root, "commit-tree", tree, "-p", self.before, "-m", "merge-equivalent").decode().strip()
        relation = qualification_relation(self.root, self.before, merged)
        self.assertEqual("QUALIFIED_TREE_PRESERVED", relation["relation"])
        self.assertFalse(relation["full_executed_on_current_head"])

    def test_false_classifier_cannot_hide_protected_change(self):
        (self.root / "src/product.java").write_text("changed")
        after = self.commit()
        with patch("ci_policy.classify", return_value="DOCS_ONLY"):
            with self.assertRaisesRegex(RuntimeError, "PROTECTED_BLOB_CHANGED"):
                classify_delta(self.root, self.before, after)


if __name__ == "__main__":
    unittest.main()
