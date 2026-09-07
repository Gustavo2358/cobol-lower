"""Pure full/performance gate contracts, independent of checkpoint transaction sequencing."""
import re

FULL_REQUIRED = {"docs", "architecture", "semantic", "performance", "full", "git"}
FULL_ORDER = ("docs", "semantic", "performance", "architecture", "git", "harness-tests", "challenge")


def performance_counts(output):
    counts = [int(n) for n in re.findall(r"^LOWER_PERFORMANCE_TESTS=([0-9]+)$", output, re.M)]
    if len(counts) != 2 or min(counts) <= 0:
        raise RuntimeError("performance tests absent/zero")
    return sum(counts)


def execute_full(required, runners):
    if set(required) != FULL_REQUIRED or len(required) != len(FULL_REQUIRED):
        raise RuntimeError("FULL required set differs from frozen first-slice contract")
    if set(runners) != set(FULL_ORDER) or any(not callable(runners[name]) for name in FULL_ORDER):
        raise RuntimeError("FULL missing/unknown executable component")
    for name in FULL_ORDER:
        runners[name]()
        print("FULL_COMPONENT_PASS=" + name, flush=True)
