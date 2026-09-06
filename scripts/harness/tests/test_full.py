import unittest
from pathlib import Path
import sys
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from full_checks import execute_full, performance_counts
import run as harness


class Full(unittest.TestCase):
    def setUp(self):
        certificate = patch.object(harness, "certificate_errors", return_value=[])
        certificate.start()
        self.addCleanup(certificate.stop)

    def test_all_required_components_once(self):
        observed = []
        names = ["docs", "semantic", "performance", "architecture", "git", "harness-tests", "challenge"]
        runners = {name: lambda n=name: observed.append(n) for name in names}
        execute_full(["docs", "architecture", "semantic", "performance", "full", "git"], runners)
        self.assertEqual(observed, names)

    def test_missing_runner(self):
        with self.assertRaisesRegex(RuntimeError, "FULL.*missing"):
            execute_full(["docs", "architecture", "semantic", "performance", "full", "git"], {})

    def test_cannot_shrink_or_expand_required_set(self):
        for required in (["docs", "full"], ["docs", "architecture", "semantic", "performance", "full", "git", "transport"]):
            with self.assertRaisesRegex(RuntimeError, "FULL.*set"):
                execute_full(required, {})

    def test_failed_leaf_propagates(self):
        observed = []
        names = ["docs", "semantic", "performance", "architecture", "git", "harness-tests", "challenge"]
        def fail():
            raise RuntimeError("independent failing performance oracle")
        runners = {name: lambda n=name: observed.append(n) for name in names}
        runners["performance"] = fail
        with self.assertRaisesRegex(RuntimeError, "independent failing performance"):
            execute_full(["docs", "architecture", "semantic", "performance", "full", "git"], runners)
        self.assertEqual(observed, ["docs", "semantic"])

    def test_real_nonzero_performance_suites(self):
        self.assertEqual(performance_counts("LOWER_PERFORMANCE_TESTS=17\nLOWER_PERFORMANCE_TESTS=23\n"), 40)

    def test_absent_zero_and_incomplete_performance_suites(self):
        for output in ("BUILD SUCCESS", "LOWER_PERFORMANCE_TESTS=0\nLOWER_PERFORMANCE_TESTS=2\n", "LOWER_PERFORMANCE_TESTS=1\n"):
            with self.assertRaisesRegex(RuntimeError, "performance tests absent/zero"):
                performance_counts(output)

    def test_published_git_requires_exact_clean_head(self):
        sha = "a" * 40
        with patch.object(harness, "candidate_errors", return_value=[]) as certificate, patch.object(harness, "git", side_effect=[sha.encode(), b""]), patch.object(harness, "check_pr") as pr:
            harness.git_gate({"checkpoint": "CP4"}, sha)
            certificate.assert_called_once_with(harness.ROOT, {"checkpoint": "CP4"}, sha)
            pr.assert_called_once_with({"checkpoint": "CP4"}, sha)

    def test_published_git_rejects_wrong_head_and_dirty(self):
        sha = "a" * 40
        for answers in ([b"b" * 40], [sha.encode(), b" M tracked"]):
            with patch.object(harness, "candidate_errors", return_value=[]), patch.object(harness, "git", side_effect=answers), patch.object(harness, "check_pr") as pr:
                with self.assertRaisesRegex(RuntimeError, "GIT_PUBLISHED_HEAD_OR_WORKTREE"):
                    harness.git_gate({}, sha)
                pr.assert_not_called()

    def test_published_git_cannot_bypass_certificate(self):
        with patch.object(harness, "candidate_errors", return_value=["CANDIDATE_DIGEST"]), patch.object(harness, "check_pr") as pr:
            with self.assertRaisesRegex(RuntimeError, "CANDIDATE_DIGEST"):
                harness.git_gate({}, "a" * 40)
            pr.assert_not_called()

    def test_published_git_rejects_uncertified_record(self):
        sha = "a" * 40
        with patch.object(harness, "candidate_errors", return_value=[]), patch.object(harness, "certificate_errors", return_value=["CERTIFICATION_NOT_PASS"]), patch.object(harness, "git", side_effect=[sha.encode(), b""]), patch.object(harness, "check_pr") as pr:
            with self.assertRaisesRegex(RuntimeError, "CERTIFICATION_NOT_PASS"):
                harness.git_gate({}, sha)
            pr.assert_not_called()

    def test_local_git_keeps_branch_preflight(self):
        with patch.object(harness, "preflight", return_value=["GIT_BRANCH"]), patch.object(harness, "check_pr") as pr:
            with self.assertRaisesRegex(RuntimeError, "GIT_BRANCH"):
                harness.git_gate({})
            pr.assert_not_called()

    def test_full_wiring_does_not_substitute_a_leaf(self):
        observed = []
        record = {"frozen_contract": {"required_gates": ["docs", "architecture", "semantic", "performance", "full", "git"]}}
        with patch.object(harness, "document_errors", side_effect=lambda root: observed.append("docs") or []), patch.object(harness, "semantic", side_effect=lambda: observed.append("semantic")), patch.object(harness, "performance", side_effect=lambda: observed.append("performance")), patch.object(harness, "architecture", side_effect=lambda: observed.append("architecture")), patch.object(harness, "git_gate", side_effect=lambda r, c: observed.append("git")), patch.object(harness, "run", side_effect=lambda args: observed.append("harness-tests") if args[1:3] == ["-m", "unittest"] else self.fail("wrong harness test command")), patch.object(harness, "challenge", side_effect=lambda: observed.append("challenge")):
            harness.full(record)
        self.assertEqual(observed, ["docs", "semantic", "performance", "architecture", "git", "harness-tests", "challenge"])
