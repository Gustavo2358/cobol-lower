"""Closed remote orchestration contract and conservative documentary Git comparison.

The workflow command allowlist rejects arbitrary scripts, aliases and reusable jobs.
The reviewed entrypoint closure is content-locked; editing it requires explicit lock
review, never a diff-derived product test selection. Runtime local-only guards are a
second boundary. This guards accidental orchestration changes, not hostile edits of
the guard and its authority together.
"""
import hashlib
import json
from pathlib import Path, PurePosixPath
import subprocess
import yaml

WORKFLOW = ".github/workflows/checkpoint.yml"
FAST_COMMAND = 'python3 scripts/harness/run.py ci-fast --commit "$CHECKOUT_SHA"'
PIP_COMMAND = "python3 -m pip install -r scripts/harness/requirements.txt"
PROTECTED_DOCS = ("docs/sources/sources.lock.json", "docs/engineering/gates.json", "docs/templates/",
                  "docs/quality/WORK-LOWER-011/qualification-contract.json")


def documentary(path):
    p = PurePosixPath(path)
    if p.is_absolute() or ".." in p.parts or "\\" in path:
        return False
    if any(path == s or s.endswith("/") and path.startswith(s) for s in PROTECTED_DOCS):
        return False
    if path.startswith("docs/quality/") and ("freeze" in p.name.lower() or p.name.endswith("source-guard.json") or p.name.startswith("CP") and p.suffix in (".json", ".yaml")):
        return False  # Executable frozen expectations/source guards are never receipts-only.
    if path in ("README.md", "ARCHITECTURE.md", "MANIFEST.sha256"):
        return True
    if path.startswith("docs/") and p.suffix == ".md":
        return True
    # Data-only receipts/lifecycle: extensions are closed; executable contracts excluded.
    if path.startswith(("docs/work/", "docs/quality/")):
        return p.suffix in (".json", ".yaml", ".txt", ".log", ".sha256", ".gz")
    return False


def classify(paths):
    return "DOCS_ONLY" if paths and all(documentary(p) for p in paths) else "CODE_OR_HARNESS_CHANGE"


def git(root, *args):
    return subprocess.check_output(["git", "-C", str(root), *args])


def protected_blobs(root, revision):
    blobs = {}
    for row in git(root, "ls-tree", "-r", "-z", revision).split(b"\0"):
        if not row:
            continue
        info, path = row.split(b"\t", 1)
        name = path.decode("utf-8")
        if not documentary(name):
            blobs[name] = info.decode("ascii")  # includes mode/type and blob identity
    return blobs


def classify_delta(root, base, head):
    paths = git(root, "diff", "--no-renames", "--name-only", "-z", base, head).decode().split("\0")[:-1]
    result = classify(paths)
    if result == "DOCS_ONLY" and protected_blobs(root, base) != protected_blobs(root, head):
        raise RuntimeError("DOCS_ONLY_PROTECTED_BLOB_CHANGED")
    return result, paths


def qualification_relation(root, qualified, current):
    """The original HEAD/tree retains its receipt; docs do not inherit an execution."""
    qt = git(root, "rev-parse", qualified + "^{tree}").decode().strip()
    ct = git(root, "rev-parse", current + "^{tree}").decode().strip()
    if qualified == current:
        state = "QUALIFIED_HEAD"
    elif qt == ct:
        state = "QUALIFIED_TREE_PRESERVED"
    elif classify_delta(root, qualified, current)[0] == "DOCS_ONLY":
        state = "DOCUMENTATION_ONLY_SUCCESSOR"
    else:
        raise RuntimeError("LOCAL_FULL_QUALIFICATION_INVALIDATED")
    return dict(qualified_head=qualified, qualified_tree=qt, current_head=current, current_tree=ct,
                relation=state, full_executed_on_current_head=qualified == current)


