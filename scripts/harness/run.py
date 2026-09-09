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
from production_challenge import scope_errors
from checks import certificate_errors, digest, document_errors, read_data, remote_errors, execution_authority
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
    codec = jar.parents[2] / "air-json/0.1.0-SNAPSHOT/air-json-0.1.0-SNAPSHOT.jar"
    provenance = dict(repository=src["repository"], commit=src["commit"], jar_sha256=digest(jar.read_bytes()),
                      codec_sha256=digest(codec.read_bytes()),
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
    codec = jar.parents[2] / "air-json/0.1.0-SNAPSHOT/air-json-0.1.0-SNAPSHOT.jar"
    if provenance.get("codec_sha256") != digest(codec.read_bytes()):
        raise RuntimeError("unproven or changed air-json artifact")
    return jar


def semantic(extra=()):
    verify_dependency()
    output = run(maven(*extra, "verify"))
    counts = [int(n) for n in re.findall(r"^LOWER_TESTS=([0-9]+)$", output, re.M)]
    expected_suites = 2  # Current first-slice core + adapters, independent of work-item identity.
    if len(counts) != expected_suites or min(counts) <= 0:
        raise RuntimeError("semantic tests absent/zero")
    output_counts = [int(n) for n in re.findall(r"^LOWER_AIR_OUTPUT_TESTS=([0-9]+)$", output, re.M)]
    if len(output_counts) != 1 or output_counts[0] <= 0:
        raise RuntimeError("AIR output tests absent/zero/duplicate")
    production_counts = re.findall(r"^LOWER_PRODUCTION_TESTS=([0-9]+)$", output, re.M)
    if len(production_counts) != 1 or int(production_counts[0]) <= 0 or "PRODUCTION_SUCCESS_PROBE data=400 moves=400" not in output:
        raise RuntimeError("production path probe absent/zero")
    if "-Dlower.performance=true" in extra and "PRODUCTION_LIMIT_ORDER data=1 moves=10000" not in output:
        raise RuntimeError("production limit-order probe absent")
    if "-Dlower.performance=true" in extra:
        capacity = re.findall(r"^LOWER_CAPACITY_TESTS=([0-9]+)$", output, re.M)
        if len(capacity) != 1 or int(capacity[0]) <= 0 or "CAPACITY_DETERMINISM=PASS" not in output:
            raise RuntimeError("capacity proof absent/zero/duplicate")
    print("SEMANTIC_TEST_COUNT=" + str(sum(counts)))
    return output


def performance():
    output = semantic(("-Dlower.performance=true",))
    print("PERFORMANCE_TEST_COUNT=" + str(performance_counts(output)))


def git_gate(record, commit=None, mode="execution"):
    if mode == "historical":
        if commit is None:
            raise RuntimeError("historical audit requires a certified commit")
        fail_on(certificate_errors(ROOT, record) + candidate_errors(ROOT, record, commit))
        check_pr(record, commit, mode="historical")
        return
    if commit is None:
        fail_on(preflight(ROOT, record))
    else:
        fail_on(certificate_errors(ROOT, record) + candidate_errors(ROOT, record, commit))
        if git(ROOT, "rev-parse", "HEAD").decode().strip() != commit or git(ROOT, "status", "--porcelain"):
            raise RuntimeError("GIT_PUBLISHED_HEAD_OR_WORKTREE")
        execution_authority(ROOT, record)
    check_pr(record, commit)


def challenge():
    run([sys.executable, "scripts/harness/challenge.py"])
    run([sys.executable, "scripts/harness/semantic_challenge.py"])
    run([sys.executable, "scripts/harness/review_challenge.py"])
    run([sys.executable, "scripts/harness/output_challenge.py"])
    run([sys.executable, "scripts/harness/scalar_challenge.py"])
    run([sys.executable, "scripts/harness/production_challenge.py"])
    run([sys.executable, "scripts/harness/capacity_challenge.py"])


def full(record, commit=None, mode="execution"):
    execute_full(record["frozen_contract"]["required_gates"], {
        "docs": lambda: fail_on(document_errors(ROOT)),
        "semantic": semantic,
        "performance": performance,
        "architecture": architecture,
        "git": lambda: git_gate(record, commit) if mode == "execution" else git_gate(record, commit, mode=mode),
        "harness-tests": lambda: run([sys.executable, "-m", "unittest", "discover", "-s", "scripts/harness/tests", "-v"]),
        "challenge": challenge,
    })


def architecture():
    jar = verify_dependency()
    # JSON is the dependency plugin's output, outside the Java domain.
    run(maven("-pl", "core", "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree",
              "-DoutputType=json", "-DoutputFile=target/dependency-tree.json"))
    tree = read_data(ROOT / "core/target/dependency-tree.json")
    hash_jar = build_root() / "m2/com/dynatrace/hash4j/hash4j/0.30.0/hash4j-0.30.0.jar"
    fail_on(architecture_errors(ROOT, tree, jar, hash_jar) + scope_errors(ROOT))


def fail_on(errors):
    if errors:
        raise RuntimeError("\n".join(errors))


def gh(*args):
    return json.loads(subprocess.check_output(["gh", *args], cwd=ROOT))


def check_pr(record, sha=None, mode="execution"):
    if mode not in ("execution", "historical"):
        raise RuntimeError("unknown PR verification mode")
    prs = gh("pr", "list", "--repo", record["repository"], "--head", record["branch"], "--state", "all",
             "--json", "number,url,state,isDraft,headRefOid,headRefName,baseRefName,autoMergeRequest,reviewDecision,mergeCommit")
    if len(prs) != 1:
        if not prs and record["checkpoint"] == "CP0" and record["pull_request"] is None and sha is None:
            print("PR_INITIAL_CREATION_PENDING: only before the first certified commit")
            return None
        raise RuntimeError("exactly one PR required")
    pr = prs[0]
    allowed_states = ("OPEN",) if mode == "execution" else ("OPEN", "MERGED")
    if (pr["state"] not in allowed_states or pr["baseRefName"] != "main"
            or pr.get("headRefName") != record["branch"]
            or (mode == "execution" and pr.get("autoMergeRequest") is not None)
            or (record["pull_request"] is not None and pr["number"] != record["pull_request"])
            or (mode == "execution" and sha is not None and pr["headRefOid"] != sha)):
        raise RuntimeError("PR identity/state/head mismatch")
    if mode == "historical" and sha is not None and pr["headRefOid"] != sha:
        git(ROOT, "merge-base", "--is-ancestor", sha, pr["headRefOid"])
    print(json.dumps(pr))
    return pr


def remote(record, sha, mode="execution"):
    pr = check_pr(record, sha, mode=mode)
    remote_sha = sha if mode == "historical" else git(ROOT, "ls-remote", "origin", "refs/heads/" + record["branch"]).decode().split()[0]
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


def reconcile_errors(registration, pr, reviews):
    """Compare a recorded observation to trusted adapter metadata, without granting authority."""
    errors = []
    observed_merge = "merged" if pr["state"] == "MERGED" else "open_not_merged" if pr["state"] == "OPEN" else "closed_unmerged"
    if registration["merge_status"] != observed_merge:
        errors.append("PR_RECONCILIATION merge status")
    if registration["review_status"] == "not_recorded":
        observation = registration.get("remote_observation", {})
        if (reviews or observation.get("head_sha") != pr["headRefOid"]
                or observation.get("merge_commit") != (pr.get("mergeCommit") or {}).get("oid")):
            errors.append("PR_RECONCILIATION absent review/head/merge not confirmed")
    if registration["review_status"] in ("reviewed", "approved"):
        relevant = [r for r in reviews if r.get("commit_id") == pr["headRefOid"] and r.get("state") in ("APPROVED", "COMMENTED", "CHANGES_REQUESTED")]
        if not relevant or (registration["review_status"] == "approved" and relevant[-1]["state"] != "APPROVED"):
            errors.append("PR_RECONCILIATION review not confirmed on head")
        observation = registration.get("remote_observation", {})
        if observation.get("head_sha") != pr["headRefOid"] or (observed_merge == "merged" and observation.get("merge_commit") != (pr.get("mergeCommit") or {}).get("oid")):
            errors.append("PR_RECONCILIATION recorded head/merge commit")
    return errors


def ci_target(sha, event):
    """Push-head execution or independently proven merge-head audit; no branch-name fallback."""
    if not re.fullmatch(r"[0-9a-f]{40}", sha) or event.get("after") != sha or not event.get("ref", "").startswith("refs/heads/"):
        raise RuntimeError("CI push SHA/ref mismatch")
    branch = event["ref"].removeprefix("refs/heads/")
    certified = sha
    mode = "execution"
    if branch == "main":
        repository = event["repository"]["full_name"]
        pulls = gh("api", f"repos/{repository}/commits/{sha}/pulls")
        matches = [p for p in pulls if p.get("merged_at") and p.get("merge_commit_sha") == sha
                   and p["base"]["ref"] == "main" and p["base"]["repo"]["full_name"] == repository]
        if len(matches) != 1:
            raise RuntimeError("CI main requires unique proven merged PR")
        pr = matches[0]
        certified = pr["head"]["sha"]
        git(ROOT, "merge-base", "--is-ancestor", certified, sha)
        mode = "historical"
    path = evidence_path(ROOT, certified)
    record = json.loads(git(ROOT, "show", certified + ":" + path))
    if record["repository"] != event["repository"]["full_name"]:
        raise RuntimeError("CI repository mismatch")
    if mode == "execution" and record["branch"] != branch:
        raise RuntimeError("CI branch/evidence mismatch")
    if mode == "historical":
        if record["pull_request"] != pr["number"] or record["branch"] != pr["head"]["ref"] or json.loads(git(ROOT, "show", sha + ":" + path)) != record:
            raise RuntimeError("CI merge/evidence identity mismatch")
    return record, certified, mode


def current_evidence():
    registry = read_data(ROOT / "docs/work/registry.json")
    paths = []
    for item in registry["active"]:
        work = read_data(ROOT / item["path"] / "work-item.yaml")
        paths.append(f"docs/quality/{item['id']}/{work['authorization']['current_checkpoint']}.json")
    for item in registry["history"]:
        paths.extend(r["evidence"] for r in item.get("remediations", []) if r["status"] in ("active", "ready_for_review"))
    if len(paths) > 1:
        raise RuntimeError("explicit --evidence required for multiple transactions")
    return paths[0] if paths else evidence_path(ROOT, "HEAD")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("gate", choices=["bootstrap", "docs", "architecture", "fast", "semantic", "performance", "full", "challenge", "git", "harness-tests", "certify", "verify-commit", "remote", "challenge-return", "ci", "reconcile"])
    parser.add_argument("--evidence")
    parser.add_argument("--commit")
    parser.add_argument("--mode", choices=["execution", "historical"], default="execution")
    args = parser.parse_args()
    if args.gate == "ci":
        if os.environ.get("GITHUB_EVENT_NAME") != "push" or not args.commit:
            raise RuntimeError("CI requires explicit push context")
        event = read_data(Path(os.environ["GITHUB_EVENT_PATH"]))
        if git(ROOT, "rev-parse", "HEAD").decode().strip() != args.commit or git(ROOT, "status", "--porcelain"):
            raise RuntimeError("CI checkout/head mismatch")
        record, certified, mode = ci_target(args.commit, event)
        full(record, certified, mode=mode)
        fail_on(certificate_errors(ROOT, record) + candidate_errors(ROOT, record, certified))
        print("PASS ci " + mode + " certified=" + certified + " checkout=" + args.commit)
        return
    needs_record = args.gate in ("full", "git", "certify", "verify-commit", "remote", "reconcile")
    if needs_record and args.evidence is None:
        if args.commit:
            args.evidence = evidence_path(ROOT, args.commit)
        else:
            args.evidence = current_evidence()
    if needs_record and args.commit:
        record = json.loads(git(ROOT, "show", args.commit + ":" + args.evidence))
    elif needs_record:
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
        full(record, args.commit, mode=args.mode)
    elif args.gate == "challenge":
        challenge()
    elif args.gate == "challenge-return":
        semantic(("-Dchallenge.halt=true",))
    elif args.gate == "harness-tests":
        run([sys.executable, "-m", "unittest", "discover", "-s", "scripts/harness/tests", "-v"])
    elif args.gate == "git":
        git_gate(record, args.commit, mode=args.mode)
    elif args.gate in ("certify", "verify-commit"):
        if args.gate == "certify":
            if args.mode != "execution":
                raise RuntimeError("HISTORICAL_READ_ONLY cannot certify new work")
            execution_authority(ROOT, record)
        if args.gate == "verify-commit" and not args.commit:
            raise RuntimeError("--commit required")
        fail_on(document_errors(ROOT) + certificate_errors(ROOT, record)
                + candidate_errors(ROOT, record, args.commit))
    elif args.gate == "remote":
        if not args.commit:
            raise RuntimeError("--commit required")
        fail_on(certificate_errors(ROOT, record) + candidate_errors(ROOT, record, args.commit))
        remote(record, args.commit, mode=args.mode)
    elif args.gate == "reconcile":
        pr = check_pr(record, args.commit, mode="historical")
        registry = read_data(ROOT / "docs/work/registry.json")
        item = next(r for r in registry["history"] if r["id"] == record["work_item"])
        reviews = gh("api", f"repos/{record['repository']}/pulls/{record['pull_request']}/reviews")
        fail_on(reconcile_errors(item, pr, reviews))
    print("PASS " + args.gate)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, subprocess.CalledProcessError, ValueError, KeyError) as ex:
        print("FAIL " + str(ex), file=sys.stderr)
        sys.exit(1)
