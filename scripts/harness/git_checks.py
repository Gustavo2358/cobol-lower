"""Git-backed verification of FREEZE bytes, candidate identity and recovery commits."""
import json
import re
from pathlib import Path
import subprocess

from checks import digest, read_data

DIFF_OPTIONS = ["-c", "core.quotePath=true", "-c", "diff.algorithm=myers", "-c", "diff.renames=false",
                "diff", "--cached", "--binary", "--no-ext-diff", "--no-textconv", "--full-index", "--no-color"]


def evidence_path(root, commit):
    trailers = git(root, "show", "-s", "--format=%(trailers:key=Checkpoint-Evidence,valueonly)", commit).decode().splitlines()
    paths = [line.strip() for line in trailers if line.strip()]
    if len(paths) != 1 or not re.fullmatch(r"docs/quality/WORK-[A-Z][A-Z0-9-]*-[0-9]{3}/CP[0-9]+\.json", paths[0]):
        raise ValueError("exactly one safe Checkpoint-Evidence trailer required")
    return paths[0]


def git(root, *arguments):
    return subprocess.check_output(["git", *arguments], cwd=root)


def exclusions(record):
    return [f"docs/quality/{record['work_item']}/{record['checkpoint']}.json",
            f"docs/work/active/{record['work_item']}/state.md"]


def candidate_digest(root, record, commit=None):
    options = list(DIFF_OPTIONS)
    if commit:
        options.remove("--cached")
    args = options + [record["base_commit"]] + ([commit] if commit else [])
    args += ["--", "."] + [":(exclude)" + path for path in exclusions(record)]
    return digest(git(root, *args))


def frozen_errors(root, record, commit=None):
    errors = []
    for ref in record["frozen_contract"]["references"]:
        try:
            revision = ref["git_revision"] or commit
            data = git(root, "show", revision + ":" + ref["path"]) if revision else (Path(root) / ref["path"]).read_bytes()
            if digest(data) != ref["sha256"]:
                errors.append("FREEZE_HASH " + ref["path"])
            if ref["path"] == "docs/sources/sources.lock.json":
                candidate = git(root, "show", commit + ":" + ref["path"]) if commit else (Path(root) / ref["path"]).read_bytes()
                if digest(candidate) != ref["sha256"]:
                    errors.append("SOURCE_LOCK_CHANGED")
        except (OSError, subprocess.CalledProcessError):
            errors.append("FREEZE_UNAVAILABLE " + ref["path"])
    return errors


def candidate_errors(root, record, commit=None):
    errors = frozen_errors(root, record, commit)
    if record.get("candidate_exclusions") != exclusions(record):
        errors.append("CANDIDATE_EXCLUSIONS")
    if record.get("diff_options") != DIFF_OPTIONS:
        errors.append("CANDIDATE_DIFF_OPTIONS")
    if candidate_digest(root, record, commit) != record.get("candidate_diff_sha256"):
        errors.append("CANDIDATE_DIGEST")
    if commit:
        try:
            committed_record = json.loads(git(root, "show", commit + ":" + exclusions(record)[0]))
            if committed_record != record:
                errors.append("COMMITTED_EVIDENCE")
        except (ValueError, subprocess.CalledProcessError):
            errors.append("COMMITTED_EVIDENCE")
        parent = git(root, "rev-parse", commit + "^").decode().strip()
        if parent != record.get("reviewed_head"):
            errors.append("REVIEWED_HEAD")
        trailer = "Checkpoint-Evidence: " + exclusions(record)[0]
        if trailer not in git(root, "show", "-s", "--format=%B", commit).decode().splitlines():
            errors.append("CHECKPOINT_TRAILER")
    else:
        if git(root, "rev-parse", "HEAD").decode().strip() != record.get("reviewed_head"):
            errors.append("REVIEWED_HEAD")
        if git(root, "diff", "--name-only") or git(root, "ls-files", "--others", "--exclude-standard"):
            errors.append("INDEX_WORKTREE_MISMATCH")
    return errors


def preflight(root, record):
    errors = frozen_errors(root, record)
    branch = git(root, "branch", "--show-current").decode().strip()
    if branch != record["branch"] or branch == "main":
        errors.append("GIT_BRANCH")
    if subprocess.run(["git", "merge-base", "--is-ancestor", record["base_commit"], "HEAD"], cwd=root).returncode:
        errors.append("GIT_BASE")
    if subprocess.run(["git", "diff", "--check"], cwd=root).returncode:
        errors.append("GIT_WHITESPACE")
    if subprocess.run(["git", "diff", "--cached", "--check"], cwd=root).returncode:
        errors.append("GIT_STAGED_WHITESPACE")
    manifest = read_data(Path(root) / f"docs/work/active/{record['work_item']}/work-item.yaml")
    scopes = [p.removeprefix("planned:") for key in ("source_scope", "test_scope", "docs_scope") for p in manifest[key]]
    changed = set(git(root, "diff", "--name-only", record["base_commit"]).decode().splitlines())
    changed.update(git(root, "ls-files", "--others", "--exclude-standard").decode().splitlines())
    for path in changed:
        if not any(path == scope or path.startswith(scope + "/") for scope in scopes):
            errors.append("GIT_SCOPE " + path)
    return errors
