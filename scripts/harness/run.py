"""Gate entrypoints only. Checkpoint sequencing and authority remain in the canonical harness."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import subprocess
import sys

from architecture import architecture_errors
from checks import certificate_errors, digest, document_errors, read_data, remote_errors, work_manifest
from git_checks import candidate_errors, evidence_path, git, preflight
from full_checks import execute_full, performance_counts

ROOT = Path(__file__).resolve().parents[2]


def build_root():
    value = os.environ.get("LOWER_BUILD_ROOT")
    if not value:
        raise RuntimeError("LOWER_BUILD_ROOT must identify an isolated temporary build directory")
    path = Path(value).resolve()
    if path == ROOT or path in ROOT.parents or path == Path.home():
        raise RuntimeError("build directory must not be a repository/home root")
    path.mkdir(parents=True, exist_ok=True)
    return path


def run(command, cwd=ROOT):
    result = subprocess.run(command, cwd=cwd, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    print(result.stdout, end="", flush=True)
    if result.returncode:
        raise RuntimeError(f"command exit {result.returncode}: {command}")
    return result.stdout


def source():
    return next(s for s in read_data(ROOT / "docs/sources/sources.lock.json")["sources"] if s["id"] == "SRC-AIR-JAVA")


def maven(*args):
    return ["mvn", "-B", "-ntp", "-Dmaven.repo.local=" + str(build_root() / "m2"), *args]


def bootstrap():
    version = run(["java", "-version"])
    if not re.search(r'version "21[.\"]', version):
        raise RuntimeError("Java 21 runtime required")
    run(["mvn", "-version"])
    src = source()
    path = build_root() / "air-java"
    if not path.exists():
        run(["git", "clone", "--no-checkout", src["repository_url"] + ".git", str(path)])
        run(["git", "checkout", "--detach", src["commit"]], path)
    if git(path, "rev-parse", "HEAD").decode().strip() != src["commit"] or git(path, "status", "--porcelain"):
        raise RuntimeError("upstream snapshot differs from lock or has local changes")
    log = run(maven("clean", "install"), path)
    if not re.search(r"PASS: [1-9][0-9]* deterministic contract checks", log):
        raise RuntimeError("upstream contract tests absent")
    jar = build_root() / "m2/io/github/gustavo2358/air-java/0.1.0-SNAPSHOT/air-java-0.1.0-SNAPSHOT.jar"
    provenance = dict(repository=src["repository"], commit=src["commit"], jar_sha256=digest(jar.read_bytes()),
                      source_lock_sha256=digest((ROOT / "docs/sources/sources.lock.json").read_bytes()),
                      java=version.strip())
    (build_root() / "air-provenance.json").write_text(json.dumps(provenance, indent=2) + "\n")
    (build_root() / "air-build.log").write_text(log)
    print(json.dumps(provenance))


def verify_dependency():
    provenance = read_data(build_root() / "air-provenance.json")
    jar = build_root() / "m2/io/github/gustavo2358/air-java/0.1.0-SNAPSHOT/air-java-0.1.0-SNAPSHOT.jar"
    if provenance["commit"] != source()["commit"] or provenance["jar_sha256"] != digest(jar.read_bytes()) or provenance["source_lock_sha256"] != digest((ROOT / "docs/sources/sources.lock.json").read_bytes()):
        raise RuntimeError("unproven or changed air-java artifact")
    return jar


def semantic(extra=()):
    verify_dependency()
    output = run(maven(*extra, "verify"))
    counts = [int(n) for n in re.findall(r"^LOWER_TESTS=([0-9]+)$", output, re.M)]
    checkpoint = work_manifest(ROOT, "WORK-LOWER-001")["authorization"]["current_checkpoint"]
    expected_suites = 1 if checkpoint == "CP0" else 2
    if len(counts) != expected_suites or min(counts) <= 0:
        raise RuntimeError("semantic tests absent/zero")
    print("SEMANTIC_TEST_COUNT=" + str(sum(counts)))
    return output


def performance():
    output = semantic(("-Dlower.performance=true",))
    print("PERFORMANCE_TEST_COUNT=" + str(performance_counts(output)))


def git_gate(record, commit=None):
    if commit is None:
        fail_on(preflight(ROOT, record))
    else:
        fail_on(certificate_errors(ROOT, record) + candidate_errors(ROOT, record, commit))
        if git(ROOT, "rev-parse", "HEAD").decode().strip() != commit or git(ROOT, "status", "--porcelain"):
            raise RuntimeError("GIT_PUBLISHED_HEAD_OR_WORKTREE")
    check_pr(record, commit)


def challenge():
    run([sys.executable, "scripts/harness/challenge.py"])
    run([sys.executable, "scripts/harness/semantic_challenge.py"])


def full(record, commit=None):
    execute_full(record["frozen_contract"]["required_gates"], {
        "docs": lambda: fail_on(document_errors(ROOT)),
        "semantic": semantic,
        "performance": performance,
        "architecture": architecture,
        "git": lambda: git_gate(record, commit),
        "harness-tests": lambda: run([sys.executable, "-m", "unittest", "discover", "-s", "scripts/harness/tests", "-v"]),
        "challenge": challenge,
    })


def architecture():
    jar = verify_dependency()
    # JSON is the dependency plugin's output, outside the Java domain.
    run(maven("-pl", "core", "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree",
              "-DoutputType=json", "-DoutputFile=target/dependency-tree.json"))
    tree = read_data(ROOT / "core/target/dependency-tree.json")
    fail_on(architecture_errors(ROOT, tree, jar))


def fail_on(errors):
    if errors:
        raise RuntimeError("\n".join(errors))


def gh(*args):
    return json.loads(subprocess.check_output(["gh", *args], cwd=ROOT))


def check_pr(record, sha=None):
    prs = gh("pr", "list", "--repo", record["repository"], "--head", record["branch"], "--state", "all",
             "--json", "number,url,state,isDraft,headRefOid,baseRefName,autoMergeRequest")
    if len(prs) != 1:
        if not prs and record["checkpoint"] == "CP0" and record["pull_request"] is None and sha is None:
            print("PR_INITIAL_CREATION_PENDING: only before the first certified commit")
            return None
        raise RuntimeError("exactly one PR required")
    pr = prs[0]
    if (pr["state"] != "OPEN" or pr["baseRefName"] != "main" or pr.get("autoMergeRequest") is not None
            or (record["pull_request"] is not None and pr["number"] != record["pull_request"])
            or (sha is not None and pr["headRefOid"] != sha)):
        raise RuntimeError("PR identity/state/head mismatch")
    print(json.dumps(pr))
    return pr


def remote(record, sha):
    pr = check_pr(record, sha)
    remote_sha = git(ROOT, "ls-remote", "origin", "refs/heads/" + record["branch"]).decode().split()[0]
    repo = record["repository"]
    checks = gh("api", f"repos/{repo}/commits/{sha}/check-runs?per_page=100")["check_runs"]
    normalized = []
    for check in checks:
        if check["name"] not in {c["name"] for c in record["frozen_contract"]["required_remote_checks"]}:
            continue
        match = re.search(r"/actions/runs/(\d+)", check.get("details_url") or "")
        if not match:
            continue
        run_id = int(match.group(1))
        info = gh("api", f"repos/{repo}/actions/runs/{run_id}")
        if info["head_sha"] != sha or info["head_branch"] != record["branch"]:
            continue
        normalized.append(dict(name=check["name"], workflow=info["path"], app_slug=check["app"]["slug"],
            event=info["event"], head_sha=check["head_sha"], status=check["status"],
            conclusion=check["conclusion"], id=check["id"], run_id=run_id, url=check["details_url"]))
    receipt = dict(pushed_sha=remote_sha, queried_at=datetime.now(timezone.utc).isoformat(),
                   pull_request=pr["number"], checks=normalized)
    print(json.dumps(receipt, indent=2))
    fail_on(remote_errors(record["frozen_contract"]["required_remote_checks"], sha, receipt))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("gate", choices=["bootstrap", "docs", "architecture", "fast", "semantic", "performance", "full", "challenge", "git", "harness-tests", "certify", "verify-commit", "remote", "challenge-return"])
    parser.add_argument("--evidence")
    parser.add_argument("--commit")
    args = parser.parse_args()
    if args.evidence is None:
        if args.commit:
            args.evidence = evidence_path(ROOT, args.commit)
        else:
            checkpoint = work_manifest(ROOT, "WORK-LOWER-001")["authorization"]["current_checkpoint"]
            args.evidence = f"docs/quality/WORK-LOWER-001/{checkpoint}.json"
    if args.commit:
        record = json.loads(git(ROOT, "show", args.commit + ":" + args.evidence))
    else:
        record = read_data(ROOT / args.evidence)
    if args.gate == "bootstrap":
        bootstrap()
    elif args.gate == "docs":
        fail_on(document_errors(ROOT))
    elif args.gate == "architecture":
        architecture()
    elif args.gate == "fast":
        fail_on(document_errors(ROOT))
        architecture()
    elif args.gate == "semantic":
        semantic()
    elif args.gate == "performance":
        performance()
    elif args.gate == "full":
        full(record, args.commit)
    elif args.gate == "challenge":
        challenge()
    elif args.gate == "challenge-return":
        semantic(("-Dchallenge.halt=true",))
    elif args.gate == "harness-tests":
        run([sys.executable, "-m", "unittest", "discover", "-s", "scripts/harness/tests", "-v"])
    elif args.gate == "git":
        git_gate(record, args.commit)
    elif args.gate in ("certify", "verify-commit"):
        if args.gate == "verify-commit" and not args.commit:
            raise RuntimeError("--commit required")
        fail_on(document_errors(ROOT) + certificate_errors(ROOT, record)
                + candidate_errors(ROOT, record, args.commit))
    elif args.gate == "remote":
        if not args.commit:
            raise RuntimeError("--commit required")
        fail_on(certificate_errors(ROOT, record) + candidate_errors(ROOT, record, args.commit))
        remote(record, args.commit)
    print("PASS " + args.gate)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, subprocess.CalledProcessError, ValueError, KeyError) as ex:
        print("FAIL " + str(ex), file=sys.stderr)
        sys.exit(1)