def orchestration_errors(root):
    errors = []
    files = sorted(p.relative_to(root).as_posix() for p in (root / ".github/workflows").glob("*") if p.is_file())
    if files != [WORKFLOW]:
        errors.append("CI_ORCHESTRATION only the reviewed checkpoint workflow is allowed; remote manual full prohibited")
    try:
        w = yaml.safe_load((root / WORKFLOW).read_text())
        if set(w) != {"name", "on", "permissions", "jobs"} or set(w["on"]) != {"push", "pull_request"}:
            errors.append("CI_ORCHESTRATION unreviewed event/workflow entrypoint")
        if set(w["jobs"]) != {"checkpoint"}:
            errors.append("CI_ORCHESTRATION unreviewed job/reusable workflow")
        job = w["jobs"]["checkpoint"]
        if set(job) != {"runs-on", "timeout-minutes", "env", "steps"} or job["timeout-minutes"] != 15:
            errors.append("CI_ORCHESTRATION unreviewed job orchestration/timeout")
        steps = job["steps"]
        uses = [s["uses"] for s in steps if "uses" in s]
        if uses != ["actions/checkout@v4", "actions/setup-java@v4", "actions/setup-python@v5"]:
            errors.append("CI_ORCHESTRATION unreviewed action/alias")
        commands = [s["run"] for s in steps if "run" in s]
        if commands != [PIP_COMMAND, FAST_COMMAND] or len(steps) != 5:
            errors.append("CI_ORCHESTRATION only ci-fast may execute remotely; no bootstrap/full/performance/challenge aliases")
        if steps[0].get("with") != {"fetch-depth": 0, "ref": "${{ github.event.pull_request.head.sha || github.sha }}"}:
            errors.append("CI_ORCHESTRATION checkout must be the exact published head")
        for step in steps:
            if set(step) - {"name", "uses", "with", "run"}:
                errors.append("CI_ORCHESTRATION unreviewed step control/env/shell")
        if job["env"].get("CHECKOUT_SHA") != "${{ github.event.pull_request.head.sha || github.sha }}":
            errors.append("CI_ORCHESTRATION head binding")
        expected_env = dict(LOWER_BUILD_ROOT="/tmp/lower-build-${{ github.run_id }}-${{ github.run_attempt }}",
                            GH_TOKEN="${{ github.token }}", MAVEN_OPTS="-Xmx3g",
                            CHECKOUT_SHA="${{ github.event.pull_request.head.sha || github.sha }}")
        if job["env"] != expected_env or job["runs-on"] != "ubuntu-latest":
            errors.append("CI_ORCHESTRATION unreviewed environment/runner")
        if w["on"] != {"push": {"branches": ["**"]}, "pull_request": {"branches": ["main"]}}:
            errors.append("CI_ORCHESTRATION event selection must retain push/PR/main FAST")
        if steps[1].get("with") != {"distribution": "temurin", "java-version": "21"} or steps[2].get("with") != {"python-version": "3.12"}:
            errors.append("CI_ORCHESTRATION unreviewed setup configuration")
        # Reviewed code closure protects hidden aliases introduced behind the workflow.
        lock = json.loads((root / "scripts/harness/remote-fast.lock.json").read_text())
        if set(reviewed_paths(root)) != set(lock["files"]):
            errors.append("CI_ORCHESTRATION missing reviewed entrypoint closure")
        for path, expected in lock["files"].items():
            if hashlib.sha256((root / path).read_bytes()).hexdigest() != expected:
                errors.append("CI_ORCHESTRATION unreviewed entrypoint blob: " + path)
    except (OSError, ValueError, KeyError, TypeError, yaml.YAMLError) as ex:
        errors.append("CI_ORCHESTRATION malformed closed contract: " + str(ex))
    return errors


def reviewed_paths(root):
    paths = {"core/pom.xml", "adapters/pom.xml", "pom.xml", "scripts/harness/requirements.txt",
             "scripts/harness/tests/test_ci_policy.py"}
    paths |= {p.relative_to(root).as_posix() for p in (root / "scripts/harness").glob("*.py")}
    for module in ("core", "adapters"):
        paths |= {p.relative_to(root).as_posix() for p in (root / module / "src/test/java").rglob("*.java")}
    return sorted(paths)
