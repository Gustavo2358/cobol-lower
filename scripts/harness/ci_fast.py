"""REMOTE FAST only: a fixed profile, never cumulative qualification."""
import json
import os
from pathlib import Path
import re
import time

from checks import document_errors, read_data
from ci_policy import classify_delta, git, orchestration_errors

ROOT = Path(__file__).resolve().parents[2]


def profile_steps(classification):
    common = ("checkout", "orchestration", "docs", "integrity", "git-scope", "harness-focal")
    if classification == "DOCS_ONLY":
        return common
    if classification != "CODE_OR_HARNESS_CHANGE":
        raise RuntimeError("unknown CI classification")
    return common + ("bootstrap-fast", "compile-reactor", "core-focal", "adapter-focal", "architecture")


def source_context(commit):
    if not commit or not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise RuntimeError("CI requires exact --commit SHA")
    if git(ROOT, "rev-parse", "HEAD").decode().strip() != commit or git(ROOT, "status", "--porcelain"):
        raise RuntimeError("CI checkout/head/worktree mismatch")
    event_name = os.environ.get("GITHUB_EVENT_NAME", "local-fast")
    if event_name == "local-fast":
        base = git(ROOT, "rev-parse", commit + "^").decode().strip()
    else:
        event = read_data(Path(os.environ["GITHUB_EVENT_PATH"]))
        if event["repository"]["full_name"] != "Gustavo2358/cobol-lower":
            raise RuntimeError("CI repository mismatch")
        if event_name == "push":
            if event["after"] != commit or not event["ref"].startswith("refs/heads/"):
                raise RuntimeError("CI push head mismatch")
            base = event["before"]
            if base == "0" * 40:
                base = git(ROOT, "merge-base", "origin/main", commit).decode().strip()
        elif event_name == "pull_request":
            pr = event["pull_request"]
            if pr["head"]["sha"] != commit or pr["base"]["ref"] != "main":
                raise RuntimeError("CI PR head/base mismatch")
            base = git(ROOT, "merge-base", pr["base"]["sha"], commit).decode().strip()
        else:
            raise RuntimeError("CI supports push/PR only; no manual qualification")
    return event_name, base


def integrity():
    # MANIFEST is explicit file-integrity evidence, never test qualification.
    import hashlib
    for line in (ROOT / "MANIFEST.sha256").read_text().splitlines():
        expected, path = line.split("  ", 1)
        if path.startswith("/") or ".." in Path(path).parts:
            raise RuntimeError("unsafe manifest path")
        if hashlib.sha256((ROOT / path).read_bytes()).hexdigest() != expected:
            raise RuntimeError("MANIFEST mismatch: " + path)


def entry(commit):
    import run as harness  # functions used below form the reviewed fast closure
    started = time.monotonic()
    event, base = source_context(commit)
    classification, paths = classify_delta(ROOT, base, commit)
    steps = profile_steps(classification)
    harness.fail_on(orchestration_errors(ROOT))
    harness.fail_on(document_errors(ROOT))
    integrity()
    harness.run(["git", "diff", "--check", base, commit])
    harness.run(["python3", "-m", "unittest", "discover", "-s", "scripts/harness/tests", "-p", "test_ci_policy.py", "-v"])
    if classification == "CODE_OR_HARNESS_CHANGE":
        harness.bootstrap(fast=True)
        # No lifecycle test executions. All Java tests compile, then fixed focal mains run.
        harness.run(harness.maven("-Dexec.skip=true", "-DskipTests=true", "install"))
        harness.verify_dependency()
        from focal import execute
        execute(ROOT, harness.build_root(), "fast")
        harness.architecture()
    receipt = dict(kind="FAST_CI", classification=classification, event=event, head_sha=commit,
                   checkout_sha=commit, source_tree=git(ROOT, "rev-parse", "HEAD^{tree}").decode().strip(),
                   base_sha=base, workflow=".github/workflows/checkpoint.yml", job="checkpoint",
                   steps=steps, changed_paths=paths, conclusion="success", exit_code=0,
                   elapsed_seconds=round(time.monotonic() - started, 3), qualification_executed=False)
    print(json.dumps(receipt, indent=2), flush=True)
    return receipt
