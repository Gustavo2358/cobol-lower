"""Exact committed-tree qualification, with non-circular receipts outside the checkout."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
from datetime import datetime, timezone

from ci_policy import git, protected_blobs, qualification_relation, orchestration_errors
from local_only import require_local

ROOT = Path(__file__).resolve().parents[2]
CONTRACT = "docs/quality/WORK-LOWER-011/qualification-contract.json"
STAGES = ("bootstrap", "docs", "semantic", "performance", "architecture", "harness-tests",
          "challenge", "w2b-challenge", "producer-e2e", "second-green", "git-certificate")


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def candidate(root, commit):
    if not commit or not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise RuntimeError("qualification requires exact --commit SHA")
    if git(root, "rev-parse", "HEAD").decode().strip() != commit or git(root, "status", "--porcelain"):
        raise RuntimeError("qualification requires clean committed HEAD")
    contract = json.loads(git(root, "show", commit + ":" + CONTRACT))
    if contract["kind"] != "LOCAL_QUALIFICATION_CONTRACT" or tuple(contract["stages"]) != STAGES:
        raise RuntimeError("qualification stage contract differs")
    branch = git(root, "branch", "--show-current").decode().strip()
    if branch != contract["branch"] or contract["work_item"] != "WORK-LOWER-011":
        raise RuntimeError("qualification work/branch mismatch")
    import yaml
    manifest = yaml.safe_load((root / contract["manifest"]).read_text())
    if manifest["status"] != "active" or manifest["authorization"]["state"] != "granted" or manifest["authorization"]["authorized_checkpoints"] != ["CP0"]:
        raise RuntimeError("qualification lacks current execution authority")
    for path, expected in contract["frozen_files"].items():
        if digest((root / path).read_bytes()) != expected:
            raise RuntimeError("qualification frozen expected changed: " + path)
    paths = git(root, "diff", "--no-renames", "--name-only", "-z", contract["baseline"], commit).decode().split("\0")[:-1]
    allowed = ("core/", "adapters/", "scripts/harness/", "docs/", ".github/workflows/")
    if any(not p.startswith(allowed) and p not in ("pom.xml", "MANIFEST.sha256", "README.md", "ARCHITECTURE.md") for p in paths):
        raise RuntimeError("qualification scope violation")
    git(root, "diff", "--check", contract["baseline"], commit)
    errors = orchestration_errors(root)
    if errors:
        raise RuntimeError("\n".join(errors))
    return contract


def verify_receipt(root, path, current):
    receipt = json.loads(path.read_text())
    if receipt["kind"] != "LOCAL_QUALIFICATION" or receipt["conclusion"] != "success":
        raise RuntimeError("not a successful LOCAL_QUALIFICATION receipt")
    if tuple(x["stage"] for x in receipt["stages"]) != STAGES or any(x["exit_code"] != 0 for x in receipt["stages"]):
        raise RuntimeError("incomplete local qualification stages")
    qualified = receipt["qualified_head"]
    if git(root, "rev-parse", qualified + "^{tree}").decode().strip() != receipt["qualified_tree"]:
        raise RuntimeError("receipt qualified tree mismatch")
    if digest(git(root, "show", qualified + ":" + CONTRACT)) != receipt["contract_sha256"]:
        raise RuntimeError("receipt contract mismatch")
    for stage in receipt["stages"]:
        if digest((path.parent / stage["log"]).read_bytes()) != stage["log_sha256"]:
            raise RuntimeError("qualification log mismatch")
    return qualification_relation(root, qualified, current)


def qualify(commit):
    require_local()
    contract = candidate(ROOT, commit)
    import run as harness
    from focal import execute
    build = harness.build_root()
    out = build / "qualification" / (commit + "-" + str(time.time_ns()))
    if out.is_relative_to(ROOT):
        raise RuntimeError("qualification receipt must live outside qualified checkout")
    out.mkdir(parents=True)
    baseline = protected_blobs(ROOT, commit)
    receipt = dict(kind="LOCAL_QUALIFICATION", qualified_head=commit,
                   qualified_tree=git(ROOT, "rev-parse", "HEAD^{tree}").decode().strip(),
                   contract_sha256=digest((ROOT / CONTRACT).read_bytes()),
                   started_at=datetime.now(timezone.utc).isoformat(), stages=[], conclusion="running")
    receipt_path = out / "receipt.json"
    def stage(name, command):
        start = time.monotonic()
        log = out / (name + ".log")
        with log.open("xb") as stream:
            result = subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT)
        receipt["stages"].append(dict(stage=name, command=command, exit_code=result.returncode,
            elapsed_seconds=round(time.monotonic()-start, 3), log=log.name, log_sha256=digest(log.read_bytes())))
        receipt_path.write_text(json.dumps(receipt, indent=2) + "\n")
        print(f"LOCAL_QUALIFICATION {name}: exit={result.returncode} log={log}", flush=True)
        if result.returncode:
            raise RuntimeError("qualification stage failed: " + name)
    try:
        for name in STAGES[:7]:
            stage(name, ["python3", "scripts/harness/run.py", name])
        stage("w2b-challenge", ["python3", "scripts/harness/if_challenge.py"])
        stage("producer-e2e", ["python3", "scripts/harness/w2b_e2e.py", "--out", str(out / "producer")])
        stage("second-green", ["python3", "-c", "from pathlib import Path; import sys; sys.path.insert(0,'scripts/harness'); import run; from focal import execute; execute(Path.cwd(),run.build_root(),'fast')"])
        stage("git-certificate", ["python3", "-c", "import sys; sys.path.insert(0,'scripts/harness'); from qualification_local import candidate,ROOT; candidate(ROOT,sys.argv[1]); print('PASS git/certificate')", commit])
        if protected_blobs(ROOT, commit) != baseline or git(ROOT, "status", "--porcelain"):
            raise RuntimeError("qualification restoration failure")
        receipt["conclusion"] = "success"
        receipt["restoration"] = "BYTE_EXACT_SECOND_GREEN"
    except Exception:
        receipt["conclusion"] = "failure"
        raise
    finally:
        receipt["finished_at"] = datetime.now(timezone.utc).isoformat()
        receipt_path.write_text(json.dumps(receipt, indent=2) + "\n")
        print("LOCAL_QUALIFICATION_RECEIPT=" + str(receipt_path), flush=True)
    verify_receipt(ROOT, receipt_path, commit)
