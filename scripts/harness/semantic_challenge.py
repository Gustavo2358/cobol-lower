"""Restored semantic/cost falsifications in isolated copies; never mutate the checkout being certified."""
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
APPLICATION = "core/src/main/java/io/github/gustavo2358/lower/application/"
CASES = [
    ("RETURN-HALT", APPLICATION + "EntryGobackLowerer.java",
     "new Operations.Return(header, List.of())", "new Operations.Halt(header, Operations.HaltKind.NORMAL)",
     "typed GOBACK must be Return, never Halt"),
    ("OPEN-COMPLETE", APPLICATION + "EntryGobackLowerer.java",
     "Evidence.InventoryStatus.PARTIAL", "Evidence.InventoryStatus.COMPLETE",
     "alternative entry inventory stays PARTIAL"),
    ("INDEX-SCAN", APPLICATION + "EntryGobackAdmission.java",
     "StatementFact lookup(StatementId id) { references++; return statements.get(id); }",
     "StatementFact lookup(StatementId id) { for (var candidate : statements.entrySet()) { references++; if (candidate.getKey().equals(id)) return candidate.getValue(); } return null; }",
     "reference ledger N+1, no repeated scan"),
]


def sha(value):
    return hashlib.sha256(value).hexdigest()


def execute(root):
    result = subprocess.run([sys.executable, "scripts/harness/run.py", "performance"], cwd=root,
                            text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    return result.returncode, result.stdout


def green(root):
    code, output = execute(root)
    if code != 0 or "PASS performance" not in output:
        raise RuntimeError("challenge baseline/restored gate not GREEN\n" + output)
    return code, output


def main():
    from local_only import require_local
    require_local()
    with tempfile.TemporaryDirectory(prefix="lower-semantic-challenge-") as name:
        root = Path(name)
        for item in ("docs", "scripts", "core", "adapters", ".github"):
            shutil.copytree(ROOT / item, root / item, ignore=shutil.ignore_patterns("target", "__pycache__"))
        for item in ("AGENTS.md", "README.md", "ARCHITECTURE.md", "pom.xml", ".gitignore"):
            shutil.copy2(ROOT / item, root / item)
        for ident, relative, before_token, after_token, oracle in CASES:
            path = root / relative
            original = path.read_bytes()
            baseline_code, baseline = green(root)
            replacement = original.replace(before_token.encode(), after_token.encode())
            if replacement == original:
                raise RuntimeError("mutation target absent: " + ident)
            try:
                path.write_bytes(replacement)
                red_code, red = execute(root)
            finally:
                path.write_bytes(original)
            restored = path.read_bytes()
            green_code, second = green(root)
            causes = [line.strip() for line in red.splitlines() if "AssertionError" in line and oracle in line]
            if red_code == 0 or not causes or restored != original or (ROOT / relative).read_bytes() != original:
                raise RuntimeError("invalid/unrestored falsification: " + ident + "\n" + red)
            record = dict(id=ident, mutation=before_token + " -> " + after_token, expected_oracle=oracle,
                          baseline_exit_code=baseline_code, observed_exit_code=red_code, failure_reason=causes[0],
                          baseline_digest=sha(original), restored_digest=sha(restored), digest_scope=relative,
                          restoration_status="RESTORED", green_command="python3 scripts/harness/run.py performance (isolated copy)",
                          green_exit_code=green_code, baseline_output_sha256=sha(baseline.encode()),
                          red_output_sha256=sha(red.encode()), restored_output_sha256=sha(second.encode()),
                          baseline_counts=re.findall(r"^(?:SEMANTIC|PERFORMANCE)_TEST_COUNT=\d+$", baseline, re.M),
                          restored_counts=re.findall(r"^(?:SEMANTIC|PERFORMANCE)_TEST_COUNT=\d+$", second, re.M))
            print(json.dumps(record), flush=True)
    print("PASS SEMANTIC CHALLENGE: 3 restored falsifications and second GREEN")


if __name__ == "__main__":
    from local_only import require_local
    require_local()
    main()
