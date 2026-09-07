"""Independent human-review counterexamples; no receipt chooses its expected commit."""
import copy
import inspect
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "scripts/harness"))
import checks
import git_checks
import run as harness
from lifecycle_fixture import activate_fixture, copy_checkout

CP5 = "5a00a152d3c05793dad825c16226867c493ee31b"
CP4 = "a487c52faded88d773c22333481f3a54a3327d47"
IDENT = "WORK-LOWER-001"


class ReviewDocuments(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory(prefix="lower-review-doc-")
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        copy_checkout(self.root)
        if not (self.root / ".git").exists():
            shutil.copytree(ROOT / ".git", self.root / ".git")

    def edit(self, path, change):
        target = self.root / path
        data = checks.read_data(target)
        change(data)
        target.write_text(json.dumps(data))

    def test_receipt_matching_certified_commit_positive(self):
        self.assertEqual([], checks.document_errors(self.root))

    def wrong_receipt(self):
        def change(r):
            r["pushed_sha"] = "a" * 40
            for check in r["checks"]:
                check["head_sha"] = "a" * 40
        self.edit("docs/quality/WORK-LOWER-001/CP4-remote.json", change)

    def test_history_rejects_self_consistent_wrong_receipt(self):
        self.wrong_receipt()
        errors = checks.document_errors(self.root)
        self.assertTrue(any("DEPENDENCY" in e for e in errors), errors)

    def test_active_rejects_self_consistent_wrong_receipt(self):
        activate_fixture(self.root)
        self.wrong_receipt()
        errors = checks.document_errors(self.root)
        self.assertTrue(any("DEPENDENCY" in e for e in errors), errors)

    def test_dependency_wrong_checkpoint_identity(self):
        (self.root / "docs/quality/WORK-LOWER-001/CP4.json").write_bytes(
            (self.root / "docs/quality/WORK-LOWER-001/CP3.json").read_bytes())
        self.assertTrue(any("DEPENDENCY" in e for e in checks.document_errors(self.root)))

    def test_resolver_positive_and_wrong_identity(self):
        branch = "feat/first-entry-goback-slice"
        self.assertEqual(CP4, git_checks.resolve_checkpoint_commit(self.root, IDENT, "CP4", branch, 2))
        for bad_branch, bad_pr in (("feat/unrelated", 2), (branch, 99)):
            with self.assertRaisesRegex(ValueError, "DEPENDENCY evidence identity"):
                git_checks.resolve_checkpoint_commit(self.root, IDENT, "CP4", bad_branch, bad_pr)

    def test_receipt_wrong_pr_rejected(self):
        self.edit("docs/quality/WORK-LOWER-001/CP4-remote.json", lambda r: r.update(pull_request=99))
        self.assertTrue(any("DEPENDENCY" in e for e in checks.document_errors(self.root)))

    def test_legacy_schema_is_auditable_but_not_new_execution_authority(self):
        activate_fixture(self.root)
        record = checks.read_data(self.root / "docs/quality/WORK-LOWER-001/CP5.json")
        self.assertEqual([], checks.certificate_errors(self.root, record))
        with self.assertRaisesRegex(ValueError, "AUTHORIZATION.*v2"):
            checks.execution_authority(self.root, record)

    def test_no_git_or_invalid_candidate_is_not_success(self):
        with patch.object(git_checks, "git", side_effect=subprocess.CalledProcessError(1, "git")):
            self.assertTrue(checks.dependency_errors(self.root, IDENT, "CP4", "feat/first-entry-goback-slice", 2))
        with patch.object(git_checks, "candidate_errors", return_value=["CANDIDATE_DIGEST"]):
            self.assertTrue(checks.dependency_errors(self.root, IDENT, "CP4", "feat/first-entry-goback-slice", 2))

    def historical(self, review, merge):
        def change(registry):
            item = registry["history"][0]
            item.update(review_status=review, merge_status=merge,
                remote_observation=dict(repository="Gustavo2358/cobol-lower", pull_request=2,
                    head_sha=CP5, branch="feat/first-entry-goback-slice", base="main",
                    state="MERGED" if merge == "merged" else "OPEN", review_status=review,
                    review_commit=CP5, review_url="https://github.com/Gustavo2358/cobol-lower/pull/2#pullrequestreview-7",
                    merge_commit="b" * 40 if merge == "merged" else None,
                    observed_at="2026-09-07T01:00:00Z"))
        self.edit("docs/work/registry.json", change)

    def test_reviewed_open_history_valid(self):
        self.historical("reviewed", "open_not_merged")
        self.assertEqual([], checks.document_errors(self.root))

    def test_reviewed_merged_history_valid_read_only(self):
        self.historical("reviewed", "merged")
        self.assertEqual([], checks.document_errors(self.root))
        self.assertFalse((self.root / "docs/work/active/WORK-LOWER-001").exists())

    def test_history_never_authorizes_execution(self):
        # Compatibility shim exercises the old behavior, not a missing-argument TypeError.
        options = {"for_execution": True} if "for_execution" in inspect.signature(checks.work_manifest).parameters else {}
        with self.assertRaisesRegex(ValueError, "AUTHORIZATION"):
            checks.work_manifest(self.root, IDENT, **options)

    def test_merged_history_cannot_authorize_even_remediation(self):
        self.historical("reviewed", "merged")
        record = checks.read_data(self.root / "docs/quality/WORK-LOWER-001/CP5.json")
        with self.assertRaisesRegex(ValueError, "AUTHORIZATION"):
            checks.execution_authority(self.root, record)
        record.update(kind="human-review-remediation", remediation="R1",
                      authorization_reference="docs/quality/WORK-LOWER-001/R1-authorization.json")
        with self.assertRaisesRegex(ValueError, "AUTHORIZATION"):
            checks.execution_authority(self.root, record)


class FreezeCandidate(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory(prefix="lower-review-freeze-")
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.git("init", "-q", "-b", "fixture")
        (self.root / "oracle.txt").write_text("Return\n")
        (self.root / "reference.md").write_text("old reference\n")
        self.git("add", ".")
        self.git("-c", "user.name=Oracle", "-c", "user.email=oracle@example.invalid", "commit", "-qm", "base")
        self.base = self.git("rev-parse", "HEAD").strip()

    def git(self, *args):
        return subprocess.check_output(["git", *args], cwd=self.root, text=True, stderr=subprocess.STDOUT)

    def record(self, path, usage):
        return {"schema_version": 2, "frozen_contract": {"references": [dict(path=path,
            sha256=checks.digest((self.root / path).read_bytes()), git_revision=self.base, usage=usage)]}}

    def test_immutable_oracle_candidate_and_committed_bytes(self):
        record = self.record("oracle.txt", "candidate_immutable")
        self.assertEqual([], git_checks.frozen_errors(self.root, record))
        (self.root / "oracle.txt").write_text("Halt\n")
        self.assertTrue(any("FREEZE_CANDIDATE" in e for e in git_checks.frozen_errors(self.root, record)))
        self.git("add", ".")
        self.git("-c", "user.name=Oracle", "-c", "user.email=oracle@example.invalid", "commit", "-qm", "bad oracle")
        mutant = self.git("rev-parse", "HEAD").strip()
        self.assertTrue(any("FREEZE_CANDIDATE" in e for e in git_checks.frozen_errors(self.root, record, mutant)))
        (self.root / "oracle.txt").write_text("Return\n")
        self.assertEqual([], git_checks.frozen_errors(self.root, record))

    def test_reference_only_may_change(self):
        record = self.record("reference.md", "reference_only")
        (self.root / "reference.md").write_text("allowed evolution\n")
        self.assertEqual([], git_checks.frozen_errors(self.root, record))

    def test_usage_missing_or_unknown_rejected_v2(self):
        for usage in (None, "convenient-default"):
            record = self.record("oracle.txt", usage)
            self.assertTrue(any("FREEZE_USAGE" in e for e in git_checks.frozen_errors(self.root, record)))


class RemoteLifecycle(unittest.TestCase):
    def setUp(self):
        self.record = dict(repository="Gustavo2358/cobol-lower", work_item=IDENT,
                           checkpoint="CP5", branch="feat/first-entry-goback-slice", pull_request=2)
        self.pr = dict(number=2, url="https://github.com/Gustavo2358/cobol-lower/pull/2", state="OPEN",
            headRefOid=CP5, headRefName=self.record["branch"], baseRefName="main", isDraft=False,
            autoMergeRequest=None, reviewDecision="APPROVED", mergeCommit=None)

    def check(self, mode):
        options = {"mode": mode} if "mode" in inspect.signature(harness.check_pr).parameters else {}
        with patch.object(harness, "gh", return_value=[self.pr]):
            return harness.check_pr(self.record, CP5, **options)

    def test_execution_open_positive(self):
        self.assertEqual(2, self.check("execution")["number"])

    def test_historical_merged_positive(self):
        self.pr.update(state="MERGED", mergeCommit={"oid": "b" * 40})
        self.assertEqual(2, self.check("historical")["number"])

    def test_execution_merged_rejected(self):
        self.pr.update(state="MERGED", mergeCommit={"oid": "b" * 40})
        with self.assertRaises(RuntimeError):
            self.check("execution")

    def test_wrong_sha_success_still_rejected(self):
        required = [dict(name="checkpoint", workflow=".github/workflows/checkpoint.yml", app_slug="github-actions", event="push")]
        receipt = dict(pushed_sha=CP5, checks=[dict(required[0], head_sha=CP4, status="completed", conclusion="success", id=1, run_id=2, url="https://example.invalid/2")])
        self.assertTrue(checks.remote_errors(required, CP5, receipt))

    def test_reconciliation_reviewed_open_and_merged(self):
        for merge in (False, True):
            pr = dict(self.pr, state="MERGED" if merge else "OPEN", mergeCommit={"oid": "b" * 40} if merge else None)
            registration = dict(review_status="reviewed", merge_status="merged" if merge else "open_not_merged",
                                remote_observation=dict(head_sha=CP5, merge_commit="b" * 40 if merge else None))
            reviews = [dict(commit_id=CP5, state="COMMENTED")]
            self.assertEqual([], harness.reconcile_errors(registration, pr, reviews))

    def test_reconciliation_pending_open_and_stale_open_after_merge(self):
        registration = dict(review_status="pending_human", merge_status="open_not_merged")
        self.assertEqual([], harness.reconcile_errors(registration, self.pr, []))
        self.pr.update(state="MERGED", mergeCommit={"oid": "b" * 40})
        self.assertIn("PR_RECONCILIATION merge status", harness.reconcile_errors(registration, self.pr, []))

    def test_review_of_other_sha_not_current_approval(self):
        registration = dict(review_status="approved", merge_status="open_not_merged", remote_observation=dict(head_sha=CP5))
        self.assertTrue(harness.reconcile_errors(registration, self.pr, [dict(commit_id=CP4, state="APPROVED")]))

    def test_historical_gate_is_read_only_and_requires_certificate(self):
        with patch.object(harness, "certificate_errors", return_value=[]), patch.object(harness, "candidate_errors", return_value=[]), \
                patch.object(harness, "check_pr") as remote, patch.object(harness, "execution_authority", side_effect=AssertionError("must not authorize")):
            harness.git_gate(self.record, CP5, mode="historical")
            remote.assert_called_once_with(self.record, CP5, mode="historical")
        with patch.object(harness, "certificate_errors", return_value=["CERTIFICATE invalid"]), patch.object(harness, "candidate_errors", return_value=[]):
            with self.assertRaisesRegex(RuntimeError, "CERTIFICATE"):
                harness.git_gate(self.record, CP5, mode="historical")


class WorkflowPolicy(unittest.TestCase):
    def test_push_policy_covers_new_work_branch_and_main(self):
        workflow = checks.read_data(ROOT / ".github/workflows/checkpoint.yml")
        trigger = workflow.get("on", workflow.get(True))
        self.assertEqual(["**"], trigger["push"]["branches"], "CI must cover a new work branch and main, not WORK-LOWER-001 only")

    def test_required_check_identity_remains_explicit(self):
        workflow = checks.read_data(ROOT / ".github/workflows/checkpoint.yml")
        self.assertIn("checkpoint", workflow["jobs"])
        commands = [step.get("run", "") for step in workflow["jobs"]["checkpoint"]["steps"]]
        self.assertTrue(any('run.py ci --commit "$GITHUB_SHA"' in command for command in commands))

    def target(self, branch, pulls=None, bad_record=False):
        sha = "b" * 40 if branch == "main" else CP5
        record = dict(repository="example/lower", work_item="WORK-LOWER-999", checkpoint="CP0",
                      branch="feat/another-authorized-work", pull_request=9)
        if bad_record:
            record["branch"] = "feat/wrong"
        event = dict(after=sha, ref="refs/heads/" + branch, repository=dict(full_name="example/lower"))
        with patch.object(harness, "gh", return_value=pulls), patch.object(harness, "evidence_path", return_value="docs/quality/WORK-LOWER-999/CP0.json"), \
                patch.object(harness, "git", return_value=json.dumps(record).encode()):
            return harness.ci_target(sha, event)

    def merged_pr(self):
        return dict(number=9, merged_at="2026-09-07T01:00:00Z", merge_commit_sha="b" * 40,
                    base=dict(ref="main", repo=dict(full_name="example/lower")), head=dict(ref="feat/another-authorized-work", sha=CP5))

    def test_new_work_branch_uses_own_evidence_without_first_branch_name(self):
        record, sha, mode = self.target("feat/another-authorized-work")
        self.assertEqual(("WORK-LOWER-999", CP5, "execution"), (record["work_item"], sha, mode))
        with self.assertRaisesRegex(RuntimeError, "branch/evidence"):
            self.target("feat/another-authorized-work", bad_record=True)

    def test_main_uses_proven_pr_head_as_read_only(self):
        _, sha, mode = self.target("main", [self.merged_pr()])
        self.assertEqual((CP5, "historical"), (sha, mode))
        with self.assertRaisesRegex(RuntimeError, "merge/evidence"):
            self.target("main", [self.merged_pr()], bad_record=True)

    def test_main_missing_ambiguous_or_wrong_merge_sha_rejected(self):
        wrong = dict(self.merged_pr(), merge_commit_sha="c" * 40)
        for pulls in ([], [self.merged_pr(), self.merged_pr()], [wrong]):
            with self.assertRaisesRegex(RuntimeError, "unique proven merged PR"):
                self.target("main", pulls)

    def test_ci_wrong_event_sha_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "push SHA/ref mismatch"):
            harness.ci_target(CP5, dict(after=CP4, ref="refs/heads/main"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
