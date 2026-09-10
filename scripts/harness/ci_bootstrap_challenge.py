"""Initial CP0 merge audit: compiled Python assertion RED, exact restore, second GREEN."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = 'scripts/harness/run.py'
CASES = [
    ('old-null-comparison', 'initial_pr = record["checkpoint"] == "CP0" and record["pull_request"] is None',
     'initial_pr = False', 'test_initial_cp0_selects_historical_head', 'initial CP0 selects historical head'),
    ('null-beyond-cp0', 'initial_pr = record["checkpoint"] == "CP0" and record["pull_request"] is None',
     'initial_pr = record["pull_request"] is None', 'test_null_later_checkpoint_rejected', 'null later checkpoint rejected'),
    ('wrong-explicit-pr', '(not initial_pr and record["pull_request"] != pr["number"])',
     'False', 'test_explicit_wrong_pr_rejected', 'explicit wrong PR rejected'),
    ('wrong-branch', 'record["branch"] != pr["head"]["ref"]', 'False',
     'test_wrong_branch_rejected', 'wrong branch rejected'),
    ('altered-evidence', 'json.loads(git(ROOT, "show", sha + ":" + path)) != record', 'False',
     'test_altered_merge_evidence_rejected', 'altered merge evidence rejected'),
    ('missing-ancestry', 'git(ROOT, "merge-base", "--is-ancestor", certified, sha)', 'None # skipped ancestry',
     'test_unrelated_head_rejected', 'unrelated head rejected'),
]


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def main():
    logs = Path(os.environ['LOWER_BUILD_ROOT']) / 'ci-bootstrap-challenge-logs'
    logs.mkdir(parents=True, exist_ok=True)
    records = []
    with tempfile.TemporaryDirectory(prefix='lower-ci-bootstrap-challenge-') as directory:
        root = Path(directory)
        shutil.copytree(ROOT / 'scripts/harness', root / 'scripts/harness', ignore=shutil.ignore_patterns('__pycache__', '*.pyc'))
        path = root / SOURCE
        original = path.read_bytes()
        for ident, before, after, method, oracle in CASES:
            command = [sys.executable, '-B', 'scripts/harness/tests/test_ci_bootstrap.py', 'CiBootstrap.' + method, '-v']
            def execute(stage):
                result = subprocess.run(command, cwd=root, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
                (logs / (ident + '-' + stage + '.log')).write_bytes(result.stdout)
                return result.returncode, result.stdout.decode()
            baseline_code, baseline = execute('baseline')
            if baseline_code or 'Ran 1 test' not in baseline or original.count(before.encode()) != 1:
                raise RuntimeError('invalid CI bootstrap challenge baseline/target ' + ident + '\n' + baseline)
            try:
                path.write_bytes(original.replace(before.encode(), after.encode()))
                red_code, red = execute('red')
            finally:
                path.write_bytes(original)
            green_code, green = execute('restored')
            if (not red_code or 'AssertionError: CI_BOOTSTRAP ' + oracle not in red or 'FAILED (failures=1)' not in red
                    or green_code or 'Ran 1 test' not in green or path.read_bytes() != original
                    or (ROOT / SOURCE).read_bytes() != original):
                raise RuntimeError('invalid/unrestored CI bootstrap falsification ' + ident + '\n' + red + '\n' + green)
            record = dict(id=ident, mutation=before + ' -> ' + after, expected_oracle='CI_BOOTSTRAP ' + oracle,
                          baseline_exit_code=baseline_code, observed_exit_code=red_code,
                          failure_reason='AssertionError: CI_BOOTSTRAP ' + oracle,
                          baseline_digest=digest(original), restored_digest=digest(path.read_bytes()),
                          digest_scope=SOURCE, restoration_status='RESTORED', green_exit_code=green_code,
                          green_command='python3 -B scripts/harness/tests/test_ci_bootstrap.py CiBootstrap.' + method + ' -v (isolated)',
                          baseline_log_sha256=digest(baseline.encode()), red_log_sha256=digest(red.encode()),
                          second_green_log_sha256=digest(green.encode()))
            records.append(record)
            print(json.dumps(record), flush=True)
    (logs / 'receipt.json').write_text(json.dumps(records, indent=2) + '\n')
    print('PASS CI BOOTSTRAP CHALLENGE: 6 restored falsifications and second GREEN')


if __name__ == '__main__':
    main()
