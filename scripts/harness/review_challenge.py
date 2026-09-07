"""Human-review A..E: isolated plausible bypasses, assertion RED, restoration, second GREEN."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile

from checks import digest
sys.path.insert(0, str(Path(__file__).parent / "tests"))
from lifecycle_fixture import copy_checkout

ROOT = Path(__file__).resolve().parents[2]
CASES = [
    ("R1-A-RECEIPT", "scripts/harness/checks.py",
     'remote_errors(certificate["frozen_contract"]["required_remote_checks"], expected, receipt)',
     'remote_errors(certificate["frozen_contract"]["required_remote_checks"], receipt["pushed_sha"], receipt)',
     "ReviewDocuments.test_history_rejects_self_consistent_wrong_receipt"),
    ("R1-B-ORACLE", "scripts/harness/git_checks.py",
     'if usage == "candidate_immutable":', 'if False and usage == "candidate_immutable":',
     "FreezeCandidate.test_immutable_oracle_candidate_and_committed_bytes"),
    ("R1-C-AUTHORITY", "scripts/harness/checks.py", 'if for_execution:', 'if False and for_execution:',
     "ReviewDocuments.test_history_never_authorizes_execution"),
    ("R1-D-WORKFLOW", ".github/workflows/checkpoint.yml", "branches: ['**']", "branches: ['feat/first-entry-goback-slice']",
     "WorkflowPolicy.test_push_policy_covers_new_work_branch_and_main"),
    ("R1-E-SHA", "scripts/harness/checks.py", 'and c.get("head_sha") == sha', 'and True',
     "RemoteLifecycle.test_wrong_sha_success_still_rejected"),
]


def execute(root, selector):
    command = [sys.executable, "-B", "scripts/harness/tests/test_human_review.py", selector, "-v"]
    result = subprocess.run(command, cwd=root, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    return result.returncode, result.stdout


def main():
    with tempfile.TemporaryDirectory(prefix="lower-review-challenge-") as directory:
        root = Path(directory)
        copy_checkout(root)
        for ident, relative, before, after, selector in CASES:
            path = root / relative
            original = path.read_bytes()
            baseline_code, baseline = execute(root, selector)
            if baseline_code != 0 or "Ran 1 test" not in baseline or original.count(before.encode()) != 1:
                raise RuntimeError("invalid challenge baseline/target " + ident + "\n" + baseline)
            try:
                path.write_bytes(original.replace(before.encode(), after.encode()))
                red_code, red = execute(root, selector)
            finally:
                path.write_bytes(original)
            green_code, green = execute(root, selector)
            if (red_code == 0 or "AssertionError" not in red or "FAILED (failures=1)" not in red
                    or green_code != 0 or "Ran 1 test" not in green
                    or path.read_bytes() != original or (ROOT / relative).read_bytes() != original):
                raise RuntimeError("invalid/unrestored falsification " + ident + "\n" + red + "\n" + green)
            print(json.dumps(dict(id=ident, mutation=before + " -> " + after, expected_oracle=selector,
                baseline_exit_code=baseline_code, observed_exit_code=red_code, failure_reason="AssertionError: " + selector,
                baseline_digest=digest(original), restored_digest=digest(path.read_bytes()), digest_scope=relative,
                restoration_status="RESTORED", green_exit_code=green_code,
                green_command="python3 -B scripts/harness/tests/test_human_review.py " + selector + " -v (isolated copy)",
                baseline_output_sha256=digest(baseline.encode()), red_output_sha256=digest(red.encode()),
                restored_output_sha256=digest(green.encode()))), flush=True)
    print("PASS REVIEW CHALLENGE: 5 restored falsifications and second GREEN")


if __name__ == "__main__":
    main()
