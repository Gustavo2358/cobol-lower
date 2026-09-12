"""Technical build helpers and compatibility gate names; no lifecycle certificates."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from architecture import architecture_errors
from checks import digest, read_data
from git_checks import git
from full_checks import performance_counts
from lean import require_local
ROOT = Path(__file__).resolve().parents[2]


def build_root():
    value = os.environ.get("LOWER_BUILD_ROOT", str(ROOT / ".harness-results/build"))
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

def bootstrap(fast=False):
    if not fast:
        require_local()
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
    if fast:
        from upstream_fast import compile_dependency
        log = compile_dependency(path, build_root(), run, maven)
    else:
        log = run(maven("clean", "install"), path)
    if not fast and not re.search(r"PASS: [1-9][0-9]* deterministic contract checks", log):
        raise RuntimeError("upstream contract tests absent")
    jar = build_root() / "m2/io/github/gustavo2358/air-java/0.1.0-SNAPSHOT/air-java-0.1.0-SNAPSHOT.jar"
    codec = jar.parents[2] / "air-json/0.1.0-SNAPSHOT/air-json-0.1.0-SNAPSHOT.jar"
    print('Pinned air-java build PASS: ' + src['commit'])


def verify_dependency():
    checkout = build_root() / 'air-java'
    if git(checkout, 'rev-parse', 'HEAD').decode().strip() != source()['commit'] or git(checkout, 'status', '--porcelain'):
        raise RuntimeError('air-java source differs from immutable pin')
    jar = build_root() / 'm2/io/github/gustavo2358/air-java/0.1.0-SNAPSHOT/air-java-0.1.0-SNAPSHOT.jar'
    codec = jar.parents[2] / 'air-json/0.1.0-SNAPSHOT/air-json-0.1.0-SNAPSHOT.jar'
    if not jar.is_file() or not codec.is_file():
        raise RuntimeError('build pinned air-java dependency first')
    return jar


def semantic(extra=()):
    require_local()
    verify_dependency()
    output = run(maven(*extra, "verify"))
    counts = [int(n) for n in re.findall(r"^LOWER_TESTS=([0-9]+)$", output, re.M)]
    expected_suites = 2  # Current first-slice core + adapters, independent of work-item identity.
    if len(counts) != expected_suites or min(counts) <= 0:
        raise RuntimeError("semantic tests absent/zero")
    output_counts = [int(n) for n in re.findall(r"^LOWER_AIR_OUTPUT_TESTS=([0-9]+)$", output, re.M)]
    if len(output_counts) != 1 or output_counts[0] <= 0:
        raise RuntimeError("AIR output tests absent/zero/duplicate")
    for marker in ("LOWER_CALL_TESTS", "LOWER_CALL_INTEGRATION_TESTS", "LOWER_IF_TESTS", "LOWER_IF_INTEGRATION_TESTS"):
        call_counts = re.findall(r"^" + marker + r"=([0-9]+)$", output, re.M)
        if len(call_counts) != 1 or int(call_counts[0]) <= 0:
            raise RuntimeError("CALL tests absent/zero/duplicate: " + marker)
    production_counts = re.findall(r"^LOWER_PRODUCTION_TESTS=([0-9]+)$", output, re.M)
    if len(production_counts) != 1 or int(production_counts[0]) <= 0 or "PRODUCTION_SUCCESS_PROBE data=400 moves=400" not in output:
        raise RuntimeError("production path probe absent/zero")
    if "-Dlower.performance=true" in extra and "PRODUCTION_CAPACITY_PROBE data=1 moves=10000" not in output:
        raise RuntimeError("production capacity probe absent")
    if "-Dlower.performance=true" in extra:
        capacity = re.findall(r"^LOWER_CAPACITY_TESTS=([0-9]+)$", output, re.M)
        if len(capacity) != 1 or int(capacity[0]) <= 0 or "CAPACITY_DETERMINISM=PASS" not in output:
            raise RuntimeError("capacity proof absent/zero/duplicate")
    print("SEMANTIC_TEST_COUNT=" + str(sum(counts)))
    return output

def performance():
    require_local()
    output = semantic(("-Dlower.performance=true",))
    print("PERFORMANCE_TEST_COUNT=" + str(performance_counts(output)))

def architecture():
    jar = verify_dependency()
    # JSON is the dependency plugin's output, outside the Java domain.
    run(maven("-pl", "core", "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree",
              "-DoutputType=json", "-DoutputFile=target/dependency-tree.json"))
    tree = read_data(ROOT / "core/target/dependency-tree.json")
    hash_jar = build_root() / "m2/com/dynatrace/hash4j/hash4j/0.30.0/hash4j-0.30.0.jar"
    fail_on(architecture_errors(ROOT, tree, jar, hash_jar))

def fail_on(errors):
    if errors:
        raise RuntimeError("\n".join(errors))

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('gate', choices=['fast','ci-fast','docs','docs-fast','ci-guard','harness-tests','bootstrap','architecture','semantic','performance','full','qualification-local','challenge'])
    parser.add_argument('--commit')
    args = parser.parse_args()
    from lean import execute, classify, changed_paths
    from lean_project import full_local, challenges
    if args.gate in ('fast','ci-fast','docs','docs-fast','ci-guard','harness-tests'):
        profile = classify(changed_paths(ROOT)) if args.gate == 'ci-fast' else 'CODE_CHANGE' if args.gate == 'fast' else 'DOCS_ONLY'
        execute(profile, ROOT)
    elif args.gate in ('full','qualification-local'):
        require_local()
        full_local(ROOT)
    elif args.gate == 'challenge':
        require_local()
        challenges(ROOT)
    else:
        globals()[args.gate]()
    print('PASS')

if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, OSError, subprocess.CalledProcessError) as error:
        print('FAIL: ' + str(error), file=sys.stderr)
        sys.exit(1)
